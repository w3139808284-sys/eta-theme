package io.github.mangi.eta.data.repository

import io.github.mangi.eta.data.model.ThemeProfile
import kotlinx.serialization.json.Json

/**
 * 主题档案的 JSON 编解码：用于导入/导出。
 *
 * 导出格式为顶层 `{ "version": 1, "profiles": [...] }`，带版本号以便未来升级迁移。
 */
object ThemeProfileCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private const val FORMAT_VERSION = 1

    @kotlinx.serialization.Serializable
    private data class ThemeBundle(
        val version: Int = FORMAT_VERSION,
        val profiles: List<ThemeProfile> = emptyList(),
    )

    fun encode(profiles: List<ThemeProfile>): String =
        json.encodeToString(ThemeBundle(profiles = profiles.map { it.normalized() }))

    fun decode(raw: String): List<ThemeProfile>? = runCatching {
        json.decodeFromString<ThemeBundle>(raw).profiles.map { it.normalized() }
    }.getOrNull()

    fun defaultExportFileName(): String = "eta-themes.json"
}
