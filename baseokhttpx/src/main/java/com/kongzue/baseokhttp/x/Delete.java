package com.kongzue.baseokhttp.x;

import android.content.Context;

import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

/**
 * DELETE 请求封装类。
 */
public class Delete extends BaseHttpRequest {

    /**
     * 创建仅包含 URL 的 DELETE 请求
     *
     * @param url 请求地址
     */
    public Delete(String url) {
        this.requestType = REQUEST_TYPE.DELETE;
        this.url = url;
    }

    /**
     * 创建绑定生命周期的 DELETE 请求
     *
     * @param context 生命周期所属的 Context
     * @param url     请求地址
     */
    public Delete(Context context, String url) {
        this.requestType = REQUEST_TYPE.DELETE;
        this.url = url;
        this.bindLifecycleOwner(context);
    }

    /**
     * 创建 DELETE 请求对象
     *
     * @param url 请求地址
     * @return Delete 实例
     */
    public static Delete deleteRequest(String url) {
        return new Delete(url);
    }

    /**
     * 创建绑定生命周期的 DELETE 请求对象
     *
     * @param context 生命周期 Context
     * @param url     请求地址
     * @return Delete 实例
     */
    public static Delete deleteRequest(Context context, String url) {
        return new Delete(context, url);
    }

    /**
     * 与 {@link #deleteRequest(String)} 功能一致
     */
    public static Delete create(String url) {
        return new Delete(url);
    }

    /**
     * 与 {@link #deleteRequest(Context, String)} 功能一致
     */
    public static Delete create(Context context, String url) {
        return new Delete(context, url);
    }
}
