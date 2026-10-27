package io.github.mangi.eta.data.repository

import io.github.mangi.eta.data.datastore.SettingsDataStore
import io.github.mangi.eta.data.model.DEFAULT_THEME_PROFILE_ID
import io.github.mangi.eta.data.model.ThemeProfile
import kotlinx.coroutines.flow.Flow

/**
 * 多主题档案仓库：管理主题的创建、编辑、删除、切换与持久化。
 *
 * 持久化委托给 [SettingsDataStore]，渲染层只依赖激活档案投影出的 [AppearanceSettings] 与
 * 气泡样式，从而与业务逻辑解耦。官方升级时业务代码零改动。
 */
object ThemeProfileRepository {
    fun profilesFlow(): Flow<List<ThemeProfile>> = SettingsDataStore.themeProfilesFlow()

    fun activeProfileIdFlow(): Flow<String> = SettingsDataStore.activeThemeProfileIdFlow()

    fun activeProfileFlow(): Flow<ThemeProfile> = SettingsDataStore.activeThemeProfileFlow()

    suspend fun profiles(): List<ThemeProfile> = SettingsDataStore.themeProfiles()

    suspend fun activeProfile(): ThemeProfile = SettingsDataStore.activeThemeProfile()

    suspend fun setActiveProfile(id: String) {
        SettingsDataStore.setActiveThemeProfileId(id)
    }

    suspend fun saveProfile(profile: ThemeProfile) {
        SettingsDataStore.saveThemeProfile(profile.normalized())
    }

    suspend fun deleteProfile(id: String) {
        SettingsDataStore.deleteThemeProfile(id)
    }

    suspend fun createProfile(name: String, basedOn: ThemeProfile): ThemeProfile {
        val id = "profile_${System.currentTimeMillis()}"
        val profile = basedOn.copy(id = id, name = name).normalized()
        SettingsDataStore.saveThemeProfile(profile)
        return profile
    }
}

internal const val THEME_PROFILE_ID_DEFAULT = DEFAULT_THEME_PROFILE_ID
