package com.oayilix.lodestar.mapping

import android.app.Activity

/**
 * Test-only aggregate mapping that mirrors the Gradle plugin generated entry point.
 * 仅测试使用的聚合映射表，用于模拟 Gradle 插件生成的入口类。
 */
object LodestarMapping {

    /**
     * Exposes the same static `get()` method shape as the production generated class.
     * 暴露与生产环境生成类一致的静态 `get()` 方法形态。
     */
    @JvmStatic
    fun get(): Map<String, Class<out Activity>> =
        mapOf("lodestar://example.com/app/test" to TestActivity::class.java)

    class TestActivity : Activity()
}
