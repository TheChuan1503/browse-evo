package com.kongzue.baseokhttp.x.interfaces;

import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

import java.util.Map;

import okhttp3.ResponseBody;

/**
 * 通用多请求合并回调接口，返回每个请求的的 {@link ResponseBody}。
 * 所有其他多请求合并回调均应实现或继承此接口。
 */
public interface BaseMultiResponseListener {

    /**
     * 请求结果回调
     *
     * @param httpRequests 当前请求对象
     * @param responseBodyMap 每个请求对象对应的响应体
     * @param errors        每个请求对象对应的请求异常，为 {@code null} 表示请求成功
     */
    void response(BaseHttpRequest[] httpRequests, Map<BaseHttpRequest, ResponseBody> responseBodyMap, Map<BaseHttpRequest, Exception> errors);
}
