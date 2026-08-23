package com.kongzue.baseokhttp.x.exceptions;

import com.kongzue.baseokhttp.x.util.RequestInfo;

public class SameRequestException extends Exception{

    public SameRequestException(RequestInfo requestInfo) {
        super("拦截重复请求:" + requestInfo);
    }
}
