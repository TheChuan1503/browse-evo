package com.kongzue.baseokhttp.x.util;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.HttpUrl;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author: Kongzue
 * @github: https://github.com/kongzue/
 * @homepage: http://kongzue.com/
 * @mail: myzcxhh@live.cn
 * @createTime: 2021/11/14 14:08
 */
public class HttpEventListener extends EventListener {
    
    /**
     * OkHttp 事件监听工厂
     */
    public static final Factory FACTORY = new Factory() {
        final AtomicLong nextCallId = new AtomicLong(1L);
        
        @Override
        /**
         * 为每次网络调用创建事件监听器
         */
        public EventListener create(Call call) {
            long callId = nextCallId.getAndIncrement();
            return new HttpEventListener(callId, call.request().url(), System.nanoTime());
        }
    };
    
    private final long callId;
    
    private final long callStartNanos;
    
    LockLog.Builder logBuilder = LockLog.Builder.create();
    
    /**
     * 构造方法
     *
     * @param callId         调用 ID
     * @param url            请求地址
     * @param callStartNanos 调用开始时间
     */
    public HttpEventListener(long callId, HttpUrl url, long callStartNanos) {
        this.callId = callId;
        this.callStartNanos = callStartNanos;
    }
    
    private void recordEventLog(String name) {
        long elapseNanos = System.nanoTime() - callStartNanos;
    
        logBuilder.i("***",
                (elapseNanos / 1000000) + "ms: \t#"+ name
                );
        if (name.equalsIgnoreCase("请求结束") || name.equalsIgnoreCase("请求失败 ×")) {
            logBuilder.build();
        }
    }
    
    @Override
    /**
     * 网络请求开始
     */
    public void callStart(Call call) {
        super.callStart(call);
        recordEventLog("请求开始：" + call.request().url());
    }

    @Override
    /**
     * DNS 解析开始
     */
    public void dnsStart(Call call, String domainName) {
        super.dnsStart(call, domainName);
        recordEventLog("DNS 解析开始");
    }

    @Override
    /**
     * DNS 解析结束
     */
    public void dnsEnd(Call call, String domainName, List<InetAddress> inetAddressList) {
        super.dnsEnd(call, domainName, inetAddressList);
        recordEventLog("DNS 解析结束");
    }

    @Override
    /**
     * 开始建立连接
     */
    public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
        super.connectStart(call, inetSocketAddress, proxy);
        recordEventLog("连接开始");
    }

    @Override
    /**
     * HTTPS 安全连接开始
     */
    public void secureConnectStart(Call call) {
        super.secureConnectStart(call);
        recordEventLog("安全连接开始（HTTPS）");
    }

    @Override
    /**
     * HTTPS 安全连接结束
     */
    public void secureConnectEnd(Call call, @Nullable Handshake handshake) {
        super.secureConnectEnd(call, handshake);
        recordEventLog("安全连接结束（HTTPS）");
    }

    @Override
    /**
     * 连接建立完成
     */
    public void connectEnd(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, @Nullable Protocol protocol) {
        super.connectEnd(call, inetSocketAddress, proxy, protocol);
        recordEventLog("连接结束");
    }

    @Override
    /**
     * 连接失败
     */
    public void connectFailed(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, @Nullable Protocol protocol, IOException ioe) {
        super.connectFailed(call, inetSocketAddress, proxy, protocol, ioe);
        recordEventLog("连接失败");
    }

    @Override
    /**
     * 获得连接
     */
    public void connectionAcquired(Call call, Connection connection) {
        super.connectionAcquired(call, connection);
        recordEventLog("连接获得");
    }

    @Override
    /**
     * 释放连接
     */
    public void connectionReleased(Call call, Connection connection) {
        super.connectionReleased(call, connection);
        recordEventLog("连接释放");
    }

    @Override
    /**
     * 请求头开始发送
     */
    public void requestHeadersStart(Call call) {
        super.requestHeadersStart(call);
        recordEventLog("请求头开始");
    }

    @Override
    /**
     * 请求头发送结束
     */
    public void requestHeadersEnd(Call call, Request request) {
        super.requestHeadersEnd(call, request);
        recordEventLog("请求头结束");
    }

    @Override
    /**
     * 请求体开始发送
     */
    public void requestBodyStart(Call call) {
        super.requestBodyStart(call);
        recordEventLog("请求体开始");
    }

    @Override
    /**
     * 请求体发送结束
     */
    public void requestBodyEnd(Call call, long byteCount) {
        super.requestBodyEnd(call, byteCount);
        recordEventLog("请求体结束");
    }

    @Override
    /**
     * 响应头开始接收
     */
    public void responseHeadersStart(Call call) {
        super.responseHeadersStart(call);
        recordEventLog("响应头开始");
    }

    @Override
    /**
     * 响应头接收结束
     */
    public void responseHeadersEnd(Call call, Response response) {
        super.responseHeadersEnd(call, response);
        recordEventLog("响应头结束");
    }

    @Override
    /**
     * 响应体开始接收
     */
    public void responseBodyStart(Call call) {
        super.responseBodyStart(call);
        recordEventLog("响应体开始");
    }

    @Override
    /**
     * 响应体接收结束
     */
    public void responseBodyEnd(Call call, long byteCount) {
        super.responseBodyEnd(call, byteCount);
        recordEventLog("响应体结束");
    }

    @Override
    /**
     * 整个调用结束
     */
    public void callEnd(Call call) {
        super.callEnd(call);
        recordEventLog("请求结束");
    }

    @Override
    /**
     * 调用失败
     */
    public void callFailed(Call call, IOException ioe) {
        super.callFailed(call, ioe);
        recordEventLog("请求失败 ×");
    }
}
