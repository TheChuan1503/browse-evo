package com.kongzue.baseokhttp.x.interfaces;

import android.graphics.Bitmap;

import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

import okhttp3.ResponseBody;

/**
 * 返回值以字节数组形式回调的接口。
 */
public abstract class BytesResponseListener implements BaseResponseListener {

    @Override
    @Deprecated
    public void response(BaseHttpRequest httpRequest, ResponseBody responseBody, Exception error) {
        if (error != null) {
            response(httpRequest, new byte[0], error);
            return;
        }
        try {
            byte[] data = responseBody.bytes();
            response(httpRequest, data, error);
        } catch (Exception e) {
            response(httpRequest, new byte[]{}, e);
        }
    }

    /**
     * 字节数组返回回调
     *
     * @param httpRequest 当前请求对象
     * @param response    字节数组形式的响应内容
     * @param error       请求或解析异常
     */
    public abstract void response(BaseHttpRequest httpRequest, byte[] response, Exception error);
}
