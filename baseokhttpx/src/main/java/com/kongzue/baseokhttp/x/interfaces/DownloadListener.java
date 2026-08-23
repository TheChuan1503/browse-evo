package com.kongzue.baseokhttp.x.interfaces;

import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * 下载进度回调接口。
 */
public interface DownloadListener {

    /**
     * 下载过程回调
     *
     * @param httpRequest 请求对象
     * @param downloadFile 下载的目标文件
     * @param progress     当前进度（0~1）
     * @param current      已完成字节数
     * @param total        文件总大小
     * @param done         是否下载完成
     * @param error        异常信息
     */
    void onDownload(BaseHttpRequest httpRequest, File downloadFile, float progress, long current, long total, boolean done, @Nullable Exception error);
}
