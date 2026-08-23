package com.kongzue.baseokhttp.x.util;

import static com.kongzue.baseokhttp.x.util.LockLog.TAG_RETURN;
import static com.kongzue.baseokhttp.x.util.LockLog.TAG_SEND;
import static com.kongzue.baseokhttp.x.util.LockLog.formatJson;
import static com.kongzue.baseokhttp.x.util.LockLog.getExceptionInfo;
import static com.kongzue.baseokhttp.x.util.RequestInfo.addRequestInfo;
import static com.kongzue.baseokhttp.x.util.RequestInfo.deleteRequestInfo;
import static com.kongzue.baseokhttp.x.util.RequestInfo.equalsRequestInfo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.kongzue.baseokhttp.util.JsonMap;
import com.kongzue.baseokhttp.x.BaseOkHttpX;
import com.kongzue.baseokhttp.x.exceptions.RequestException;
import com.kongzue.baseokhttp.x.exceptions.SameRequestException;
import com.kongzue.baseokhttp.x.exceptions.TimeOutException;
import com.kongzue.baseokhttp.x.interfaces.BaseMultiResponseListener;
import com.kongzue.baseokhttp.x.interfaces.BaseResponseListener;
import com.kongzue.baseokhttp.x.interfaces.DownloadListener;
import com.kongzue.baseokhttp.x.interfaces.UploadListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Proxy;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 所有 HTTP 请求的基础类，提供通用的请求能力和配置。
 */
public class BaseHttpRequest implements LifecycleOwner {

    public enum REQUEST_TYPE {
        GET, POST, PUT, DELETE, PATCH
    }

    public enum REQUEST_BODY_TYPE {
        STRING, JSON, FORM, FILE
    }

    protected REQUEST_TYPE requestType = REQUEST_TYPE.GET;      // 请求方式
    protected REQUEST_BODY_TYPE requestBodyType;                // 请求体类型

    protected String url;
    protected List<BaseResponseListener> callbacks = new ArrayList<>();
    protected List<BaseMultiResponseListener> multiResponseListenerList = new ArrayList<>();
    protected Proxy proxy;
    protected OkHttpClient okHttpClient;
    protected long timeoutDuration = BaseOkHttpX.globalTimeOutDuration;     // 请求超时（秒）
    protected boolean callbackInMainLooper;                     // 强行主线程回调
    protected boolean callAsync;                                // 直接在当前线程请求
    protected String requestMimeType = "text/plain; charset=utf-8";

    protected Parameter headerParameter = new Parameter(BaseOkHttpX.globalHeader);
    protected Parameter requestParameter = new Parameter(BaseOkHttpX.globalParameter);
    protected String stringRequestParameter;
    private final Cache undefinedCache = new Cache(new File(""), 1);
    protected Cache cacheSettings = undefinedCache;
    protected File downloadFile;
    protected UploadListener uploadListener;
    protected DownloadListener downloadListener;
    protected boolean showLogs = true;
    protected String cookieStr;
    protected boolean streamRequest;                            // 流式请求
    protected LifecycleRegistry lifecycle = new LifecycleRegistry(this);
    protected Boolean enableMock;                               // 启用 mock 数据
    protected byte[] mockData;                                  // mock 数据

    protected boolean requesting;

    /**
     * 发起请求并同时注册回调监听器。
     *
     * @param callback 用于接收请求结果的回调接口
     */
    public void go(BaseResponseListener callback) {
        registerCallback(callback);
        go();
    }

    /**
     * 同时发起多个请求并注册多请求统一回调监听器
     *
     * @param callback 用于接收多请求统一回调的回调接口
     */
    public void go(BaseMultiResponseListener callback) {
        registerMultiCallback(callback);
        go();
    }

    protected BaseHttpRequest() {
        setLifecycleState(Lifecycle.State.INITIALIZED);
    }

    private Call httpCall;
    private Handler handler;

