package com.kongzue.baseokhttp.x.interfaces;

import com.kongzue.baseokhttp.util.JsonMap;
import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

public abstract class JsonMultiResponseListener extends MultiResponseListener {
    @Override
    public void response(BaseHttpRequest[] httpRequests, String[] responses, Exception[] errors) {
        JsonMap[] result = new JsonMap[responses.length];
        for (int i = 0; i < responses.length; i++) {
            result[i] = new JsonMap(responses[i]);
        }
        onResponse(httpRequests, result, errors);
    }

    /**
     * 返回 JsonMap 数据的回调
     *
     * @param httpRequests 所有请求对象集
     * @param mainArray    解析后的 JsonMap 数据集
     * @param errors       请求或解析异常集
     */
    public abstract void onResponse(BaseHttpRequest[] httpRequests, JsonMap[] mainArray, Exception[] errors);
}
