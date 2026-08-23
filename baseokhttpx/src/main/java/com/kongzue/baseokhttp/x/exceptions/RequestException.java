package com.kongzue.baseokhttp.x.exceptions;

import okhttp3.Call;

public class RequestException extends Exception {

    Call call;
    int requestCode;

    public RequestException(Call call, int requestCode) {
        super("请求失败：requestCode=" + requestCode);
        this.requestCode = requestCode;
        this.call = call;
    }

    public Call getCall() {
        return call;
    }

    public int getRequestCode() {
        return requestCode;
    }
}
