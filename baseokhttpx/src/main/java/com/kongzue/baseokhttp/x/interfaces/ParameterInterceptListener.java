package com.kongzue.baseokhttp.x.interfaces;

import android.content.Context;

import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

/**
 * Author: @Kongzue
 * Github: https://github.com/kongzue/
 * Homepage: http://kongzue.com/
 * Mail: myzcxhh@live.cn
 * CreateTime: 2018/11/22 17:27
 */
public interface ParameterInterceptListener {
    /**
     * 拦截并返回新的请求参数
     *
     * @param httpRequest 当前请求对象
     * @param url         请求地址
     * @param parameter   原始参数
     * @return 处理后的参数对象
     */
    Object onIntercept(BaseHttpRequest httpRequest, String url, Object parameter);
}
