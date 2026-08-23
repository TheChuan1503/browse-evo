package com.kongzue.baseokhttp.x.interfaces;

import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

import java.util.Map;

import okhttp3.ResponseBody;

public abstract class MultiResponseListener implements BaseMultiResponseListener {

    @Override
    public void response(BaseHttpRequest[] httpRequests, Map<BaseHttpRequest, ResponseBody> responseBodyMap, Map<BaseHttpRequest, Exception> errorsMap) {
        String[] result = new String[httpRequests.length];
        Exception[] errors = new Exception[httpRequests.length];
        for (int i = 0; i < httpRequests.length; i++) {
            try {
                if (responseBodyMap.get(httpRequests[i]) != null) {
                    String data = responseBodyMap.get(httpRequests[i]).string();
                    result[i] = data;
                } else {
                    result[i] = "";
                    errors[i] = errorsMap.get(httpRequests[i]);
                }
            } catch (Exception e) {
                result[i] = "";
                errors[i] = e;
            }
        }
        response(httpRequests, result, errors);
    }

    /**
     * 字符串形式的回调
     *
     * @param httpRequests 当前请求对象
     * @param responses    字符串数据
     * @param errors       请求异常
     */
    public abstract void response(BaseHttpRequest[] httpRequests, String[] responses, Exception[] errors);
}
