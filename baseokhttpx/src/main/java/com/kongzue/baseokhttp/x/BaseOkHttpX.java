package com.kongzue.baseokhttp.x;

import android.content.Context;

import com.kongzue.baseokhttp.util.JsonMap;
import com.kongzue.baseokhttp.x.interfaces.BaseResponseInterceptListener;
import com.kongzue.baseokhttp.x.interfaces.HeaderInterceptListener;
import com.kongzue.baseokhttp.x.interfaces.ParameterInterceptListener;
import com.kongzue.baseokhttp.x.interfaces.ResponseInterceptListener;
import com.kongzue.baseokhttp.x.util.AssetHelper;
import com.kongzue.baseokhttp.x.util.BaseHttpRequest;
import com.kongzue.baseokhttp.x.util.Parameter;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Cache;
import okhttp3.Cookie;
import okhttp3.HttpUrl;

/**
 * BaseOkHttpX 全局配置类。
 */
public class BaseOkHttpX {

    // 服务器 URL
    public static String serviceUrl;

    // 是否调试模式
    public static boolean debugMode = true;

    // 超时时长（单位：秒）
    public static int globalTimeOutDuration = 10;

    // 强制验证放置在 SSL 证书（当值不为空时生效）
    public static String forceValidationOfSSLCertificatesFilePath;

    // 缓存请求: new Cache(path, cacheSize)
    public static Cache requestCacheSettings = null;

    // 容灾地址
    public static String[] reserveServiceUrls;

    // 保留 Cookies
    public static boolean keepCookies;

    // 已存储的 Cookie
    public static HashMap<HttpUrl, List<Cookie>> cookieStore = new HashMap<>();

    // 启用详细请求事件日志
    public static boolean httpRequestDetailsLogs = false;

    // 全局拦截器
    public static BaseResponseInterceptListener responseInterceptListener;

    // 全局参数拦截器
    public static ParameterInterceptListener parameterInterceptListener;

    // 全局Header拦截器
    public static HeaderInterceptListener headerInterceptListener;

    // 禁止重复请求
    public static boolean disallowSameRequest = false;

    // 全局 Header
    public static Parameter globalHeader;

    // 全局请求参数
    public static Parameter globalParameter;

    // mock 数据延迟返回（ms）
    public static long mockRequestDelay = 0;

    // 全局 mock 数据
    public static Map<String, byte[]> globalMockData = new HashMap<>();

    // 判断一个请求是否启用 mock 数据
    public static boolean isRequestMockEnable(BaseHttpRequest request) {
        if (request == null) {
            return false;
        }
        if (globalMockData == null) {
            globalMockData = new HashMap<>();
        }
        if (request.isEnableMock() != null) {
            return request.isEnableMock();
        }
        return (globalMockData.get(request.getUrl()) != null || globalMockData.get(request.getSubUrl()) != null) && request.isEnableMock() == null;
    }

    // 添加 mock 数据
    public static void addGlobalMockData(String url, byte[] mockData) {
        if (globalMockData == null) {
            globalMockData = new HashMap<>();
        }
        globalMockData.put(url, mockData);
    }

    // 添加 mock 数据
    public static void addGlobalMockData(String url, String mockData) {
        if (globalMockData == null) {
            globalMockData = new HashMap<>();
        }
        globalMockData.put(url, mockData.getBytes());
    }

    // 添加 mock 数据
    public static void addGlobalMockData(String url, JsonMap mockData) {
        if (globalMockData == null) {
            globalMockData = new HashMap<>();
        }
        globalMockData.put(url, mockData.toString().getBytes());
    }

    // 添加 mock 数据
    public static void addGlobalMockData(String url, JSONObject mockData) {
        if (globalMockData == null) {
            globalMockData = new HashMap<>();
        }
        globalMockData.put(url, mockData.toString().getBytes());
    }

    // 添加 mock 数据
    public static void addGlobalMockDataFromAssetFile(Context context, String url, String assetFileName) {
        if (globalMockData == null) {
            globalMockData = new HashMap<>();
        }
        globalMockData.put(url, AssetHelper.readTextFromAssets(context, assetFileName).toString().getBytes());
    }

    // 删除 mock 数据
    public static void removeGlobalMockData(String url) {
        if (globalMockData != null) {
            globalMockData.remove(url);
        }
    }

    //ToDo: WebSocket
}
