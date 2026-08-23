package com.kongzue.baseokhttp.x.interfaces;

import com.kongzue.baseokhttp.util.JsonBean;
import com.kongzue.baseokhttp.util.JsonMap;
import com.kongzue.baseokhttp.x.exceptions.DecodeJsonException;
import com.kongzue.baseokhttp.x.exceptions.InstanceBeanException;
import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

import java.lang.reflect.ParameterizedType;

public abstract class BeanMultiResponseListener<T> extends MultiResponseListener {
    @Override
    public void response(BaseHttpRequest[] httpRequests, String[] responses, Exception[] errors) {
        T[] results = (T[]) new Object[responses.length];
        Class<T> tClass;
        ParameterizedType pt = (ParameterizedType) this.getClass().getGenericSuperclass();
        tClass = (Class<T>) pt.getActualTypeArguments()[0];
        for (int i = 0; i < responses.length; i++) {
            try {
                JsonMap data = new JsonMap(responses[i]);
                if (data.isEmpty()) {
                    errors[i] = new DecodeJsonException(responses[i]);
                } else {
                    T tInstance = JsonBean.getBean(data, tClass);
                    results[i] = tInstance;
                }
            } catch (Exception e) {
                //这种情况下没办法实例化泛型对象
                errors[i] = new InstanceBeanException("请检查该 Bean 是否为 public 且其构造方法为 public");
                return;
            }
        }
        onResponse(httpRequests, results, errors);
    }

    /**
     * 解析后的 Bean 对象回调集
     *
     * @param httpRequests 请求对象集
     * @param dataArray    解析后的数据对象集
     * @param errors       解析或请求过程中产生的异常集
     */
    public abstract void onResponse(BaseHttpRequest[] httpRequests, T[] dataArray, Exception[] errors);
}