    /**
     * 执行网络请求。
     * <p>
     * 根据配置自动输出日志并在请求完成后触发回调。
     */
    public void go() {
        responseBytes = null;
        responseMediaType = null;
        responseException = null;
        updateProgressCurrent = 0;
        updateProgressTotal = 0;
        updateProgressDone = false;
        downloadProgress = 0;
        downloadProgressCurrent = 0;
        downloadProgressTotal = 0;
        if (isShowLogs()) {
            LockLog.Builder logBuilder = LockLog.Builder.create()
                    .i(TAG_SEND, "-------------------------------------")
                    .i(TAG_SEND, "发出" + requestType.name() + "请求:" + getUrl() + " 请求时间：" + getNowTimeStr());
            if (!getHeaderParameter().isEmpty()) {
                logBuilder.i(TAG_SEND, "请求头:\t" + getHeaderParameter());
            }
            if (requestBodyType != null) {
                logBuilder.i(TAG_SEND, requestBodyType.name() + "参数:\n" + formatParameterStr());
            }
            logBuilder.i(TAG_SEND, "=====================================")
                    .build();
        }

        if (BaseOkHttpX.isRequestMockEnable(this) && getMockData() != null) {
            setLifecycleState(Lifecycle.State.CREATED);

            if (handler == null) {
                Looper myLooper = Looper.myLooper();
                handler = myLooper == null ? null : new Handler(myLooper);
            }

            if (BaseOkHttpX.disallowSameRequest && equalsRequestInfo(requestInfo) != null) {
                LockLog.logE(TAG_RETURN, "拦截重复请求:" + requestInfo);
                onFail(new SameRequestException(requestInfo));
                return;
            }

            addRequestInfo(requestInfo);
            setRequesting(true);
            if (isCallAsync()) {
                try {
                    Thread.sleep(BaseOkHttpX.mockRequestDelay);
                    onFinish(createMockResponse(getMockData()));
                    setRequesting(false);
                } catch (Exception e) {
                    onFail(e);
                }
            } else {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onFinish(createMockResponse(getMockData()));
                        setRequesting(false);
                    }
                }, BaseOkHttpX.mockRequestDelay);
            }
        } else {
            OkHttpClient client = okHttpClient == null ? createClient() : okHttpClient;

            setLifecycleState(Lifecycle.State.CREATED);

            Request request = createRequest();
            httpCall = client.newCall(request);
            if (handler == null) {
                Looper myLooper = Looper.myLooper();
                handler = myLooper == null ? null : new Handler(myLooper);
            }
            requestInfo = new RequestInfo(url, getRequestParameter());
            if (BaseOkHttpX.disallowSameRequest && equalsRequestInfo(requestInfo) != null) {
                LockLog.logE(TAG_RETURN, "拦截重复请求:" + requestInfo);
                onFail(new SameRequestException(requestInfo));
                return;
            }
            addRequestInfo(requestInfo);
            setRequesting(true);
            if (isCallAsync()) {
                try (Response response = httpCall.execute()) {
                    onFinish(response.body());
                    setRequesting(false);
                } catch (Exception e) {
                    onFail(e);
                }
            } else {
                httpCall.enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        setRequesting(false);
                        onFail(e);
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        try {
                            if (isStreamRequest()) {
                                if (!response.isSuccessful()) {
                                    onFail(new RequestException(call, response.code()));
                                    return;
                                }
                                try (ResponseBody responseBody = response.body();
                                     BufferedReader reader = new BufferedReader(
                                             new InputStreamReader(responseBody.byteStream()))) {
                                    if (isShowLogs()) {
                                        LockLog.Builder logBuilder = LockLog.Builder.create()
                                                .i(TAG_RETURN, "-------------------------------------")
                                                .i(TAG_RETURN, "成功" + requestType.name() + "请求:" + getUrl() + " 返回时间：" + getNowTimeStr());
                                        if (requestBodyType != null) {
                                            logBuilder.i(TAG_RETURN, requestBodyType.name() + "参数:\n" + formatParameterStr());
                                        }
                                        logBuilder.i(TAG_RETURN, "返回内容:");
                                        logBuilder.build();
                                    }
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        onStream(line, responseBody.contentType());
                                    }
                                    if (isShowLogs()) {
                                        LockLog.logI(TAG_RETURN, "=====================================");
                                    }
                                    setRequesting(false);
                                }
                            } else {
                                onFinish(response.body());
                                setRequesting(false);
                            }
                        } finally {
                            response.close();
                        }
                    }
                });
            }

            if (multiRequestList != null) {
                for (BaseHttpRequest otherRequest : multiRequestList) {
                    if (otherRequest.getLifecycle().getCurrentState() == Lifecycle.State.INITIALIZED) {
                        otherRequest.go();
                    }
                }
            }
        }
    }

    private ResponseBody createMockResponse(byte[] mockData) {
        try {
            String mimeType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(mockData));
            if (mimeType == null) {
                String content = new String(mockData, StandardCharsets.UTF_8).trim();
                if (content.startsWith("{") || content.startsWith("[")) {
                    mimeType = "application/json";
                } else if (content.startsWith("<")) {
                    mimeType = "application/xml";
                } else if (isPlainText(content)) {
                    mimeType = "text/plain";
                } else {
                    mimeType = "application/octet-stream";
                }
            }
            return ResponseBody.create(mockData, MediaType.get(mimeType));
        } catch (Exception e) {
            return ResponseBody.create(mockData, MediaType.get("application/json"));
        }
    }

    private static boolean isPlainText(String content) {
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (!(c >= 32 && c <= 126 || c == '\n' || c == '\r' || c == '\t')) {
                return false;
            }
        }
        return true;
    }

    private String formatParameterStr() {
        if (BaseOkHttpX.parameterInterceptListener != null) {
            Object parameter = BaseOkHttpX.parameterInterceptListener.onIntercept(BaseHttpRequest.this, getUrl(), getRequestParameter());
            return parameter instanceof Parameter ? ((Parameter) parameter).toString(requestBodyType) : String.valueOf(parameter);
        } else {
            return requestBodyType == REQUEST_BODY_TYPE.STRING ? stringRequestParameter : getRequestParameter().toString(requestBodyType);
        }
    }

    private void onStream(String line, MediaType mediaType) {
        deleteRequestInfo(requestInfo);
        if (isShowLogs()) {
            LockLog.logI(TAG_RETURN, line);
        }
        this.responseMediaType = mediaType;
        this.responseBytes = line.getBytes();
        if (callbackInMainLooper) {
            Looper mainLooper = Looper.getMainLooper();
            handler = new Handler(mainLooper);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callCallbacks();
                }
            });
        } else {
            if (handler != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callCallbacks();
                    }
                });
            } else {
                callCallbacks();
            }
        }
    }

    private byte[] responseBytes;
    private MediaType responseMediaType;
    private Exception responseException;

    private void onFinish(ResponseBody body) {
        deleteRequestInfo(requestInfo);
        try {
            if (downloadFile == null) {
                responseBytes = body.bytes();
                responseMediaType = body.contentType();
                String charset = responseMediaType.charset(StandardCharsets.UTF_8).name();
                String result = new String(responseBytes, charset);

                if (isShowLogs()) {
                    LockLog.Builder logBuilder = LockLog.Builder.create()
                            .i(TAG_RETURN, "-------------------------------------")
                            .i(TAG_RETURN, "成功" + requestType.name() + "请求:" + getUrl() + " 返回时间：" + getNowTimeStr());
                    if (requestBodyType != null) {
                        logBuilder.i(TAG_RETURN, requestBodyType.name() + "参数:\n" + formatParameterStr());
                    }
                    logBuilder.i(TAG_RETURN, "返回内容:");

                    List<LockLog.LogBody> logBodyList = formatJson(result);
                    if (logBodyList == null) {
                        logBuilder.i(TAG_RETURN, result);
                    } else {
                        logBuilder.add(logBodyList);
                    }
                    logBuilder.i(TAG_RETURN, "=====================================");
                    logBuilder.build();
                }

                if (callbackInMainLooper) {
                    Looper mainLooper = Looper.getMainLooper();
                    handler = new Handler(mainLooper);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callCallbacks();
                        }
                    });
                } else {
                    if (handler != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callCallbacks();
                            }
                        });
                    } else {
                        callCallbacks();
                    }
                }
            } else {
                byte[] buf = new byte[2048];
                int len = 0;
                long sum = 0;
                long total = body.contentLength();
                try (InputStream is = body.byteStream();
                     FileOutputStream fos = new FileOutputStream(downloadFile)) {
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        callDownloadingCallback(sum * 1.0f / total, sum, total, null);
                    }
                    fos.flush();
                }
            }
        } catch (Exception e) {
            onFail(e);
        }
    }

    private void callCallbacks() {
        if (BaseOkHttpX.responseInterceptListener != null &&
                BaseOkHttpX.responseInterceptListener.onIntercept(BaseHttpRequest.this, (responseBytes == null || responseMediaType == null) ? null : ResponseBody.create(responseBytes, responseMediaType), responseException)) {
            return;
        }
        for (BaseResponseListener callback : callbacks) {
            callback.response(BaseHttpRequest.this, (responseBytes == null || responseMediaType == null) ? null : ResponseBody.create(responseBytes, responseMediaType), responseException);
        }
    }

    private void onFail(Exception e) {
        responseException = e;
        if (isShowLogs()) {
            LockLog.Builder logBuilder = LockLog.Builder.create()
                    .i(TAG_RETURN, "-------------------------------------")
                    .i(TAG_RETURN, "失败" + requestType.name() + "请求:" + getUrl() + " 失败时间：" + getNowTimeStr());

            if (requestBodyType != null) {
                logBuilder.i(TAG_RETURN, requestBodyType.name() + "参数:\n" + formatParameterStr());
            }
            logBuilder.i(TAG_RETURN, "错误信息:")
                    .e(TAG_RETURN, getExceptionInfo(e))
                    .i(TAG_RETURN, "=====================================")
                    .build();
        }
        deleteRequestInfo(requestInfo);
        if (BaseOkHttpX.reserveServiceUrls != null && BaseOkHttpX.reserveServiceUrls.length > 0) {
            String nextServiceUrl = findNextServiceUrl(BaseOkHttpX.reserveServiceUrls, BaseOkHttpX.serviceUrl);
            if (!isNull(nextServiceUrl)) {
                BaseOkHttpX.serviceUrl = nextServiceUrl;
                LockLog.logI(TAG_RETURN, "尝试切换容灾服务器地址：" + nextServiceUrl + " 重新请求...");
                go();
                return;
            }
        }
        if (callbackInMainLooper) {
            Looper mainLooper = Looper.getMainLooper();
            handler = new Handler(mainLooper);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callCallbacks();
                }
            });
        } else {
            if (handler != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callCallbacks();
                    }
                });
            } else {
                callCallbacks();
            }
        }
        callUploadingCallback(updateProgressCurrent, updateProgressTotal, updateProgressDone, e);
        callDownloadingCallback(downloadProgress, downloadProgressCurrent, downloadProgressTotal, e);
    }

    private OkHttpClient createClient() {
        InputStream certificates = null;
        if (!isNull(BaseOkHttpX.forceValidationOfSSLCertificatesFilePath)) {
            try {
                certificates = new FileInputStream(new File(BaseOkHttpX.forceValidationOfSSLCertificatesFilePath));
            } catch (Exception e) {
                if (BaseOkHttpX.debugMode) e.printStackTrace();
            }
        }
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .retryOnConnectionFailure(false)
                .connectTimeout(getTimeoutDuration(), TimeUnit.SECONDS)
                .writeTimeout(getTimeoutDuration(), TimeUnit.SECONDS)
                .readTimeout(getTimeoutDuration(), TimeUnit.SECONDS)
                .cache(getCacheSettings());
        if (certificates != null) {
            Object[] getSSLSocketFactoryResult = getSSLSocketFactory(certificates);
            if (getSSLSocketFactoryResult != null && getSSLSocketFactoryResult.length == 2) {
                SSLSocketFactory factory = (SSLSocketFactory) getSSLSocketFactoryResult[0];
                TrustManager[] trustManagers = (TrustManager[]) getSSLSocketFactoryResult[1];
                if (trustManagers != null && trustManagers.length > 0 && trustManagers[0] instanceof X509TrustManager) {
                    builder.sslSocketFactory(factory, (X509TrustManager) trustManagers[0]);
                } else {
                    builder.sslSocketFactory(factory);
                }
            }
        }
        if (proxy != null) {
            builder.proxy(proxy);
        }
        if (BaseOkHttpX.httpRequestDetailsLogs) {
            builder.eventListenerFactory(HttpEventListener.FACTORY);
        }
        if (BaseOkHttpX.keepCookies) {
            builder.cookieJar(new CookieJar() {
                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                    BaseOkHttpX.cookieStore.put(url, cookies);
                }

                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    List<Cookie> cookies = BaseOkHttpX.cookieStore.get(url);
                    return cookies != null ? cookies : new ArrayList<>();
                }
            });
        }
        return builder.build();
    }

    private Request createRequest() {
        Request.Builder builder = new Request.Builder();
        RequestBody originRequestBody = createOriginRequestBody();
        RequestBody requestBody = originRequestBody == null ? null : new RequestBodyImpl(originRequestBody) {
            @Override
            public void onUploading(long current, long total, boolean done) {
                callUploadingCallback(current, total, done, null);
            }
        };
        switch (requestType) {
            case POST:
                if (requestBody != null) builder.post(requestBody);
                break;
            case PUT:
                if (requestBody != null) builder.put(requestBody);
                break;
            case PATCH:
                if (requestBody != null) builder.patch(requestBody);
                break;
            case DELETE:
                if (requestBody == null) {
                    builder.delete();
                } else {
                    builder.delete(requestBody);
                }
                break;
        }
        builder.url(getUrl());
        for (Map.Entry<String, Object> entry : getHeaderParameter().entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue() + "");
        }
        if (!isNull(cookieStr)) {
            builder.addHeader("Cookie", cookieStr);
        }
        return builder.build();
    }

    long updateProgressCurrent;
    long updateProgressTotal;
    boolean updateProgressDone;

    private void callUploadingCallback(long current, long total, boolean done, Exception error) {
        if (getUploadListener() == null) return;
        if (callbackInMainLooper) {
            Looper mainLooper = Looper.getMainLooper();
            handler = new Handler(mainLooper);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    getUploadListener().onUpload(BaseHttpRequest.this, (float) current / total, current, total, done, error);
                }
            });
        } else {
            if (handler != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        getUploadListener().onUpload(BaseHttpRequest.this, (float) current / total, current, total, done, error);
                    }
                });
            } else {
                getUploadListener().onUpload(BaseHttpRequest.this, (float) current / total, current, total, done, error);
            }
        }
        if (error == null) {
            updateProgressCurrent = current;
            updateProgressTotal = total;
            updateProgressDone = done;
        }
    }

    float downloadProgress;
    long downloadProgressCurrent;
    long downloadProgressTotal;

    private void callDownloadingCallback(float progress, long current, long total, Exception error) {
        if (getDownloadListener() == null) return;
//        LockLog.logI(TAG_RETURN, "下载：" + getUrl() + " 进度：" + progress + " 已下载：" + current + " 总共：" + total);
        if (callbackInMainLooper) {
            Looper mainLooper = Looper.getMainLooper();
            handler = new Handler(mainLooper);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    getDownloadListener().onDownload(BaseHttpRequest.this, downloadFile, progress, current, total, progress >= 1f, error);
                }
            });
        } else {
            if (handler != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        getDownloadListener().onDownload(BaseHttpRequest.this, downloadFile, progress, current, total, progress >= 1f, error);
                    }
                });
            } else {
                getDownloadListener().onDownload(BaseHttpRequest.this, downloadFile, progress, current, total, progress >= 1f, error);
            }
        }
        if (error == null) {
            downloadProgress = progress;
            downloadProgressCurrent = current;
            downloadProgressTotal = total;
        }
    }

    private @Nullable RequestBody createOriginRequestBody() {
        if (requestBodyType != null) {
            if (BaseOkHttpX.parameterInterceptListener != null) {
                switch (requestBodyType) {
                    case STRING:
                        String parameterStr = String.valueOf(BaseOkHttpX.parameterInterceptListener.onIntercept(BaseHttpRequest.this, getUrl(), stringRequestParameter));
                        return RequestBody.create(isNull(parameterStr) ? "" : parameterStr, MediaType.parse(getRequestMimeType()));
                    case FILE:
                        return ((Parameter) BaseOkHttpX.parameterInterceptListener.onIntercept(BaseHttpRequest.this, getUrl(), getRequestParameter())).toFileParameter();
                    case FORM:
                        return ((Parameter) BaseOkHttpX.parameterInterceptListener.onIntercept(BaseHttpRequest.this, getUrl(), getRequestParameter())).toFormParameter();
                    case JSON:
                        return RequestBody.create(
                                ((Parameter) BaseOkHttpX.parameterInterceptListener.onIntercept(BaseHttpRequest.this, getUrl(), getRequestParameter()))
                                        .toParameterJsonMap().toString(), MediaType.parse(getRequestMimeType()));
                }
            } else {
                switch (requestBodyType) {
                    case STRING:
                        return RequestBody.create(isNull(stringRequestParameter) ? "" : stringRequestParameter, MediaType.parse(getRequestMimeType()));
                    case FILE:
                        return getRequestParameter().toFileParameter();
                    case FORM:
                        return getRequestParameter().toFormParameter();
                    case JSON:
                        return RequestBody.create(getRequestParameter().toParameterJsonMap().toString(), MediaType.parse(getRequestMimeType()));
                }
            }
        }
        return null;
    }

    private String getRealRequestUrl(String url) {
        String serviceUrl = BaseOkHttpX.serviceUrl;
        if (serviceUrl.endsWith("/") && url.startsWith("/")) {
            return serviceUrl + url.substring(1);
        }
        if (!serviceUrl.endsWith("/") && !url.startsWith("/")) {
            return serviceUrl + "/" + url;
        }
        return serviceUrl + url;
    }

    /**
     * 获取最终请求地址。
     * <p>
     * 当为 GET 请求时会自动拼接参数。
     *
     * @return 完整的请求地址
     */
    public String getUrl() {
        if (requestType == REQUEST_TYPE.GET) {
            Object parameter = getRequestParameter();
            String result;
            if (!url.startsWith("http")) {
                result = getRealRequestUrl(url);
            } else {
                result = url;
            }
            if (BaseOkHttpX.parameterInterceptListener != null) {
                parameter = BaseOkHttpX.parameterInterceptListener.onIntercept(BaseHttpRequest.this, result, parameter);
            }
            String parameterStr = (parameter instanceof Parameter) ? "?" + ((Parameter) parameter).toUrlParameter() : String.valueOf(parameter);
            return result + parameterStr;
        } else {
            if (!url.startsWith("http")) {
                return getRealRequestUrl(url);
            } else {
                return url;
            }
        }
    }

    /**
     * 获取本次请求设置的节点请求地址
     *
     * @return 节点请求地址
     */
    public String getSubUrl() {
        return url;
    }

    /**
     * 设置请求地址。
     *
     * @param url 请求的相对或绝对路径
     * @return 当前请求对象
     */
    public BaseHttpRequest setUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * 获取当前使用的代理设置。
     *
     * @return 代理对象，可能为 {@code null}
     */
    public Proxy getProxy() {
        return proxy;
    }

    /**
     * 设置请求所使用的代理。
     *
     * @param proxy 代理配置
     * @return 当前请求对象
     */
    public BaseHttpRequest setProxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    /**
     * 获取自定义的 OkHttpClient 实例。
     *
     * @return OkHttpClient 实例
     */
    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    /**
     * 指定自定义的 OkHttpClient。
     *
     * @param okHttpClient OkHttpClient 对象
     * @return 当前请求对象
     */
    public BaseHttpRequest setOkHttpClient(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
        return this;
    }

    /**
     * 获取本次请求的超时时长，单位秒。
     *
     * @return 超时秒数
     */
    public long getTimeoutDuration() {
        return timeoutDuration <= 0 ? (BaseOkHttpX.globalTimeOutDuration <= 0 ? 10 : BaseOkHttpX.globalTimeOutDuration) : timeoutDuration;
    }

    /**
     * 设置本次请求的超时时长。
     *
     * @param timeoutDuration 超时秒数
     * @return 当前请求对象
     */
    public BaseHttpRequest setTimeoutDuration(long timeoutDuration) {
        this.timeoutDuration = timeoutDuration;
        return this;
    }

    /**
     * 是否在主线程回调结果。
     *
     * @return true 表示强制在主线程回调
     */
    public boolean isCallbackInMainLooper() {
        return callbackInMainLooper;
    }

    /**
     * 设置是否在主线程回调结果。
     *
     * @param callbackInMainLooper true 时回调将在主线程执行
     * @return 当前请求对象
     */
    public BaseHttpRequest setCallbackInMainLooper(boolean callbackInMainLooper) {
        this.callbackInMainLooper = callbackInMainLooper;
        return this;
    }

    /**
     * 获取底层的 {@link Call} 对象。
     *
     * @return OkHttp 的 Call 实例
     */
    public Call getHttpCall() {
        return httpCall;
    }

    /**
     * 获取当前请求方式。
     *
     * @return 请求类型枚举
     */
    public REQUEST_TYPE getRequestType() {
        return requestType;
    }

    /**
     * 设置请求方式，例如 GET 或 POST。
     *
     * @param requestType 请求类型
     * @return 当前请求对象
     */
    public BaseHttpRequest setRequestType(REQUEST_TYPE requestType) {
        this.requestType = requestType;
        return this;
    }

    protected void log(String data) {
        Log.i(TAG_SEND, data);
    }

    /**
     * 获取请求体的 MimeType。
     *
     * @return 请求体类型描述
     */
    public String getRequestMimeType() {
        return requestMimeType;
    }

    /**
     * 指定请求体的 MimeType。
     *
     * @param requestMimeType MimeType 字符串
     * @return 当前请求对象
     */
    public BaseHttpRequest setRequestMimeType(String requestMimeType) {
        this.requestMimeType = requestMimeType;
        return this;
    }

    private boolean isNull(String s) {
        if (s == null || s.trim().isEmpty() || "null".equals(s) || "(null)".equals(s)) {
            return true;
        }
        return false;
    }

    /**
     * 获取当前请求头参数集合。
     *
     * @return 请求头参数
     */
    public Parameter getHeaderParameter() {
        Parameter headers = headerParameter == null ? headerParameter = new Parameter(BaseOkHttpX.globalHeader) : headerParameter;
        if (BaseOkHttpX.headerInterceptListener != null) {
            headers = BaseOkHttpX.headerInterceptListener.onIntercept(BaseHttpRequest.this, getUrl(), headers);
        }
        return headers;
    }

    /**
     * 获取当前的请求参数集合。
     *
     * @return 请求参数
     */
    public Parameter getRequestParameter() {
        return requestParameter == null ? requestParameter = new Parameter(BaseOkHttpX.globalParameter) : requestParameter;
    }

    /**
     * 向请求体中添加单个参数。
     *
     * @param key   参数名
     * @param value 参数值
     * @return 当前请求对象
     */
    public BaseHttpRequest addParameter(String key, Object value) {
        getRequestParameter().add(key, value);
        if (requestBodyType == null)
            requestBodyType = value instanceof File ? REQUEST_BODY_TYPE.FILE : REQUEST_BODY_TYPE.FORM;
        return this;
    }

    /**
     * 向请求体中添加数组形式的参数。
     *
     * @param key   参数名
     * @param value 参数数组
     * @return 当前请求对象
     */
    public BaseHttpRequest addParameter(String key, Object... value) {
        getRequestParameter().add(key, value);
        requestBodyType = REQUEST_BODY_TYPE.FORM;
        return this;
    }

    /**
     * 直接设置字符串形式的请求体。
     *
     * @param parameter 原始字符串参数
     * @return 当前请求对象
     */
    public BaseHttpRequest setParameter(String parameter) {
        this.stringRequestParameter = parameter;
        if (requestBodyType == null) requestBodyType = REQUEST_BODY_TYPE.STRING;
        return this;
    }

    /**
     * 使用 {@link Parameter} 对象设置请求参数。
     *
     * @param parameter 参数集合
     * @return 当前请求对象
     */
    public BaseHttpRequest setParameter(Parameter parameter) {
        this.requestParameter = parameter;
        if (requestBodyType == null) requestBodyType = REQUEST_BODY_TYPE.FORM;
        return this;
    }

    /**
     * 使用 JsonMap 设置请求参数，并自动将 MimeType 设置为 JSON。
     *
     * @param parameter JsonMap 参数
     * @return 当前请求对象
     */
    public BaseHttpRequest setParameter(JsonMap parameter) {
        this.requestParameter = new Parameter(parameter);
        if (requestBodyType == null) requestBodyType = REQUEST_BODY_TYPE.JSON;
        requestMimeType = "application/json; charset=utf-8";
        return this;
    }

    /**
     * 使用 JSONObject 设置请求参数，MimeType 同样为 JSON。
     *
     * @param parameter JSON 对象
     * @return 当前请求对象
     */
    public BaseHttpRequest setParameter(JSONObject parameter) {
        this.requestParameter = new Parameter(new JsonMap(parameter == null ? "" : parameter.toString()));
        if (requestBodyType == null) requestBodyType = REQUEST_BODY_TYPE.JSON;
        requestMimeType = "application/json; charset=utf-8";
        return this;
    }

    /**
     * 新增请求头信息。
     *
     * @param key   请求头名称
     * @param value 请求头值
     * @return 当前请求对象
     */
    public BaseHttpRequest addHeader(String key, Object value) {
        getHeaderParameter().add(key, value);
        return this;
    }

    /**
     * 设置完整的请求头参数集合。
     *
     * @param parameter 请求头参数
     * @return 当前请求对象
     */
    public BaseHttpRequest setHeader(Parameter parameter) {
        this.headerParameter = parameter;
        return this;
    }

    /**
     * 获取当前请求体类型。
     *
     * @return 请求体类型枚举
     */
    public REQUEST_BODY_TYPE getRequestBodyType() {
        return requestBodyType;
    }

    /**
     * 设置请求体类型。
     *
     * @param requestBodyType 请求体类型枚举
     * @return 当前请求对象
     */
    public BaseHttpRequest setRequestBodyType(REQUEST_BODY_TYPE requestBodyType) {
        this.requestBodyType = requestBodyType;
        return this;
    }

    /**
     * 当前请求是否在调用线程中同步执行。
     *
     * @return true 表示同步执行
     */
    public boolean isCallAsync() {
        return callAsync;
    }

    /**
     * 设置是否在调用线程内同步执行请求。
     *
     * @param callAsync true 则在当前线程执行
     * @return 当前请求对象
     */
    public BaseHttpRequest setCallAsync(boolean callAsync) {
        this.callAsync = callAsync;
        return this;
    }

    /**
     * 当前请求是否正在执行中。
     *
     * @return true 表示请求未完成
     */
    public boolean isRequesting() {
        return requesting;
    }

    private Timer timeoutChecker;
    private RequestInfo requestInfo;

    /**
     * 手动标记请求的执行状态。
     * <p>
     * 当设置为 true 时会启动超时检测，false 则停止检测。
     *
     * @param requesting 是否处于请求状态
     */
    public void setRequesting(boolean requesting) {
        this.requesting = requesting;
        if (requesting) {
            if (!streamRequest) {
                timeoutChecker = new Timer();
                timeoutChecker.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (isRequesting()) {
                            onFail(new TimeOutException());
                        }
                    }
                }, timeoutDuration * 1000);
            }
            setLifecycleState(Lifecycle.State.STARTED);
        } else {
            if (timeoutChecker != null) {
                timeoutChecker.cancel();
            }
            setLifecycleState(Lifecycle.State.DESTROYED);
        }
    }

    private static Object[] getSSLSocketFactory(InputStream... certificates) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            int index = 0;
            for (InputStream certificate : certificates) {
                String certificateAlias = Integer.toString(index++);
                keyStore.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(certificate));

                try {
                    if (certificate != null) {
                        certificate.close();
                    }
                } catch (IOException e) {
                }
            }
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            return new Object[]{sslContext.getSocketFactory(), trustManagerFactory.getTrustManagers()};
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 绑定生命周期，在 Context 销毁时自动取消请求。
     *
     * @param context Activity 或其他上下文对象
     * @return 当前请求对象
     */
    public BaseHttpRequest bindLifecycleOwner(Context context) {
        if (context instanceof LifecycleOwner) {
            return bindLifecycleOwner((LifecycleOwner) context);
        }
        return this;
    }

    /**
     * 绑定生命周期，在 LifecycleOwner 销毁时自动取消请求。
     *
     * @param lifecycleOwner 生命周期拥有者
     * @return 当前请求对象
     */
    public BaseHttpRequest bindLifecycleOwner(LifecycleOwner lifecycleOwner) {
        lifecycleOwner.getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    cancel();
                }
            }
        });
        return this;
    }

    /**
     * 取消当前请求并清理相关资源。
     */
    public void cancel() {
        handler = null;
        if (callbacks != null) {
            callbacks.clear();
        }
        if (timeoutChecker != null) {
            timeoutChecker.cancel();
        }
        if (httpCall != null) {
            httpCall.cancel(); // 取消未完成的请求
            httpCall = null;
        }
        responseBytes = null;
        responseMediaType = null;
        responseException = null;
        lifecycle = new LifecycleRegistry(this);
        setLifecycleState(Lifecycle.State.INITIALIZED);
    }

    /**
     * 重新发起请求。
     */
    public void retry() {
        cancel();
        go();
    }

    /**
     * 注册一个结果回调监听器。
     *
     * @param callback 回调接口
     * @return 当前请求对象
     */
    public BaseHttpRequest registerCallback(BaseResponseListener callback) {
        if (!callbacks.contains(callback)) callbacks.add(callback);
        return this;
    }

    /**
     * 设置回调监听器，效果同 {@link #registerCallback(BaseResponseListener)}。
     */
    public BaseHttpRequest setCallback(BaseResponseListener callback) {
        return registerCallback(callback);
    }

    /**
     * 追加一个回调监听器。
     */
    public BaseHttpRequest addCallback(BaseResponseListener callback) {
        return registerCallback(callback);
    }

    /**
     * 移除指定的回调监听器。
     *
     * @param callback 待移除的回调
     * @return 当前请求对象
     */
    public BaseHttpRequest unregisterCallback(BaseResponseListener callback) {
        callbacks.remove(callback);
        return this;
    }

    /**
     * 与 {@link #unregisterCallback(BaseResponseListener)} 相同。
     */
    public BaseHttpRequest removeCallback(BaseResponseListener callback) {
        return unregisterCallback(callback);
    }

    /**
     * 清除所有已注册的回调监听器。
     *
     * @return 当前请求对象
     */
    public BaseHttpRequest removeAllCallbacks() {
        callbacks.clear();
        return this;
    }

    /**
     * 获取所有已注册的回调监听器列表。
     *
     * @return 回调监听器集合
     */
    public List<BaseResponseListener> getCallbacks() {
        return callbacks;
    }

    /**
     * 注册一个多请求统一合并回调监听器
     *
     * @param callback 回调接口
     * @return 当前请求对象
     */
    public BaseHttpRequest registerMultiCallback(BaseMultiResponseListener callback) {
        if (!multiResponseListenerList.contains(callback)) multiResponseListenerList.add(callback);
        return this;
    }

    /**
     * 与 {@link #registerMultiCallback(BaseMultiResponseListener)} 相同。
     */
    public BaseHttpRequest setMultiCallback(BaseMultiResponseListener callback) {
        registerMultiCallback(callback);
        return this;
    }

    /**
     * 与 {@link #registerMultiCallback(BaseMultiResponseListener)} 相同。
     */
    public BaseHttpRequest addMultiCallback(BaseMultiResponseListener callback) {
        registerMultiCallback(callback);
        return this;
    }

    /**
     * 移除指定的多请求统一合并回调监听器
     *
     * @param callback 待移除的回调
     * @return 当前请求对象
     */
    public BaseHttpRequest unregisterMultiCallback(BaseMultiResponseListener callback) {
        multiResponseListenerList.remove(callback);
        return this;
    }

    /**
     * 与 {@link #unregisterMultiCallback(BaseMultiResponseListener)} 相同。
     */
    public BaseHttpRequest removeMultiCallback(BaseMultiResponseListener callback) {
        unregisterMultiCallback(callback);
        return this;
    }

    /**
     * 清除所有已注册的统一合并回调监听器。
     *
     * @return 当前请求对象
     */
    public BaseHttpRequest removeAllMultiCallback() {
        multiResponseListenerList.clear();
        return this;
    }

    /**
     * 获取缓存配置，如果未设置则返回全局配置。
     *
     * @return 缓存配置对象
     */
    public Cache getCacheSettings() {
        return cacheSettings != undefinedCache ? cacheSettings : BaseOkHttpX.requestCacheSettings;
    }

    /**
     * 设置当前请求的缓存策略。
     *
     * @param cacheSettings 缓存配置
     * @return 当前请求对象
     */
    public BaseHttpRequest setCacheSettings(Cache cacheSettings) {
        this.cacheSettings = cacheSettings;
        return this;
    }

    /**
     * 指定下载文件的保存路径，并设置下载监听器。
     *
     * @param file             目标文件
     * @param downloadListener 下载进度回调
     * @return 当前请求对象
     */
    public BaseHttpRequest downloadToFile(File file, DownloadListener downloadListener) {
        this.downloadFile = file;
        this.downloadListener = downloadListener;
        File dir = downloadFile.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return this;
    }

    /**
     * 获取上传监听器。
     *
     * @return 上传监听器
     */
    public UploadListener getUploadListener() {
        return uploadListener;
    }

    /**
     * 设置上传监听器。
     *
     * @param uploadListener 上传监听回调
     * @return 当前请求对象
     */
    public BaseHttpRequest setUploadListener(UploadListener uploadListener) {
        this.uploadListener = uploadListener;
        return this;
    }

    /**
     * 获取下载监听器。
     *
     * @return 下载监听器
     */
    public DownloadListener getDownloadListener() {
        return downloadListener;
    }

    /**
     * 设置是否输出调试日志。
     *
     * @param showLogs 是否显示日志
     * @return 当前请求对象
     */
    public BaseHttpRequest setShowLogs(boolean showLogs) {
        this.showLogs = showLogs;
        return this;
    }

    /**
     * 判断是否允许输出日志。
     *
     * @return true 表示会输出日志
     */
    public boolean isShowLogs() {
        return showLogs && BaseOkHttpX.debugMode;
    }

    /**
     * 获取设置的 Cookie 字符串。
     *
     * @return Cookie 内容
     */
    public String getCookieStr() {
        return cookieStr;
    }

    /**
     * 设置请求的 Cookie 字符串。
     *
     * @param cookieStr Cookie 内容
     * @return 当前请求对象
     */
    public BaseHttpRequest setCookieStr(String cookieStr) {
        this.cookieStr = cookieStr;
        return this;
    }

    protected String getNowTimeStr() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
    }

    protected String findNextServiceUrl(String[] reserveServiceUrls, String serviceUrl) {
        if (reserveServiceUrls == null || reserveServiceUrls.length == 0) {
            return null;
        }
        for (int i = 0; i < reserveServiceUrls.length; i++) {
            if (Objects.equals(serviceUrl, reserveServiceUrls[i])) {
                if (i == reserveServiceUrls.length - 1) {
                    return null;
                } else {
                    return reserveServiceUrls[i + 1];
                }
            }
        }
        return reserveServiceUrls[0];
    }

    /**
     * 是否以流的方式处理响应。
     *
     * @return true 表示流式请求
     */
    public boolean isStreamRequest() {
        return streamRequest;
    }

    /**
     * 设置是否以流的方式处理响应内容。
     *
     * @param streamRequest 是否流式处理
     * @return 当前请求对象
     */
    public BaseHttpRequest setStreamRequest(boolean streamRequest) {
        this.streamRequest = streamRequest;
        return this;
    }

    /**
     * 获取 BaseOkHttpRequest 的生命周期对象
     *
     * @return Lifecycle 请求流程的生命周期对象
     */
    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    protected void setLifecycleState(Lifecycle.State s) {
        if (lifecycle == null || s == null) return;
        try {
            lifecycle.setCurrentState(s);
        } catch (Exception e) {
        }
    }

    List<BaseHttpRequest> multiRequestList;
    List<BaseHttpRequest> multiResponseList;

    /**
     * 设置同时请求的其他 BaseOkHttpRequest 请求对象，
     * 所有请求会一起统一合并返回，请使用 '.go(? extends BaseMultiResponseListener)' 进行统一回调处理
     *
     * @param request 其他 BaseOkHttpRequest 请求对象
     * @return 当前请求对象
     */
    public BaseHttpRequest with(BaseHttpRequest request) {
        if (multiRequestList == null) {
            multiRequestList = new ArrayList<>();
            multiRequestList.add(this);
        }
        if (!multiRequestList.contains(request)) multiRequestList.add(request);
        registerMultiResult(request);
        return this;
    }

    private void registerMultiResult(BaseHttpRequest request) {
        if (request.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
            if (request.responseException != null || (request.responseBytes != null && request.responseMediaType != null)) {
                if (getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
                    toMultiResult(request);
                } else {
                    addCallback(new BaseResponseListener() {
                        @Override
                        public void response(BaseHttpRequest httpRequest, ResponseBody responseBody, Exception error) {
                            toMultiResult(request);
                        }
                    });
                }
                return;
            }
        }
        request.addCallback(new BaseResponseListener() {
            @Override
            public void response(BaseHttpRequest httpRequest, ResponseBody responseBody, Exception error) {
                toMultiResult(httpRequest);
            }
        });
        addCallback(new BaseResponseListener() {
            @Override
            public void response(BaseHttpRequest httpRequest, ResponseBody responseBody, Exception error) {
                toMultiResult(httpRequest);
            }
        });
    }

    private void toMultiResult(BaseHttpRequest request) {
        if (multiResponseList == null) multiResponseList = new ArrayList<>();
        if (multiResponseList.contains(request)) return;
        multiResponseList.add(request);

        if (containsAllList(multiResponseList, multiRequestList)) {
            BaseHttpRequest[] allRequest = new BaseHttpRequest[multiRequestList.size()];
            Map<BaseHttpRequest, ResponseBody> responseBodyMap = new LinkedHashMap<>();
            Map<BaseHttpRequest, Exception> errors = new LinkedHashMap<>();

            for (int i = 0; i < allRequest.length; i++) {
                allRequest[i] = multiRequestList.get(i);
                Exception error = allRequest[i].responseException;
                ResponseBody resultBody = allRequest[i].responseBytes == null ? null : ResponseBody.create(allRequest[i].responseBytes, allRequest[i].responseMediaType);
                responseBodyMap.put(allRequest[i], resultBody);
                errors.put(allRequest[i], error);
            }

            if (callbackInMainLooper) {
                Looper mainLooper = Looper.getMainLooper();
                handler = new Handler(mainLooper);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (BaseMultiResponseListener multiResponseListener : multiResponseListenerList) {
                            multiResponseListener.response(allRequest, responseBodyMap, errors);
                        }
                    }
                });
            } else {
                if (handler != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            for (BaseMultiResponseListener multiResponseListener : multiResponseListenerList) {
                                multiResponseListener.response(allRequest, responseBodyMap, errors);
                            }
                        }
                    });
                } else {
                    for (BaseMultiResponseListener multiResponseListener : multiResponseListenerList) {
                        multiResponseListener.response(allRequest, responseBodyMap, errors);
                    }
                }
            }
        }
    }

    private static <T> boolean containsAllList(List<T> list1, List<T> list2) {
        Set<T> set1 = new HashSet<>(list1);
        return set1.containsAll(list2);
    }

    /**
     * 在请求完成后获取服务器响应结果的请求字节
     *
     * @return 服务器返回的数据字节
     */
    public byte[] getResponseBytes() {
        return responseBytes;
    }

    /**
     * 在请求完成后获取服务器响应结果的媒体类型
     *
     * @return 服务器返回的媒体类型
     */
    public MediaType getResponseMediaType() {
        return responseMediaType;
    }

    /**
     * 在请求完成后获取服务器响应结果的错误信息
     *
     * @return 请求过程中抛出的异常
     */
    public Exception getResponseException() {
        return responseException;
    }

    /**
     * 判断本请求是否异常
     *
     * @return 是否存在异常
     */
    public boolean isError() {
        return responseException == null;
    }

    /**
     * 获取上传进度的已上传字节数
     *
     * @return 已上传字节数
     */
    public long getUpdateProgressCurrent() {
        return updateProgressCurrent;
    }

    /**
     * 获取上传进度的总字节数
     *
     * @return 总字节数
     */
    public long getUpdateProgressTotal() {
        return updateProgressTotal;
    }

    /**
     * 获取上传进度是否完成
     *
     * @return 是否完成
     */
    public boolean isUpdateProgressDone() {
        return updateProgressDone;
    }

    /**
     * 获取下载进度的百分比
     *
     * @return 当前下载进度（0~1）
     */
    public float getDownloadProgress() {
        return downloadProgress;
    }

    /**
     * 获取下载进度的已完成字节数
     *
     * @return 已完成字节数
     */
    public long getDownloadProgressCurrent() {
        return downloadProgressCurrent;
    }

    /**
     * 获取下载进度的总字节数
     *
     * @return 总字节数
     */
    public long getDownloadProgressTotal() {
        return downloadProgressTotal;
    }

    public Boolean isEnableMock() {
        return enableMock;
    }

    /**
     * 是否启用 mock 数据
     *
     * @param enableMock 启用 mock 数据
     * @return this
     */
    public BaseHttpRequest setEnableMock(boolean enableMock) {
        this.enableMock = enableMock;
        return this;
    }

    public byte[] getMockData() {
        if (mockData == null) {
            byte[] result = BaseOkHttpX.globalMockData.get(getUrl());
            if (result != null) {
                return result;
            }
            return BaseOkHttpX.globalMockData.get(getSubUrl());
        }
        return mockData;
    }

    /**
     * 设置 mock 数据
     *
     * @param mockData mock 数据
     * @return this
     */
    public BaseHttpRequest setMockData(byte[] mockData) {
        this.mockData = mockData;
        return this;
    }

    public BaseHttpRequest setMockData(String mockData) {
        this.mockData = mockData.getBytes();
        return this;
    }

    public BaseHttpRequest setMockAssetFile(Context context, String mockDataFileName) {
        setMockData(AssetHelper.readTextFromAssets(context, mockDataFileName));
        return this;
    }

    public BaseHttpRequest setMockData(JsonMap mockData) {
        this.mockData = mockData.toString().getBytes();
        return this;
    }

    public BaseHttpRequest setMockData(JSONObject mockData) {
        this.mockData = mockData.toString().getBytes();
        return this;
    }
}
