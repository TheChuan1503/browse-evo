package com.kongzue.baseokhttp.x.interfaces;

import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

import java.io.IOException;

import okhttp3.ResponseBody;

/**
 * 拦截并以字节数组形式处理返回内容的回调接口。
 */
public abstract class BytesResponseInterceptListener implements BaseResponseInterceptListener {

    @Override
    @Deprecated
    public boolean onIntercept(BaseHttpRequest httpRequest, ResponseBody response, Exception error) {
        try {
            byte[] result = response.bytes();
            return onIntercept(httpRequest, result, error);
        } catch (IOException e) {
            return onIntercept(httpRequest, new byte[]{}, e);
        }
    }

    /**
     * 字节数组拦截回调
     *
     * @param httpRequest 当前请求对象
     * @param response    响应字节数组
     * @param error       请求异常
     * @return 是否拦截后续回调
     */
    public abstract boolean onIntercept(BaseHttpRequest httpRequest, byte[] response, Exception error);
}
