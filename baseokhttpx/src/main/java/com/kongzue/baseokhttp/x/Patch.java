package com.kongzue.baseokhttp.x;

import android.content.Context;

import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

/**
 * PATCH 请求封装类。
 */
public class Patch extends BaseHttpRequest {

    /**
     * 创建仅包含 URL 的 PATCH 请求
     *
     * @param url 请求地址
     */
    public Patch(String url) {
        this.requestType = REQUEST_TYPE.PATCH;
        this.url = url;
    }

    /**
     * 创建绑定生命周期的 PATCH 请求
     *
     * @param context 生命周期所属的 Context
     * @param url     请求地址
     */
    public Patch(Context context, String url) {
        this.requestType = REQUEST_TYPE.PATCH;
        this.url = url;
        this.bindLifecycleOwner(context);
    }

    /**
     * 创建 PATCH 请求对象
     *
     * @param url 请求地址
     * @return Patch 实例
     */
    public static Patch patchRequest(String url) {
        return new Patch(url);
    }

    /**
     * 创建绑定生命周期的 PATCH 请求对象
     *
     * @param context 生命周期 Context
     * @param url     请求地址
     * @return Patch 实例
     */
    public static Patch patchRequest(Context context, String url) {
        return new Patch(context, url);
    }

    /**
     * 与 {@link #patchRequest(String)} 功能一致
     */
    public static Patch create(String url) {
        return new Patch(url);
    }

    /**
     * 与 {@link #patchRequest(Context, String)} 功能一致
     */
    public static Patch create(Context context, String url) {
        return new Patch(context, url);
    }
}
