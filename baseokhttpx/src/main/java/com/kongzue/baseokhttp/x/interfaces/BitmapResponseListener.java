package com.kongzue.baseokhttp.x.interfaces;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

import java.io.InputStream;

import okhttp3.ResponseBody;

/**
 * 将返回内容转换为 {@link Bitmap} 的回调接口。
 */
public abstract class BitmapResponseListener implements BaseResponseListener {

    @Override
    @Deprecated
    public void response(BaseHttpRequest httpRequest, ResponseBody responseBody, Exception error) {
        if (error != null) {
            response(httpRequest, Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888), error);
            return;
        }
        try {
            InputStream inputStream = responseBody.byteStream();
            response(httpRequest, BitmapFactory.decodeStream(inputStream), error);
        } catch (Exception e) {
            response(httpRequest, Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888), e);
        }
    }

    /**
     * Bitmap 类型回调
     *
     * @param httpRequest 当前请求对象
     * @param response    解码后的位图
     * @param error       请求或解析过程中的异常
     */
    public abstract void response(BaseHttpRequest httpRequest, Bitmap response, Exception error);
}