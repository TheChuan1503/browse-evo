package com.kongzue.baseokhttp.x.interfaces;

import com.kongzue.baseokhttp.x.BaseOkHttpX;
import com.kongzue.baseokhttp.x.util.BaseHttpRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * OpenAI 流式接口专用回调，可逐行返回内容。
 */
public class OpenAIAPIResponseListener extends ResponseListener {

    StringBuilder resultBuilder = new StringBuilder();
    boolean isFinish;
    String optimizationContent;

    @Override
    public void response(BaseHttpRequest httpRequest, String line, Exception error) {
        if (error == null) {
            if (line.startsWith("data: ")) {
                String jsonData = line.substring(6).trim();
                String contentChunk = parseResponseJson(jsonData);
                if (jsonData.equals("[DONE]")) {
                    httpRequest.setRequesting(false);
                    isFinish = true;
                    onResponse(httpRequest, contentChunk, optimizationContent, error, isFinish);
                    onFinish(httpRequest, optimizationContent, error);
                    return;
                }
                resultBuilder.append(contentChunk);
                optimizationContent = resultBuilder.toString().replaceAll("\n\n", "\n　　");
                onResponse(httpRequest, contentChunk, optimizationContent, error, isFinish);
            }
        } else {
            onResponse(httpRequest, "", optimizationContent, error, isFinish);
        }
    }

    private String parseResponseJson(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            JSONArray choices = json.getJSONArray("choices");
            if (choices.length() > 0) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject delta = choice.getJSONObject("delta");
                return delta.optString("content", "");
            }
        } catch (JSONException e) {
            if (BaseOkHttpX.debugMode) {
                e.printStackTrace();
            }
        }
        return "";
    }

    /**
     * 流式文本回调
     *
     * @param httpRequest       当前请求对象
     * @param subText           本次返回的增量文本
     * @param fullResponseText  截止目前的完整文本
     * @param error             请求异常
     * @param isFinish          是否已经完成
     */
    public void onResponse(BaseHttpRequest httpRequest, String subText, String fullResponseText, Exception error, boolean isFinish) {
    }

    /**
     * 流式请求结束回调
     *
     * @param httpRequest      当前请求对象
     * @param fullResponseText 完整的返回内容
     * @param error            请求异常
     */
    public void onFinish(BaseHttpRequest httpRequest, String fullResponseText, Exception error) {

    }
}