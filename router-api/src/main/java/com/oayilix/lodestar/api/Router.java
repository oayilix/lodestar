package com.oayilix.lodestar.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Router {

    private static final String TAG = "Router";

    // 编译期间生成的总映射表
    private static final String GENERATED_MAPPING = "com.oayilix.lodestar.mapping.RouterMapping";

    // 存储所有映射表信息
    private static Map<String, String> mapping = new HashMap<>();

    public static void init() {
        // 反射获取 GENERATED_MAPPING 类的 get() 方法
        try {
            Class<?> clazz = Class.forName(GENERATED_MAPPING);
            Method getMethod = clazz.getMethod("get");
            Map<String, String> allMapping = (Map<String, String>) getMethod.invoke(null);
            if (allMapping != null && !allMapping.isEmpty()) {
                Log.i(TAG, "init: get all mapping");
                mapping.putAll(allMapping);
                Set<Map.Entry<String, String>> entrySet = mapping.entrySet();
                for (Map.Entry<String, String> entry : entrySet) {
                    Log.i(TAG, "mapping: key = " + entry.getKey() + ", value = " + entry.getValue());
                }
            }
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "init called: " + e);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "init called: " + e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "init called: " + e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "init called: " + e);
        }
    }

    public static void navigation(Context context, String url) {
        if (context == null || TextUtils.isEmpty(url)) {
            Log.i(TAG, "navigation called: param error");
            return;
        }
        // 1、匹配 url，找到目标页面
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        String path = uri.getPath();

        String targetActivityClass = "";
        Set<Map.Entry<String, String>> entries = mapping.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            Uri sUri = Uri.parse(entry.getKey());
            String sScheme = sUri.getScheme();
            String sHost = sUri.getHost();
            String sPath = sUri.getPath();

            if (TextUtils.equals(scheme, sScheme)
                    && TextUtils.equals(host, sHost)
                    && TextUtils.equals(path, sPath)) {
                targetActivityClass = entry.getValue();
            }
        }

        if (TextUtils.isEmpty(targetActivityClass)) {
            Log.i(TAG, "navigation called: no destination found");
            return;
        }

        // 2、打开对应页面
        try {
            Class<?> clazz = Class.forName(targetActivityClass);
            Intent intent = new Intent(context, clazz);
            context.startActivity(intent);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "navigation called: " + e);
        }
    }
}
