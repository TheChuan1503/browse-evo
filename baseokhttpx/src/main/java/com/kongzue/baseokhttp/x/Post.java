package com.kongzue.baseokhttp.x;

import android.content.Context;

import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

/**
 * POST 请求封装类。
 */
public class Post extends BaseHttpRequest {

    /**
     * 创建仅包含 URL 的 POST 请求
     *
     * @param url 请求地址
     */
    public Post(String url) {
        this.requestType = REQUEST_TYPE.POST;
        this.url = url;
    }

    /**
     * 创建绑定生命周期的 POST 请求
     *
     * @param context 生命周期所属的 Context
     * @param url     请求地址
     */
    public Post(Context context, String url) {
        this.requestType = REQUEST_TYPE.POST;
        this.url = url;
        this.bindLifecycleOwner(context);
    }

    /**
     * 创建 POST 请求对象
     *
     * @param url 请求地址
     * @return Post 实例
     */
    public static Post postRequest(String url) {
        return new Post(url);
    }

    /**
     * 创建绑定生命周期的 POST 请求对象
     *
     * @param context 生命周期 Context
     * @param url     请求地址
     * @return Post 实例
     */
    public static Post postRequest(Context context, String url) {
        return new Post(context, url);
    }

    /**
     * 与 {@link #postRequest(String)} 功能一致
     */
    public static Post create(String url) {
        return new Post(url);
    }

    /**
     * 与 {@link #postRequest(Context, String)} 功能一致
     */
    public static Post create(Context context, String url) {
        return new Post(context, url);
    }

}
