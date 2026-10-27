package io.github.mangi.eta.data.repository

import io.github.mangi.eta.data.datastore.SettingsDataStore
import io.github.mangi.eta.data.model.AppearanceSettings
import io.github.mangi.eta.data.model.ThemeProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 外观设置的唯一读写入口。
 *
 * 自多主题档案引入后，这里桥接到「激活主题档案」：
 * - 读：激活档案投影为 [AppearanceSettings]；
 * - 写：把 [AppearanceSettings] 的变化回写到激活档案的对应字段。
 *
 * 这样旧设置页（AppearanceSettingsScreen）无需改动，切换档案即时生效，
 * 且外观配置与业务状态彻底解耦。
 */
object AppearanceSettingsRepository {
    fun settingsFlow(): Flow<AppearanceSettings> =
        SettingsDataStore.activeThemeProfileFlow().map { it.toAppearanceSettings() }

    suspend fun settings(): AppearanceSettings =
        SettingsDataStore.activeThemeProfile().toAppearanceSettings()

    suspend fun update(settings: AppearanceSettings) {
        val active = SettingsDataStore.activeThemeProfile()
        SettingsDataStore.saveThemeProfile(active.applyAppearance(settings))
    }

    suspend fun update(transform: (AppearanceSettings) -> AppearanceSettings) {
        val active = SettingsDataStore.activeThemeProfile()
        val current = active.toAppearanceSettings()
        SettingsDataStore.saveThemeProfile(active.applyAppearance(transform(current)))
    }

    /** 把 AppearanceSettings 中「档案管理的字段」回写到档案，气泡样式保持档案原值。 */
    private fun ThemeProfile.applyAppearance(appearance: AppearanceSettings): ThemeProfile = copy(
        themeMode = appearance.themeMode,
        monetEnabled = appearance.monetEnabled,
        paletteStyle = appearance.paletteStyle,
        accentColor = appearance.accentColor,
        pureBlackEnabled = appearance.pureBlackEnabled,
        interfaceScale = appearance.interfaceScale,
        bubbleCornerScale = appearance.bubbleCornerScale,
        homeGreeting = appearance.homeGreeting,
        wallpaper = appearance.wallpaper,
    )
}

internal fun ThemeProfile.toAppearanceSettings(): AppearanceSettings = AppearanceSettings(
    themeMode = themeMode,
    monetEnabled = monetEnabled,
    paletteStyle = paletteStyle,
    accentColor = accentColor,
    pureBlackEnabled = pureBlackEnabled,
    interfaceScale = interfaceScale,
    bubbleCornerScale = bubbleCornerScale,
    homeGreeting = homeGreeting,
    wallpaper = wallpaper,
)
