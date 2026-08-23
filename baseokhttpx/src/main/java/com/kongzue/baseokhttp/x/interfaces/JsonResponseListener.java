package com.kongzue.baseokhttp.x.interfaces;

import com.kongzue.baseokhttp.util.JsonMap;
import com.kongzue.baseokhttp.x.exceptions.DecodeJsonException;
import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

/**
 * 将服务器返回值转换为 {@link JsonMap} 的回调接口。
 */
public abstract class JsonResponseListener extends ResponseListener {

    @Override
    public void response(BaseHttpRequest httpRequest, String response, Exception error) {
        if (error == null) {
            JsonMap data = new JsonMap(response);
            if (!data.isEmpty()) {
                onResponse(httpRequest, data, null);
            } else {
                onResponse(httpRequest, new JsonMap(), new DecodeJsonException(response));
            }
        } else {
            onResponse(httpRequest, new JsonMap(), error);
        }
    }

    /**
     * 返回 JsonMap 数据的回调
     *
     * @param httpRequest 当前请求对象
     * @param main        解析后的 JsonMap 数据
     * @param error       请求或解析异常
     */
    public abstract void onResponse(BaseHttpRequest httpRequest, JsonMap main, Exception error);
}
