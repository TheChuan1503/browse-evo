package com.kongzue.baseokhttp.x;

import android.content.Context;

import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

/**
 * GET 请求封装类。
 */
public class Get extends BaseHttpRequest {

    /**
     * 创建仅包含 URL 的 GET 请求
     *
     * @param url 请求地址
     */
    public Get(String url) {
        this.requestType = REQUEST_TYPE.GET;
        this.url = url;
    }

    /**
     * 创建绑定生命周期的 GET 请求
     *
     * @param context 生命周期所属的 Context
     * @param url     请求地址
     */
    public Get(Context context, String url) {
        this.requestType = REQUEST_TYPE.GET;
        this.url = url;
        this.bindLifecycleOwner(context);
    }

    /**
     * 创建 GET 请求对象
     *
     * @param url 请求地址
     * @return Get 实例
     */
    public static Get getRequest(String url) {
        return new Get(url);
    }

    /**
     * 创建绑定生命周期的 GET 请求对象
     *
     * @param context 生命周期 Context
     * @param url     请求地址
     * @return Get 实例
     */
    public static Get getRequest(Context context, String url) {
        return new Get(context, url);
    }

    /**
     * 与 {@link #getRequest(String)} 功能一致
     */
    public static Get create(String url) {
        return new Get(url);
    }

    /**
     * 与 {@link #getRequest(Context, String)} 功能一致
     */
    public static Get create(Context context, String url) {
        return new Get(context, url);
    }
}
