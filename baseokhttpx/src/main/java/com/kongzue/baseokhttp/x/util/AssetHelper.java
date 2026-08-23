package com.kongzue.baseokhttp.x.util;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class AssetHelper {

    public static String readTextFromAssets(Context context, String fileName) {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader reader = null;
        try {
            InputStream is = context.getAssets().open(fileName);
            reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) { }
            }
        }
        return stringBuilder.toString();
    }
}
