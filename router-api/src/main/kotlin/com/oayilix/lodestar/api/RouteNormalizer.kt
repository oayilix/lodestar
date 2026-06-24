package com.oayilix.lodestar.api

import java.net.URI
import java.util.Locale

/**
 * Converts a caller URI into a stable `scheme://host/path` route key.
 * 将调用方 URI 转换为稳定的 `scheme://host/path` 路由键。
 *
 * Scheme and host are case-insensitive; query and fragment are left for the destination to parse.
 * scheme 与 host 不区分大小写，query 和 fragment 被保留给目标页面自行解析。
 */
internal object RouteNormalizer {

    fun normalize(value: String): String? = runCatching {
        if (value.isBlank() || value != value.trim()) return null
        val uri = URI(value)
        if (!uri.isAbsolute || uri.scheme.isNullOrBlank() || uri.host.isNullOrBlank()) return null
        val path = uri.normalize().rawPath?.takeIf(String::isNotEmpty) ?: "/"
        "${uri.scheme.lowercase(Locale.ROOT)}://${uri.host.lowercase(Locale.ROOT)}$path"
    }.getOrNull()
}
