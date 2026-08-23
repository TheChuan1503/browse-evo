package com.kongzue.baseokhttp.x.interfaces;

import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

import java.io.IOException;

import okhttp3.ResponseBody;

/**
 * 字符串形式的响应拦截器。
 */
public abstract class ResponseInterceptListener implements BaseResponseInterceptListener {

    @Override
    @Deprecated
    public boolean onIntercept(BaseHttpRequest httpRequest, ResponseBody response, Exception error) {
        try {
            String result = response.string();
            return onIntercept(httpRequest, result, error);
        } catch (IOException e) {
            return onIntercept(httpRequest, "", e);
        }
    }

    /**
     * 字符串数据拦截回调
     *
     * @param httpRequest 当前请求对象
     * @param response    字符串形式的响应内容
     * @param error       异常信息
     * @return 是否拦截后续回调
     */
    public abstract boolean onIntercept(BaseHttpRequest httpRequest, String response, Exception error);
}