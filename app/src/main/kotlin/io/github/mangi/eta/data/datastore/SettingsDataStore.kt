package io.github.mangi.eta.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.mangi.eta.data.model.AppearanceAccentColor
import io.github.mangi.eta.data.model.AppearancePaletteStyle
import io.github.mangi.eta.data.model.AppearanceSettings
import io.github.mangi.eta.data.model.AppearanceThemeMode
import io.github.mangi.eta.data.model.AppearanceTopBarBlurStyle
import io.github.mangi.eta.data.model.DEFAULT_WALLPAPER_BLUR
import io.github.mangi.eta.data.model.DEFAULT_WALLPAPER_DIM
import io.github.mangi.eta.data.model.SpeechSettings
import io.github.mangi.eta.data.model.ThemeProfile
import io.github.mangi.eta.data.model.WallpaperScaleMode
import io.github.mangi.eta.data.model.WallpaperSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import io.github.mangi.eta.data.model.DEFAULT_THEME_PROFILE_ID
import io.github.mangi.eta.data.model.Settings
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal object SettingsDataStore {
    private val SPEECH_SETTINGS = stringPreferencesKey("speech_settings_v1")
    private val speechJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun speechSettingsFlow(): Flow<SpeechSettings> {
        ensureInitialized()
        return dataStore.data.map { prefs ->
            prefs[SPEECH_SETTINGS]?.let { speechJson.decodeFromString<SpeechSettings>(it) }
                ?: SpeechSettings()
        }
    }

    suspend fun speechSettings() = speechSettingsFlow().first()

    suspend fun setSpeechSettings(settings: SpeechSettings) {
        ensureInitialized()
        dataStore.edit { it[SPEECH_SETTINGS] = speechJson.encodeToString(settings) }
    }

    private const val STORE_NAME = "eta_settings"

    private val SELECTED_PROVIDER_ID = stringPreferencesKey("selected_provider_id")
    private val SELECTED_MODEL_ID = stringPreferencesKey("selected_model_id")
    private val OFFICIAL_MODEL_CATALOG_REVISION = intPreferencesKey("official_model_catalog_revision")
    private val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
    private val SHOW_BUILT_IN_PROVIDERS = booleanPreferencesKey("show_built_in_providers")
    private val LINUX_DISTRIBUTION = stringPreferencesKey("linux_distribution")
    private val APPEARANCE_THEME_MODE = stringPreferencesKey("appearance_theme_mode")
    private val APPEARANCE_MONET_ENABLED = booleanPreferencesKey("appearance_monet_enabled")
    private val APPEARANCE_PALETTE_STYLE = stringPreferencesKey("appearance_palette_style")
    private val APPEARANCE_ACCENT_COLOR = stringPreferencesKey("appearance_accent_color")
    private val APPEARANCE_PURE_BLACK_ENABLED = booleanPreferencesKey("appearance_pure_black_enabled")
    private val APPEARANCE_BLUR_ENABLED = booleanPreferencesKey("appearance_blur_enabled")
    private val APPEARANCE_TOP_BAR_BLUR_STYLE = stringPreferencesKey("appearance_top_bar_blur_style")
    private val APPEARANCE_SWIPE_DISMISS_ENABLED =
        booleanPreferencesKey("appearance_swipe_dismiss_enabled")
    private val APPEARANCE_PREDICTIVE_BACK_ENABLED =
        booleanPreferencesKey("appearance_predictive_back_enabled")
    private val APPEARANCE_INTERFACE_SCALE = floatPreferencesKey("appearance_interface_scale")
    private val APPEARANCE_BUBBLE_CORNER_SCALE = floatPreferencesKey("appearance_bubble_corner_scale")
    private val APPEARANCE_HOME_GREETING = stringPreferencesKey("appearance_home_greeting")
    private val WALLPAPER_ENABLED = booleanPreferencesKey("wallpaper_enabled")
    private val WALLPAPER_DIM = floatPreferencesKey("wallpaper_dim")
    private val WALLPAPER_BLUR = floatPreferencesKey("wallpaper_blur_radius")
    private val WALLPAPER_SCALE_MODE = stringPreferencesKey("wallpaper_scale_mode")
    private const val SELECTED_MODEL_BY_PROVIDER_PREFIX = "selected_model_id_by_provider."

    // 多主题档案：以 JSON 序列化存储，避免为每个字段单独声明 DataStore key。
    private val THEME_PROFILES = stringPreferencesKey("theme_profiles_v1")
    private val ACTIVE_THEME_PROFILE_ID = stringPreferencesKey("active_theme_profile_id")
    private val themeJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = STORE_NAME)

    @Volatile
    private lateinit var dataStore: DataStore<Preferences>

    fun init(context: Context) {
        if (!::dataStore.isInitialized) {
            dataStore = context.applicationContext.dataStore
        }
    }

    fun settingsFlow(): Flow<Settings> {
        ensureInitialized()
        return dataStore.data
            .catch { cause ->
                if (cause is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw cause
                }
            }
            .map { preferences -> preferences.toSettings() }
    }

    suspend fun settings(): Settings = settingsFlow().first()

    suspend fun officialModelCatalogRevision(): Int {
        ensureInitialized()
        return dataStore.data.first()[OFFICIAL_MODEL_CATALOG_REVISION] ?: 0
    }

    suspend fun setOfficialModelCatalogRevision(revision: Int) {
        ensureInitialized()
        dataStore.edit { preferences ->
            preferences[OFFICIAL_MODEL_CATALOG_REVISION] = revision
        }
    }

    suspend fun updateSettings(transform: (Settings) -> Settings) {
        ensureInitialized()
        dataStore.edit { prefs ->
            val current = prefs.toSettings()
            val updated = transform(current)
            prefs.putOrRemove(SELECTED_PROVIDER_ID, updated.selectedProviderId)
            prefs.putOrRemove(SELECTED_MODEL_ID, updated.selectedModelId)
            prefs[MEMORY_ENABLED] = updated.memoryEnabled
            prefs[SHOW_BUILT_IN_PROVIDERS] = updated.showBuiltInProviders
            prefs.putAppearance(updated.appearance.normalized())
        }
    }

    fun selectedProviderIdFlow(): Flow<String?> =
        settingsFlow().map { it.selectedProviderId }

    fun selectedModelIdFlow(): Flow<String?> =
        settingsFlow().map { it.selectedModelId }

    suspend fun selectedModelIdForProvider(providerId: String): String? {
        ensureInitialized()
        return dataStore.data
            .catch { cause ->
                if (cause is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw cause
                }
            }
            .map { prefs -> prefs[selectedModelByProviderKey(providerId)] }
            .first()
    }

    fun memoryEnabledFlow(): Flow<Boolean> =
        settingsFlow().map { it.memoryEnabled }

    fun showBuiltInProvidersFlow(): Flow<Boolean> =
        settingsFlow().map { it.showBuiltInProviders }

    fun linuxDistributionFlow(): Flow<String?> {
        ensureInitialized()
        return dataStore.data
            .catch { cause ->
                if (cause is IOException) emit(emptyPreferences()) else throw cause
            }
            .map { preferences -> preferences[LINUX_DISTRIBUTION] }
    }

    fun linuxBackendFlow(distribution: String): Flow<String?> {
        ensureInitialized()
        return dataStore.data.catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }.map { it[stringPreferencesKey("linux_backend.$distribution")] }
    }

    suspend fun setLinuxBackend(distribution: String, backend: String?) {
        ensureInitialized()
        dataStore.edit { preferences ->
            val key = stringPreferencesKey("linux_backend.$distribution")
            if (backend == null) preferences.remove(key) else preferences[key] = backend
        }
    }

    fun appearanceSettingsFlow(): Flow<AppearanceSettings> =
        settingsFlow().map { it.appearance }

    suspend fun setSelectedProviderId(id: String?) {
        updateSettings { it.copy(selectedProviderId = id) }
    }

    suspend fun setSelectedModelId(id: String?) {
        updateSettings { it.copy(selectedModelId = id) }
    }

    suspend fun setSelection(providerId: String?, modelId: String?) {
        ensureInitialized()
        dataStore.edit { prefs ->
            val previousProviderId = prefs[SELECTED_PROVIDER_ID]
            val previousModelId = prefs[SELECTED_MODEL_ID]
            if (previousProviderId != null && previousModelId != null) {
                prefs[selectedModelByProviderKey(previousProviderId)] = previousModelId
            }

            prefs.putOrRemove(SELECTED_PROVIDER_ID, providerId)
            prefs.putOrRemove(SELECTED_MODEL_ID, modelId)
            if (providerId != null && modelId != null) {
                prefs[selectedModelByProviderKey(providerId)] = modelId
            }
        }
    }

    suspend fun clearSelectedModelIdForProvider(providerId: String) {
        ensureInitialized()
        dataStore.edit { prefs ->
            prefs.remove(selectedModelByProviderKey(providerId))
        }
    }

    suspend fun setMemoryEnabled(enabled: Boolean) {
        updateSettings { it.copy(memoryEnabled = enabled) }
    }

    suspend fun setShowBuiltInProviders(enabled: Boolean) {
        updateSettings { it.copy(showBuiltInProviders = enabled) }
    }

    suspend fun setLinuxDistribution(value: String?) {
        ensureInitialized()
        dataStore.edit { preferences -> preferences.putOrRemove(LINUX_DISTRIBUTION, value) }
    }

    suspend fun setAppearanceSettings(settings: AppearanceSettings) {
        updateSettings { it.copy(appearance = settings.normalized()) }
    }

    suspend fun updateAppearanceSettings(transform: (AppearanceSettings) -> AppearanceSettings) {
        updateSettings { settings ->
            settings.copy(appearance = transform(settings.appearance).normalized())
        }
    }

    private fun ensureInitialized() {
        check(::dataStore.isInitialized) {
            "SettingsDataStore.init(context) must be called in Application.onCreate()"
        }
    }

    private fun selectedModelByProviderKey(providerId: String): Preferences.Key<String> =
        stringPreferencesKey("$SELECTED_MODEL_BY_PROVIDER_PREFIX$providerId")

    private fun MutablePreferences.putOrRemove(key: Preferences.Key<String>, value: String?) {
        if (value.isNullOrBlank()) {
            remove(key)
        } else {
            this[key] = value
        }
    }

    private fun Preferences.toSettings(): Settings = Settings(
        selectedProviderId = this[SELECTED_PROVIDER_ID],
        selectedModelId = this[SELECTED_MODEL_ID],
        memoryEnabled = this[MEMORY_ENABLED] ?: true,
        showBuiltInProviders = this[SHOW_BUILT_IN_PROVIDERS] ?: true,
        appearance = AppearanceSettings(
            themeMode = AppearanceThemeMode.fromPersistedValue(this[APPEARANCE_THEME_MODE]),
            monetEnabled = this[APPEARANCE_MONET_ENABLED] ?: false,
            paletteStyle = AppearancePaletteStyle.fromPersistedValue(this[APPEARANCE_PALETTE_STYLE]),
            accentColor = AppearanceAccentColor.fromPersistedValue(this[APPEARANCE_ACCENT_COLOR]),
            pureBlackEnabled = this[APPEARANCE_PURE_BLACK_ENABLED] ?: false,
            blurEnabled = this[APPEARANCE_BLUR_ENABLED] ?: true,
            topBarBlurStyle = AppearanceTopBarBlurStyle.fromPersistedValue(
                this[APPEARANCE_TOP_BAR_BLUR_STYLE],
            ),
            swipeDismissEnabled = this[APPEARANCE_SWIPE_DISMISS_ENABLED] ?: true,
            predictiveBackEnabled = this[APPEARANCE_PREDICTIVE_BACK_ENABLED] ?: true,
            interfaceScale = this[APPEARANCE_INTERFACE_SCALE] ?: 1f,
            bubbleCornerScale = this[APPEARANCE_BUBBLE_CORNER_SCALE] ?: 1f,
            homeGreeting = this[APPEARANCE_HOME_GREETING] ?: "",
            wallpaper = WallpaperSettings(
                enabled = this[WALLPAPER_ENABLED] ?: false,
                dim = this[WALLPAPER_DIM] ?: DEFAULT_WALLPAPER_DIM,
                blurRadius = this[WALLPAPER_BLUR] ?: DEFAULT_WALLPAPER_BLUR,
                scaleMode = WallpaperScaleMode.fromPersistedValue(this[WALLPAPER_SCALE_MODE]),
            ),
        ).normalized(),
    )

    private fun MutablePreferences.putAppearance(settings: AppearanceSettings) {
        this[APPEARANCE_THEME_MODE] = settings.themeMode.persistedValue
        this[APPEARANCE_MONET_ENABLED] = settings.monetEnabled
        this[APPEARANCE_PALETTE_STYLE] = settings.paletteStyle.persistedValue
        this[APPEARANCE_ACCENT_COLOR] = settings.accentColor.persistedValue
        this[APPEARANCE_PURE_BLACK_ENABLED] = settings.pureBlackEnabled
        this[APPEARANCE_BLUR_ENABLED] = settings.blurEnabled
        this[APPEARANCE_TOP_BAR_BLUR_STYLE] = settings.topBarBlurStyle.persistedValue
        this[APPEARANCE_SWIPE_DISMISS_ENABLED] = settings.swipeDismissEnabled
        this[APPEARANCE_PREDICTIVE_BACK_ENABLED] = settings.predictiveBackEnabled
        this[APPEARANCE_INTERFACE_SCALE] = settings.interfaceScale
        this[APPEARANCE_BUBBLE_CORNER_SCALE] = settings.bubbleCornerScale
        this[APPEARANCE_HOME_GREETING] = settings.homeGreeting
        this[WALLPAPER_ENABLED] = settings.wallpaper.enabled
        this[WALLPAPER_DIM] = settings.wallpaper.dim
        this[WALLPAPER_BLUR] = settings.wallpaper.blurRadius
        this[WALLPAPER_SCALE_MODE] = settings.wallpaper.scaleMode.persistedValue
    }

    // ────────────────────────────────────────────────────────────────
    // 多主题档案持久化
    // ────────────────────────────────────────────────────────────────

    private fun defaultThemeProfiles(): List<ThemeProfile> = listOf(ThemeProfile())

    private fun Preferences.themeProfilesRaw(): List<ThemeProfile> {
        val raw = this[THEME_PROFILES] ?: return defaultThemeProfiles()
        return try {
            val element = themeJson.parseToJsonElement(raw)
            themeJson.decodeFromJsonElement<List<ThemeProfile>>(element)
        } catch (_: Exception) {
            defaultThemeProfiles()
        }
    }

    private fun MutablePreferences.putThemeProfiles(profiles: List<ThemeProfile>) {
        val normalized = profiles.map { it.normalized() }
        this[THEME_PROFILES] = themeJson.encodeToString(themeJson.encodeToJsonElement(normalized))
    }

    fun themeProfilesFlow(): Flow<List<ThemeProfile>> {
        ensureInitialized()
        return dataStore.data
            .catch { cause ->
                if (cause is IOException) emit(emptyPreferences()) else throw cause
            }
            .map { prefs -> prefs.themeProfilesRaw() }
    }

    suspend fun themeProfiles(): List<ThemeProfile> = themeProfilesFlow().first()

    fun activeThemeProfileIdFlow(): Flow<String> {
        ensureInitialized()
        return dataStore.data
            .catch { cause ->
                if (cause is IOException) emit(emptyPreferences()) else throw cause
            }
            .map { prefs -> prefs[ACTIVE_THEME_PROFILE_ID] ?: DEFAULT_THEME_PROFILE_ID }
    }

    fun activeThemeProfileFlow(): Flow<ThemeProfile> {
        ensureInitialized()
        return dataStore.data
            .catch { cause ->
                if (cause is IOException) emit(emptyPreferences()) else throw cause
            }
            .map { prefs ->
                val activeId = prefs[ACTIVE_THEME_PROFILE_ID] ?: DEFAULT_THEME_PROFILE_ID
                val profiles = prefs.themeProfilesRaw()
                profiles.firstOrNull { it.id == activeId }
                    ?: profiles.firstOrNull()
                    ?: ThemeProfile()
            }
    }

    suspend fun activeThemeProfile(): ThemeProfile = activeThemeProfileFlow().first()

    suspend fun setActiveThemeProfileId(id: String) {
        ensureInitialized()
        dataStore.edit { prefs ->
            prefs[ACTIVE_THEME_PROFILE_ID] = id
        }
    }

    suspend fun saveThemeProfile(profile: ThemeProfile) {
        ensureInitialized()
        dataStore.edit { prefs ->
            val current = prefs.themeProfilesRaw().toMutableList()
            val normalized = profile.normalized()
            val index = current.indexOfFirst { it.id == normalized.id }
            if (index >= 0) {
                current[index] = normalized
            } else {
                current.add(normalized)
            }
            prefs.putThemeProfiles(current)
        }
    }

    suspend fun deleteThemeProfile(id: String) {
        ensureInitialized()
        dataStore.edit { prefs ->
            if (id == DEFAULT_THEME_PROFILE_ID) return@edit
            val current = prefs.themeProfilesRaw()
            val remaining = current.filterNot { it.id == id }
            if (remaining.isNotEmpty()) {
                prefs.putThemeProfiles(remaining)
                if (prefs[ACTIVE_THEME_PROFILE_ID] == id) {
                    prefs[ACTIVE_THEME_PROFILE_ID] = DEFAULT_THEME_PROFILE_ID
                }
            }
        }
    }
}
