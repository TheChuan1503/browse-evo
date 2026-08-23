package com.kongzue.baseokhttp.x.interfaces;

import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

import java.util.Map;

import okhttp3.ResponseBody;

public abstract class BytesMultiResponseListener implements BaseMultiResponseListener {

    @Override
    public void response(BaseHttpRequest[] httpRequests, Map<BaseHttpRequest, ResponseBody> responseBodyMap, Map<BaseHttpRequest, Exception> errorsMap) {
        byte[][] result = new byte[httpRequests.length][];
        Exception[] errors = new Exception[httpRequests.length];
        for (int i = 0; i < httpRequests.length; i++) {
            try {
                if (responseBodyMap.get(httpRequests[i]) != null) {
                    byte[] data = responseBodyMap.get(httpRequests[i]).bytes();
                    result[i] = data;
                } else {
                    result[i] = new byte[0];
                    errors[i] = errorsMap.get(httpRequests[i]);
                }
            } catch (Exception e) {
                result[i] = new byte[0];
                errors[i] = e;
            }
        }
        response(httpRequests, result, errors);
    }

    /**
     * 字节形式的回调
     *
     * @param httpRequests 当前请求对象集
     * @param responses    字节数据集
     * @param errors       请求异常集
     */
    public abstract void response(BaseHttpRequest[] httpRequests, byte[][] responses, Exception[] errors);
}
