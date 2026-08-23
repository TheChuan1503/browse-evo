package com.kongzue.baseokhttp.x.util;

import com.kongzue.baseokhttp.x.interfaces.ResponseListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Kongzue
 * @github: https://github.com/kongzue/
 * @homepage: http://kongzue.com/
 * @mail: myzcxhh@live.cn
 * @createTime: 2020/11/4 12:48
 */
public class RequestInfo {

    private String url;
    private String parameter;

    private List<ResponseListener> sameRequestCallbacks = new ArrayList<ResponseListener>();

    /**
     * 构造请求信息
     *
     * @param url       请求地址
     * @param parameter 请求参数字符串
     */
    public RequestInfo(String url, String parameter) {
        this.url = url;
        this.parameter = parameter;
    }

    /**
     * 构造请求信息
     *
     * @param url       请求地址
     * @param parameter 参数对象
     */
    public RequestInfo(String url, Parameter parameter) {
        this.url = url;
        this.parameter = parameter == null ? "" : parameter.toParameterString();
    }

    /** 获取请求地址 */
    public String getUrl() {
        return url;
    }

    /** 设置请求地址 */
    public RequestInfo setUrl(String url) {
        this.url = url;
        return this;
    }

    /** 获取请求参数字符串 */
    public String getParameter() {
        return parameter;
    }

    /** 设置请求参数字符串 */
    public RequestInfo setParameter(String parameter) {
        this.parameter = parameter;
        return this;
    }

    /**
     * 判断是否为相同请求
     */
    public boolean equals(RequestInfo requestInfo) {
        if (this == requestInfo) return true;
        if (requestInfo == null || getClass() != requestInfo.getClass()) return false;
        return equalsString(url, requestInfo.url) && equalsString(parameter, requestInfo.parameter);
    }

    private boolean equalsString(String a, String b) {
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    /**
     * 添加同一请求的回调
     */
    public void addSameRequestCallback(ResponseListener listener) {
        sameRequestCallbacks.add(listener);
    }

    /**
     * 获取同一请求回调列表
     */
    public List<ResponseListener> getSameRequestCallbacks() {
        return sameRequestCallbacks;
    }

    @Override
    /** 返回调试用字符串 */
    public String toString() {
        return "RequestInfo{" +
                "url='" + url + '\'' +
                ", parameter='" + parameter + '\'' +
                '}';
    }

    /** 清空记录的请求列表 */
    public static void cleanSameRequestList() {
        requestInfoList = new ArrayList<>();
    }

    private static List<RequestInfo> requestInfoList;

    /** 添加请求信息到列表 */
    public static void addRequestInfo(RequestInfo requestInfo) {
        synchronized (RequestInfo.class) {
            if (requestInfoList == null) {
                requestInfoList = new ArrayList<>();
            }
            requestInfoList.add(requestInfo);
        }
    }

    /** 从列表移除请求信息 */
    public static void deleteRequestInfo(RequestInfo requestInfo) {
        synchronized (RequestInfo.class) {
            if (requestInfoList == null || requestInfoList.isEmpty() || requestInfo == null) {
                return;
            }
            requestInfoList.remove(requestInfo);
        }
    }

    /**
     * 判断给定请求是否已存在
     */
    public static RequestInfo equalsRequestInfo(RequestInfo requestInfo) {
        synchronized (RequestInfo.class) {
            if (requestInfoList == null || requestInfoList.isEmpty()) {
                return null;
            }
            for (RequestInfo requestInfo1 : requestInfoList) {
                if (requestInfo1.equals(requestInfo)) {
                    return requestInfo1;
                }
            }
            return null;
        }
    }
}
