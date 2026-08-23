package com.kongzue.baseokhttp.x.interfaces;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

import java.util.Map;

import okhttp3.ResponseBody;

public abstract class BitmapMultiResponseListener implements BaseMultiResponseListener {

    @Override
    public void response(BaseHttpRequest[] httpRequests, Map<BaseHttpRequest, ResponseBody> responseBodyMap, Map<BaseHttpRequest, Exception> errorsMap) {
        Bitmap[] result = new Bitmap[httpRequests.length];
        Exception[] errors = new Exception[httpRequests.length];
        for (int i = 0; i < httpRequests.length; i++) {
            try {
                if (responseBodyMap.get(httpRequests[i]) != null) {
                    result[i] = BitmapFactory.decodeStream(responseBodyMap.get(httpRequests[i]).byteStream());
                } else {
                    result[i] = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                    errors[i] = errorsMap.get(httpRequests[i]);
                }
            } catch (Exception e) {
                result[i] = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                errors[i] = e;
            }
        }
        response(httpRequests, result, errors);
    }

    /**
     * 位图形式的回调
     *
     * @param httpRequests 请求对象集
     * @param responses    位图集
     * @param errors       请求异常集
     */
    public abstract void response(BaseHttpRequest[] httpRequests, Bitmap[] responses, Exception[] errors);
}
