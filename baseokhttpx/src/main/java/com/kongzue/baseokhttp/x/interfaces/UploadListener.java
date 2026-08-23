package com.kongzue.baseokhttp.x.interfaces;

import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

import org.jetbrains.annotations.Nullable;

/**
 * 上传进度回调接口。
 */
public interface UploadListener {

    /**
     * 上传进度回调
     *
     * @param httpRequest 当前请求对象
     * @param progress    进度（0~1）
     * @param current     已上传字节数
     * @param total       总字节数
     * @param done        是否完成
     * @param error       异常信息
     */
    void onUpload(BaseHttpRequest httpRequest, float progress, long current, long total, boolean done, @Nullable Exception error);
}
