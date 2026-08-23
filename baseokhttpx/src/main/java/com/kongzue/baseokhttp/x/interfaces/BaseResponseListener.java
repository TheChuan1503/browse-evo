package com.kongzue.baseokhttp.x.interfaces;

import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

import okhttp3.ResponseBody;

/**
 * 通用请求回调接口，直接返回 OkHttp 的 {@link ResponseBody}。
 * 所有自定义回调均应实现或继承此接口。
 */
public interface BaseResponseListener {

    /**
     * 请求结果回调
     *
     * @param httpRequest 当前请求对象
     * @param responseBody OkHttp 原始响应体
     * @param error        请求异常，为 {@code null} 表示请求成功
     */
    void response(BaseHttpRequest httpRequest, ResponseBody responseBody, Exception error);

}
