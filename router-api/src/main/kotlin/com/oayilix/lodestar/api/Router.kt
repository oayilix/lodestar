package com.oayilix.lodestar.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

object Router {

    private const val TAG = "Router"

    // 编译期间生成的总映射表
    private const val GENERATED_MAPPING = "com.oayilix.lodestar.mapping.RouterMapping"

    // 存储所有映射表信息
    private val mapping = HashMap<String, String>()

    fun init() {
        // 反射获取 GENERATED_MAPPING 类的 get() 方法
        runCatching {
            val clazz = Class.forName(GENERATED_MAPPING)
            val getMethod = clazz.getMethod("get")
            @Suppress("UNCHECKED_CAST")
            val allMapping = getMethod.invoke(null) as? Map<String, String>
            allMapping?.let {
                Log.i(TAG, "init: get all mapping")
                mapping.putAll(it)
                mapping.forEach { (key, value) ->
                    Log.i(TAG, "mapping: key = $key, value = $value")
                }
            }
        }.onFailure { e ->
            Log.e(TAG, "init failed", e)
        }
    }

    fun navigation(context: Context, url: String) {
        if (url.isEmpty()) {
            Log.i(TAG, "navigation called: param error")
            return
        }

        // 1、匹配 url，找到目标页面
        val uri = Uri.parse(url)
        val scheme = uri.scheme
        val host = uri.host
        val path = uri.path

        val targetActivityClass = mapping.entries.firstOrNull { (key, _) ->
            val sUri = Uri.parse(key)
            scheme == sUri.scheme && host == sUri.host && path == sUri.path
        }?.value

        if (targetActivityClass.isNullOrEmpty()) {
            Log.i(TAG, "navigation called: no destination found")
            return
        }

        // 2、打开对应页面
        runCatching {
            val clazz = Class.forName(targetActivityClass)
            context.startActivity(Intent(context, clazz))
        }.onFailure { e ->
            Log.e(TAG, "navigation failed", e)
        }
    }
}
