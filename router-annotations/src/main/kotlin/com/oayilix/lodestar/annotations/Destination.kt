package com.oayilix.lodestar.annotations

/**
 * 标记页面路由目标。
 * 编译期注解处理器会扫描所有标注了此注解的类，生成路由映射表。
 *
 * @property url 页面定义的 url，不能为空
 * @property description 定义当前页面的描述
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Destination(
    val url: String,
    val description: String = "no description"
)
