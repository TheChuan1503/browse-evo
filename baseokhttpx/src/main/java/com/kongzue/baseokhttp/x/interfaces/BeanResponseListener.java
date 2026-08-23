package com.kongzue.baseokhttp.x.interfaces;

import com.kongzue.baseokhttp.util.JsonBean;
import com.kongzue.baseokhttp.util.JsonMap;
import com.kongzue.baseokhttp.x.exceptions.DecodeJsonException;
import com.kongzue.baseokhttp.x.exceptions.InstanceBeanException;
import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

import java.lang.reflect.ParameterizedType;

/**
 * 将服务器返回的 Json 数据自动解析为泛型 Bean 的回调接口。
 *
 * @param <T> 目标 Bean 类型
 */
public abstract class BeanResponseListener<T> extends ResponseListener {

    @Override
    public void response(BaseHttpRequest httpRequest, String response, Exception error) {
        T tInstance = null;
        Class<T> tClass;
        try {
            ParameterizedType pt = (ParameterizedType) this.getClass().getGenericSuperclass();
            tClass = (Class<T>) pt.getActualTypeArguments()[0];
            tInstance = tClass.newInstance();
        } catch (Exception e) {
            //这种情况下没办法实例化泛型对象
            onResponse(httpRequest, null, new InstanceBeanException("请检查该 Bean 是否为 public 且其构造方法为 public"));
            return;
        }
        if (error == null) {
            JsonMap data = new JsonMap(response.toString());
            if (data.isEmpty()) {
                onResponse(httpRequest, tInstance, new DecodeJsonException(response.toString()));
            }
            tInstance = JsonBean.getBean(data, tClass);

            onResponse(httpRequest, tInstance, null);
        } else {
            onResponse(httpRequest, tInstance, error);
        }
    }

    /**
     * 解析后的 Bean 对象回调
     *
     * @param httpRequest 当前请求对象
     * @param data        解析后的数据对象
     * @param e           解析或请求过程中产生的异常
     */
    public abstract void onResponse(BaseHttpRequest httpRequest, T data, Exception e);
}
