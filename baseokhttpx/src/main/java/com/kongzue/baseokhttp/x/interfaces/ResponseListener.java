package com.kongzue.baseokhttp.x.interfaces;

import android.graphics.Bitmap;

import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

import okhttp3.ResponseBody;

/**
 * 将返回数据以字符串形式处理的回调抽象类。
 */
public abstract class ResponseListener implements BaseResponseListener  {

    @Override
    @Deprecated
    public void response(BaseHttpRequest httpRequest, ResponseBody responseBody, Exception error) {
        if (error != null) {
            response(httpRequest, "", error);
            return;
        }
        try {
            String data = responseBody.string();
            response(httpRequest, data, error);
        } catch (Exception e) {
            response(httpRequest, "", e);
        }
    }

    /**
     * 字符串形式的回调
     *
     * @param httpRequest 当前请求对象
     * @param response    字符串数据
     * @param error       请求异常
     */
    public abstract void response(BaseHttpRequest httpRequest, String response, Exception error);
}
