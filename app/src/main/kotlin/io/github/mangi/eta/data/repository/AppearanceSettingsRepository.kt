package io.github.mangi.eta.data.repository

import io.github.mangi.eta.data.datastore.SettingsDataStore
import io.github.mangi.eta.data.model.AppearanceSettings
import io.github.mangi.eta.data.model.ThemeProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * 外观设置的唯一读写入口。
 *
 * 自多主题档案引入后，这里桥接到「激活主题档案」：
 * - 读：激活档案投影为 [AppearanceSettings]，并叠加两项全局交互设置；
 * - 写：视觉字段回写档案，交互字段写全局键。
 *
 * 这样旧设置页（AppearanceSettingsScreen）无需改动，切换档案即时生效，
 * 且外观配置与业务状态彻底解耦。
 */
object AppearanceSettingsRepository {
    fun settingsFlow(): Flow<AppearanceSettings> =
        combine(
            SettingsDataStore.activeThemeProfileFlow(),
            SettingsDataStore.swipeDismissEnabledFlow(),
            SettingsDataStore.predictiveBackEnabledFlow(),
        ) { profile, swipeDismiss, predictiveBack ->
            profile.toAppearanceSettings().copy(
                swipeDismissEnabled = swipeDismiss,
                predictiveBackEnabled = predictiveBack,
            )
        }

    suspend fun settings(): AppearanceSettings {
        val profile = SettingsDataStore.activeThemeProfile()
        val swipeDismiss = SettingsDataStore.swipeDismissEnabledFlow().first()
        val predictiveBack = SettingsDataStore.predictiveBackEnabledFlow().first()
        return profile.toAppearanceSettings().copy(
            swipeDismissEnabled = swipeDismiss,
            predictiveBackEnabled = predictiveBack,
        )
    }

    suspend fun update(settings: AppearanceSettings) {
        val active = SettingsDataStore.activeThemeProfile()
        SettingsDataStore.saveThemeProfile(active.applyAppearance(settings))
        persistInteractionSettings(active.toAppearanceSettings(), settings)
    }

    suspend fun update(transform: (AppearanceSettings) -> AppearanceSettings) {
        val active = SettingsDataStore.activeThemeProfile()
        val current = active.toAppearanceSettings().copy(
            swipeDismissEnabled = SettingsDataStore.swipeDismissEnabledFlow().first(),
            predictiveBackEnabled = SettingsDataStore.predictiveBackEnabledFlow().first(),
        )
        val updated = transform(current)
        SettingsDataStore.saveThemeProfile(active.applyAppearance(updated))
        persistInteractionSettings(current, updated)
    }

    /** 交互设置写全局键，不进入主题档案。 */
    private suspend fun persistInteractionSettings(
        before: AppearanceSettings,
        after: AppearanceSettings,
    ) {
        if (before.swipeDismissEnabled != after.swipeDismissEnabled) {
            SettingsDataStore.setSwipeDismissEnabled(after.swipeDismissEnabled)
        }
        if (before.predictiveBackEnabled != after.predictiveBackEnabled) {
            SettingsDataStore.setPredictiveBackEnabled(after.predictiveBackEnabled)
        }
    }

    /**
     * 把 AppearanceSettings 中「档案管理的字段」回写到档案。
     *
     * 视觉项 blurEnabled / topBarBlurStyle 纳入档案（主题可各有不同模糊表现）；
     * 交互项 swipeDismissEnabled / predictiveBackEnabled 刻意不纳入，
     * 避免切换主题时改变用户的手势习惯。
     */
    private fun ThemeProfile.applyAppearance(appearance: AppearanceSettings): ThemeProfile = copy(
        themeMode = appearance.themeMode,
        monetEnabled = appearance.monetEnabled,
        paletteStyle = appearance.paletteStyle,
        accentColor = appearance.accentColor,
        pureBlackEnabled = appearance.pureBlackEnabled,
        blurEnabled = appearance.blurEnabled,
        topBarBlurStyle = appearance.topBarBlurStyle,
        interfaceScale = appearance.interfaceScale,
        bubbleCornerScale = appearance.bubbleCornerScale,
        homeGreeting = appearance.homeGreeting,
        wallpaper = appearance.wallpaper,
    )
}

/**
 * 把激活档案投影为渲染层的 [AppearanceSettings]。
 *
 * swipeDismissEnabled / predictiveBackEnabled 属全局交互设置，不从档案读取，
 * 这里保留 [AppearanceSettings] 的默认值，由设置页以全局语义单独维护。
 */
internal fun ThemeProfile.toAppearanceSettings(): AppearanceSettings = AppearanceSettings(
    themeMode = themeMode,
    monetEnabled = monetEnabled,
    paletteStyle = paletteStyle,
    accentColor = accentColor,
    pureBlackEnabled = pureBlackEnabled,
    blurEnabled = blurEnabled,
    topBarBlurStyle = topBarBlurStyle,
    interfaceScale = interfaceScale,
    bubbleCornerScale = bubbleCornerScale,
    homeGreeting = homeGreeting,
    wallpaper = wallpaper,
)
