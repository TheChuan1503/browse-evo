package com.kongzue.baseokhttp.x.exceptions;

public class DecodeJsonException extends Exception {
    public DecodeJsonException(String errorInfo){
        super("Json解析失败：\n" + errorInfo);
    }
}
