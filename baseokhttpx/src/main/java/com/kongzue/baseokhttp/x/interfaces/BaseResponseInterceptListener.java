package com.kongzue.baseokhttp.x.interfaces;

import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

import okhttp3.ResponseBody;

/**
 * 用于拦截所有请求返回的数据。
 * 在此可以统一处理响应结果，当返回 {@code true} 时表示拦截，
 * 将不再继续向后回调。
 */
public interface BaseResponseInterceptListener {

    /**
     * 拦截回调
     *
     * @param httpRequest 发起请求的对象
     * @param response    原始响应体
     * @param error       请求中出现的异常，为 {@code null} 表示没有异常
     * @return {@code true} 表示已拦截此次回调
     */
    boolean onIntercept(BaseHttpRequest httpRequest, ResponseBody response, Exception error);
}
