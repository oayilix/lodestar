package com.oayilix.lodestar.annotations

/**
 * Marks an Activity that can be navigated to by Lodestar.
 * 标记可由 Lodestar 导航的 Activity。
 *
 * The compile-time processor validates the route and target type, then generates a deterministic
 * module registry.
 * 编译期处理器会校验路由格式与目标类型，并生成确定性的模块路由表。
 *
 * @property url An absolute route URI without query or fragment. 不含 query 或 fragment 的绝对路由 URI。
 * @property description A human-readable description for docs and diagnostics. 供文档和诊断使用的页面描述。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Destination(
    val url: String,
    val description: String = "no description"
)
