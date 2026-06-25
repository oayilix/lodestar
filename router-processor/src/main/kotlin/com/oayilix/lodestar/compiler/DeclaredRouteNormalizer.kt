package com.oayilix.lodestar.compiler

import java.net.URI
import java.util.Locale

/**
 * Normalizes `@Destination` route declarations into the compile-time route key.
 * 将 `@Destination` 中声明的路由规范化为编译期路由键。
 *
 * Declarations intentionally reject query and fragment because runtime calls can provide those
 * values per navigation request through `Intent.data`.
 * 声明阶段有意拒绝 query 和 fragment，因为运行时每次导航可通过 `Intent.data` 传入这些值。
 */
internal object DeclaredRouteNormalizer {

    fun normalize(value: String): String? = runCatching {
        if (value.isBlank() || value != value.trim()) return null
        val uri = URI(value)
        if (!uri.isAbsolute || uri.scheme.isNullOrBlank() || uri.host.isNullOrBlank()) return null
        if (uri.rawQuery != null || uri.rawFragment != null) return null
        val path = uri.normalize().rawPath?.takeIf(String::isNotEmpty) ?: "/"
        "${uri.scheme.lowercase(Locale.ROOT)}://${uri.host.lowercase(Locale.ROOT)}$path"
    }.getOrNull()
}
