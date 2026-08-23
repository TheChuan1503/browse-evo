package com.kongzue.baseokhttp.x;

import android.content.Context;

import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

/**
 * PUT 请求封装类。
 */
public class Put extends BaseHttpRequest {

    /**
     * 创建仅包含 URL 的 PUT 请求
     *
     * @param url 请求地址
     */
    public Put(String url) {
        this.requestType = REQUEST_TYPE.PUT;
        this.url = url;
    }

    /**
     * 创建绑定生命周期的 PUT 请求
     *
     * @param context 生命周期所属的 Context
     * @param url     请求地址
     */
    public Put(Context context, String url) {
        this.requestType = REQUEST_TYPE.PUT;
        this.url = url;
        this.bindLifecycleOwner(context);
    }

    /**
     * 创建 PUT 请求对象
     *
     * @param url 请求地址
     * @return Put 实例
     */
    public static Put putRequest(String url) {
        return new Put(url);
    }

    /**
     * 创建绑定生命周期的 PUT 请求对象
     *
     * @param context 生命周期 Context
     * @param url     请求地址
     * @return Put 实例
     */
    public static Put putRequest(Context context, String url) {
        return new Put(context, url);
    }

    /**
     * 与 {@link #putRequest(String)} 功能一致
     */
    public static Put create(String url) {
        return new Put(url);
    }

    /**
     * 与 {@link #putRequest(Context, String)} 功能一致
     */
    public static Put create(Context context, String url) {
        return new Put(context, url);
    }
}
