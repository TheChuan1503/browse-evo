package com.kongzue.baseokhttp.x.interfaces;

import com.kongzue.baseokhttp.x.util.BaseHttpRequest;
import com.kongzue.baseokhttp.x.util.Parameter;

/**
 * 请求头拦截器，可在此对 Header 进行统一处理。
 */
public interface HeaderInterceptListener {
    /**
     * 拦截并返回新的 Header
     *
     * @param httpRequest 当前请求对象
     * @param url         请求地址
     * @param originHeader 原始 Header
     * @return 处理后的 Header
     */
    Parameter onIntercept(BaseHttpRequest httpRequest, String url, Parameter originHeader);
}
