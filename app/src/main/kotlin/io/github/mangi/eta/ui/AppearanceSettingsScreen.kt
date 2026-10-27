package io.github.mangi.eta.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.R
import io.github.mangi.eta.data.model.AppearanceAccentColor
import io.github.mangi.eta.data.model.AppearancePaletteStyle
import io.github.mangi.eta.data.model.AppearanceSettings
import io.github.mangi.eta.data.model.AppearanceThemeMode
import io.github.mangi.eta.data.model.AppearanceTopBarBlurStyle
import io.github.mangi.eta.data.model.AvatarSettings
import io.github.mangi.eta.data.model.AvatarShape
import io.github.mangi.eta.data.model.BubbleMaterial
import io.github.mangi.eta.data.model.DEFAULT_THEME_PROFILE_ID
import io.github.mangi.eta.data.model.DEFAULT_THEME_PROFILE_NAME
import io.github.mangi.eta.data.model.MAX_AVATAR_CORNER
import io.github.mangi.eta.data.model.MAX_AVATAR_SIZE
import io.github.mangi.eta.data.model.MAX_BUBBLE_CORNER_SCALE
import io.github.mangi.eta.data.model.MAX_BUBBLE_CORNER_RADIUS
import io.github.mangi.eta.data.model.MAX_HOME_GREETING_LENGTH
import io.github.mangi.eta.data.model.MAX_INTERFACE_SCALE
import io.github.mangi.eta.data.model.MAX_PADDING
import io.github.mangi.eta.data.model.MAX_WALLPAPER_BLUR
import io.github.mangi.eta.data.model.MAX_WALLPAPER_DIM
import io.github.mangi.eta.data.model.MIN_AVATAR_CORNER
import io.github.mangi.eta.data.model.MIN_AVATAR_SIZE
import io.github.mangi.eta.data.model.MIN_BUBBLE_CORNER_RADIUS
import io.github.mangi.eta.data.model.MIN_BUBBLE_CORNER_SCALE
import io.github.mangi.eta.data.model.MIN_INTERFACE_SCALE
import io.github.mangi.eta.data.model.MIN_PADDING
import io.github.mangi.eta.data.model.MIN_WALLPAPER_BLUR
import io.github.mangi.eta.data.model.MIN_WALLPAPER_DIM
import io.github.mangi.eta.data.model.WallpaperScaleMode
import io.github.mangi.eta.data.model.normalizeAvatarCorner
import io.github.mangi.eta.data.model.normalizeAvatarSize
import io.github.mangi.eta.data.model.normalizeBubbleCornerScale
import io.github.mangi.eta.data.model.normalizeHomeGreeting
import io.github.mangi.eta.data.model.normalizeInterfaceScale
import io.github.mangi.eta.data.model.ThemeProfile
import io.github.mangi.eta.data.repository.AppearanceSettingsRepository
import io.github.mangi.eta.data.repository.ThemeProfileCodec
import io.github.mangi.eta.data.repository.ThemeProfileRepository
import io.github.mangi.eta.ui.app.AvatarStore
import io.github.mangi.eta.ui.app.LocalAppearanceSettings
import io.github.mangi.eta.ui.app.WallpaperStore
import io.github.mangi.eta.ui.components.EtaArrowPreference
import io.github.mangi.eta.ui.components.EtaCard
import io.github.mangi.eta.ui.components.EtaOverlayDropdownPreference
import io.github.mangi.eta.ui.components.EtaPreferenceDivider
import io.github.mangi.eta.ui.components.EtaPreferenceGroup
import io.github.mangi.eta.ui.components.EtaPreferenceGroupTitle
import io.github.mangi.eta.ui.components.EtaSwitchPreference
import io.github.mangi.eta.ui.components.EtaWindowDialog
import io.github.mangi.eta.ui.components.MiuixDialogActions
import io.github.mangi.eta.ui.components.MiuixScaffoldPage
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AppearanceSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val appearance = LocalAppearanceSettings.current
    val coroutineScope = rememberCoroutineScope()
    var scaleDraft by remember(appearance.interfaceScale) {
        mutableFloatStateOf(appearance.interfaceScale * 100f)
    }
    var showScaleDialog by remember { mutableStateOf(false) }
    var scaleInput by remember { mutableStateOf("") }
    var showGreetingDialog by remember { mutableStateOf(false) }
    var greetingInput by remember { mutableStateOf("") }
    var showWallpaperError by remember { mutableStateOf(false) }
    val blurSupported = isRuntimeShaderSupported()

    // 主题档案状态
    val profiles by ThemeProfileRepository.profilesFlow().collectAsState(initial = emptyList())
    val activeProfileId by ThemeProfileRepository.activeProfileIdFlow()
        .collectAsState(initial = DEFAULT_THEME_PROFILE_ID)
    val activeProfile = profiles.firstOrNull { it.id == activeProfileId } ?: ThemeProfile()
    var showNewProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var showBubbleDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAvatarError by remember { mutableStateOf(false) }

    fun update(transform: (AppearanceSettings) -> AppearanceSettings) {
        coroutineScope.launch {
            AppearanceSettingsRepository.update(transform)
        }
    }

    fun saveProfile(profile: ThemeProfile) {
        coroutineScope.launch {
            ThemeProfileRepository.saveProfile(profile)
        }
    }

    fun updateAvatars(transform: (AvatarSettings) -> AvatarSettings) {
        saveProfile(activeProfile.copy(avatars = transform(activeProfile.avatars).normalized()))
    }

    fun switchProfile(id: String) {
        coroutineScope.launch {
            ThemeProfileRepository.setActiveProfile(id)
        }
    }

    fun createProfile() {
        val name = newProfileName.trim().ifBlank { DEFAULT_THEME_PROFILE_NAME }
        coroutineScope.launch {
            val created = ThemeProfileRepository.createProfile(name, activeProfile)
            ThemeProfileRepository.setActiveProfile(created.id)
        }
        newProfileName = ""
        showNewProfileDialog = false
    }

    fun duplicateActiveProfile() {
        val name = activeProfile.name + " · " + context.getString(R.string.appearance_profile_copy_suffix)
        coroutineScope.launch {
            val created = ThemeProfileRepository.createProfile(name, activeProfile)
            ThemeProfileRepository.setActiveProfile(created.id)
        }
    }

    fun deleteActiveProfile() {
        val id = activeProfile.id
        coroutineScope.launch {
            ThemeProfileRepository.deleteProfile(id)
            ThemeProfileRepository.setActiveProfile(DEFAULT_THEME_PROFILE_ID)
        }
        showDeleteDialog = false
    }

    fun commitScale(percent: Float) {
        val scale = normalizeInterfaceScale(percent.roundToInt() / 100f)
        scaleDraft = scale * 100f
        update { current -> current.copy(interfaceScale = scale) }
    }

    val wallpaperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val imported = WallpaperStore.import(context, uri)
            if (imported) {
                update { current ->
                    current.copy(wallpaper = current.wallpaper.copy(enabled = true))
                }
            } else {
                showWallpaperError = true
            }
        }
    }

    // 头像图片选择：用户 / AI 各一个。
    val userAvatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val path = AvatarStore.import(context, AvatarStore.Target.USER, uri)
            if (path != null) {
                updateAvatars { it.copy(userImagePath = path, enabled = true) }
            } else {
                showAvatarError = true
            }
        }
    }

    val aiAvatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val path = AvatarStore.import(context, AvatarStore.Target.AI, uri)
            if (path != null) {
                updateAvatars { it.copy(aiImagePath = path, enabled = true) }
            } else {
                showAvatarError = true
            }
        }
    }

    // 主题档案导入/导出
    val themeExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            runCatching {
                val text = ThemeProfileCodec.encode(profiles)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(text.toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    val themeImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                } ?: return@launch
                val imported = ThemeProfileCodec.decode(text) ?: return@launch
                imported.forEach { ThemeProfileRepository.saveProfile(it) }
                if (imported.isNotEmpty()) {
                    ThemeProfileRepository.setActiveProfile(imported.first().id)
                }
            }
        }
    }

    val themeModes = AppearanceThemeMode.entries
    val themeModeLabels = listOf(
        stringResource(R.string.appearance_theme_system),
        stringResource(R.string.appearance_theme_light),
        stringResource(R.string.appearance_theme_dark),
    )
    val paletteStyles = AppearancePaletteStyle.entries
    val paletteLabels = listOf(
        stringResource(R.string.appearance_palette_tonal_spot),
        stringResource(R.string.appearance_palette_neutral),
        stringResource(R.string.appearance_palette_vibrant),
        stringResource(R.string.appearance_palette_expressive),
        stringResource(R.string.appearance_palette_rainbow),
        stringResource(R.string.appearance_palette_fruit_salad),
        stringResource(R.string.appearance_palette_monochrome),
        stringResource(R.string.appearance_palette_fidelity),
        stringResource(R.string.appearance_palette_content),
    )
    val accentColors = AppearanceAccentColor.entries
    val accentLabels = listOf(
        stringResource(R.string.appearance_accent_system),
        stringResource(R.string.appearance_accent_blue),
        stringResource(R.string.appearance_accent_purple),
        stringResource(R.string.appearance_accent_pink),
        stringResource(R.string.appearance_accent_red),
        stringResource(R.string.appearance_accent_orange),
        stringResource(R.string.appearance_accent_yellow),
        stringResource(R.string.appearance_accent_green),
        stringResource(R.string.appearance_accent_teal),
    )
    val blurStyles = AppearanceTopBarBlurStyle.entries
    val blurStyleLabels = listOf(
        stringResource(R.string.appearance_blur_style_gaussian),
        stringResource(R.string.appearance_blur_style_progressive),
    )
    val blurStyleSummaries = listOf(
        stringResource(R.string.appearance_blur_style_gaussian_summary),
        stringResource(R.string.appearance_blur_style_progressive_summary),
    )
    val wallpaperScaleModes = WallpaperScaleMode.entries
    val wallpaperScaleLabels = listOf(
        stringResource(R.string.appearance_wallpaper_scale_crop),
        stringResource(R.string.appearance_wallpaper_scale_fit),
    )

    MiuixScaffoldPage(
        title = stringResource(R.string.appearance_title),
        onBack = onBack,
    ) {
        item(key = "appearance_profile_title") {
            EtaPreferenceGroupTitle(text = stringResource(R.string.appearance_group_profile))
        }
        item(key = "appearance_profile_card") {
            EtaPreferenceGroup(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                // 当前档案名 + 档案列表切换
                val profileNames = profiles.map { it.name }
                EtaOverlayDropdownPreference(
                    title = stringResource(R.string.appearance_profile_active),
                    summary = activeProfile.name,
                    items = profileNames.ifEmpty { listOf(stringResource(R.string.appearance_profile_default_name)) },
                    selectedIndex = profiles.indexOfFirst { it.id == activeProfileId }
                        .coerceAtLeast(0),
                    onSelectedIndexChange = { index ->
                        profiles.getOrNull(index)?.let { switchProfile(it.id) }
                    },
                )
                EtaPreferenceDivider(hasLeading = false)
                EtaArrowPreference(
                    title = stringResource(R.string.appearance_profile_new),
                    summary = stringResource(R.string.appearance_profile_new_summary),
                    onClick = { showNewProfileDialog = true },
                    holdDownState = showNewProfileDialog,
                )
                EtaPreferenceDivider(hasLeading = false)
                EtaArrowPreference(
                    title = stringResource(R.string.appearance_profile_duplicate),
                    summary = stringResource(R.string.appearance_profile_duplicate_summary),
                    onClick = { duplicateActiveProfile() },
                )
                EtaPreferenceDivider(hasLeading = false)
                EtaArrowPreference(
                    title = stringResource(R.string.appearance_profile_export),
                    summary = stringResource(R.string.appearance_profile_export_summary),
                    onClick = { themeExportLauncher.launch(ThemeProfileCodec.defaultExportFileName()) },
                )
                EtaPreferenceDivider(hasLeading = false)
                EtaArrowPreference(
                    title = stringResource(R.string.appearance_profile_import),
                    summary = stringResource(R.string.appearance_profile_import_summary),
                    onClick = { themeImportLauncher.launch(arrayOf("application/json")) },
                )
                if (activeProfile.id != DEFAULT_THEME_PROFILE_ID) {
                    EtaPreferenceDivider(hasLeading = false)
                    EtaArrowPreference(
                        title = stringResource(R.string.appearance_profile_delete),
                        summary = stringResource(R.string.appearance_profile_delete_summary),
                        onClick = { showDeleteDialog = true },
                        holdDownState = showDeleteDialog,
                    )
                }
            }
        }

        item(key = "appearance_bubble_title") {
            EtaPreferenceGroupTitle(text = stringResource(R.string.appearance_group_bubble))
        }
        item(key = "appearance_bubble_card") {
            EtaPreferenceGroup(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                EtaArrowPreference(
                    title = stringResource(R.string.appearance_bubble_edit),
                    summary = stringResource(R.string.appearance_bubble_edit_summary),
                    onClick = { showBubbleDialog = true },
                    holdDownState = showBubbleDialog,
                )
            }
        }

        item(key = "appearance_avatar_title") {
            EtaPreferenceGroupTitle(text = stringResource(R.string.appearance_group_avatar))
        }
        item(key = "appearance_avatar_card") {
            val avatars = activeProfile.avatars
            EtaPreferenceGroup(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                EtaSwitchPreference(
                    title = stringResource(R.string.appearance_avatar_enabled),
                    summary = stringResource(R.string.appearance_avatar_enabled_summary),
                    checked = avatars.enabled,
                    onCheckedChange = { enabled ->
                        updateAvatars { it.copy(enabled = enabled) }
                    },
                )
                EtaPreferenceDivider(hasLeading = false)
                // 用户头像
                EtaArrowPreference(
                    title = if (avatars.userImagePath != null) {
                        stringResource(R.string.appearance_avatar_replace)
                    } else {
                        stringResource(R.string.appearance_avatar_pick)
                    },
                    summary = stringResource(R.string.appearance_avatar_user),
                    onClick = { userAvatarPicker.launch(arrayOf("image/*")) },
                )
                if (avatars.userImagePath != null) {
                    EtaPreferenceDivider(hasLeading = false)
                    EtaArrowPreference(
                        title = stringResource(R.string.appearance_avatar_remove),
                        summary = stringResource(R.string.appearance_avatar_user),
                        onClick = {
                            AvatarStore.clear(context, AvatarStore.Target.USER)
                            updateAvatars { it.copy(userImagePath = null) }
                        },
                    )
                }
                EtaPreferenceDivider(hasLeading = false)
                // AI 头像
                EtaArrowPreference(
                    title = if (avatars.aiImagePath != null) {
                        stringResource(R.string.appearance_avatar_replace)
                    } else {
                        stringResource(R.string.appearance_avatar_pick)
                    },
                    summary = stringResource(R.string.appearance_avatar_ai),
                    onClick = { aiAvatarPicker.launch(arrayOf("image/*")) },
                )
                if (avatars.aiImagePath != null) {
                    EtaPreferenceDivider(hasLeading = false)
                    EtaArrowPreference(
                        title = stringResource(R.string.appearance_avatar_remove),
                        summary = stringResource(R.string.appearance_avatar_ai),
                        onClick = {
                            AvatarStore.clear(context, AvatarStore.Target.AI)
                            updateAvatars { it.copy(aiImagePath = null) }
                        },
                    )
                }
                EtaPreferenceDivider(hasLeading = false)
                // 形状
                val shapeLabels = listOf(
                    stringResource(R.string.appearance_avatar_shape_circle),
                    stringResource(R.string.appearance_avatar_shape_rounded),
                )
                EtaOverlayDropdownPreference(
                    title = stringResource(R.string.appearance_avatar_shape),
                    summary = shapeLabels[avatars.shape.ordinal],
                    items = shapeLabels,
                    selectedIndex = avatars.shape.ordinal,
                    onSelectedIndexChange = { index ->
                        AvatarShape.entries.getOrNull(index)?.let { shape ->
                            updateAvatars { it.copy(shape = shape) }
                        }
                    },
                )
                // 圆角（仅圆角方形时显示）
                if (avatars.shape == AvatarShape.ROUNDED) {
                    EtaPreferenceDivider(hasLeading = false)
                    EtaArrowPreference(
                        title = stringResource(R.string.appearance_avatar_corner),
                        endActions = {
                            Text(
                                text = "${avatars.cornerRadiusDp.roundToInt()}dp",
                                fontSize = MiuixTheme.textStyles.body2.fontSize,
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            )
                        },
                        bottomAction = {
                            Slider(
                                value = avatars.cornerRadiusDp.coerceIn(
                                    MIN_AVATAR_CORNER,
                                    MAX_AVATAR_CORNER,
                                ),
                                onValueChange = { value ->
                                    updateAvatars { it.copy(cornerRadiusDp = normalizeAvatarCorner(value)) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                valueRange = MIN_AVATAR_CORNER..MAX_AVATAR_CORNER,
                                showKeyPoints = true,
                                keyPoints = listOf(0f, 4f, 8f, 16f),
                                magnetThreshold = 0.4f,
                                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                            )
                        },
                    )
                }
                EtaPreferenceDivider(hasLeading = false)
                // 尺寸
                EtaArrowPreference(
                    title = stringResource(R.string.appearance_avatar_size),
                    endActions = {
                        Text(
                            text = "${avatars.sizeDp.roundToInt()}dp",
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    },
                    bottomAction = {
                        Slider(
                            value = avatars.sizeDp.coerceIn(MIN_AVATAR_SIZE, MAX_AVATAR_SIZE),
                            onValueChange = { value ->
                                updateAvatars { it.copy(sizeDp = normalizeAvatarSize(value)) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            valueRange = MIN_AVATAR_SIZE..MAX_AVATAR_SIZE,
                            showKeyPoints = true,
                            keyPoints = listOf(24f, 32f, 40f, 48f),
                            magnetThreshold = 0.4f,
                            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                        )
                    },
                )
            }
        }

        item(key = "appearance_color_title") {
            EtaPreferenceGroupTitle(text = stringResource(R.string.appearance_group_color))
        }
        item(key = "appearance_color_card") {
            EtaPreferenceGroup(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                EtaOverlayDropdownPreference(
                    title = stringResource(R.string.appearance_theme_mode),
                    summary = themeModeLabels[appearance.themeMode.ordinal],
                    items = themeModeLabels,
                    selectedIndex = appearance.themeMode.ordinal,
                    onSelectedIndexChange = { index ->
                        themeModes.getOrNull(index)?.let { mode ->
                            update { current -> current.copy(themeMode = mode) }
                        }
                    },
                )
                EtaPreferenceDivider(hasLeading = false)
                EtaSwitchPreference(
                    title = stringResource(R.string.appearance_monet),
                    summary = stringResource(R.string.appearance_monet_summary),
                    checked = appearance.monetEnabled,
                    onCheckedChange = { enabled ->
                        update { current -> current.copy(monetEnabled = enabled) }
                    },
                )
                AnimatedVisibility(
                    visible = appearance.monetEnabled,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                ) {
                    Column {
                        EtaPreferenceDivider(hasLeading = false)
                        EtaOverlayDropdownPreference(
                            title = stringResource(R.string.appearance_palette_style),
                            summary = paletteLabels[appearance.paletteStyle.ordinal],
                            items = paletteLabels,
                            selectedIndex = appearance.paletteStyle.ordinal,
                            onSelectedIndexChange = { index ->
                                paletteStyles.getOrNull(index)?.let { style ->
                                    update { current -> current.copy(paletteStyle = style) }
                                }
                            },
                        )
                        EtaPreferenceDivider(hasLeading = false)
                        EtaOverlayDropdownPreference(
                            title = stringResource(R.string.appearance_accent_color),
                            summary = accentLabels[appearance.accentColor.ordinal],
                            items = accentLabels,
                            selectedIndex = appearance.accentColor.ordinal,
                            onSelectedIndexChange = { index ->
                                accentColors.getOrNull(index)?.let { accent ->
                                    update { current -> current.copy(accentColor = accent) }
                                }
                            },
                        )
                        EtaPreferenceDivider(hasLeading = false)
                        EtaSwitchPreference(
                            title = stringResource(R.string.appearance_pure_black),
                            summary = stringResource(R.string.appearance_pure_black_summary),
                            checked = appearance.pureBlackEnabled,
                            onCheckedChange = { enabled ->
                                update { current -> current.copy(pureBlackEnabled = enabled) }
                            },
                        )
                    }
                }
            }
        }

        item(key = "appearance_wallpaper_title") {
            EtaPreferenceGroupTitle(text = stringResource(R.string.appearance_group_wallpaper))
        }
        item(key = "appearance_wallpaper_card") {
            EtaPreferenceGroup(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                EtaSwitchPreference(
                    title = stringResource(R.string.appearance_wallpaper),
                    summary = stringResource(R.string.appearance_wallpaper_summary),
                    checked = appearance.wallpaper.enabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !WallpaperStore.isSet(context)) {
                            // 还没有图片时先让用户选图，选完自动开启，避免出现「已开启但没有背景」。
                            wallpaperPicker.launch(arrayOf("image/*"))
                        } else {
                            update { current ->
                                current.copy(wallpaper = current.wallpaper.copy(enabled = enabled))
                            }
                        }
                    },
                )
                EtaPreferenceDivider(hasLeading = false)
                EtaArrowPreference(
                    title = if (WallpaperStore.isSet(context)) {
                        stringResource(R.string.appearance_wallpaper_replace)
                    } else {
                        stringResource(R.string.appearance_wallpaper_pick)
                    },
                    onClick = { wallpaperPicker.launch(arrayOf("image/*")) },
                )
                if (WallpaperStore.isSet(context)) {
                    EtaPreferenceDivider(hasLeading = false)
                    EtaArrowPreference(
                        title = stringResource(R.string.appearance_wallpaper_remove),
                        onClick = {
                            WallpaperStore.clear(context)
                            update { current ->
                                current.copy(wallpaper = current.wallpaper.copy(enabled = false))
                            }
                        },
                    )
                }
                AnimatedVisibility(
                    visible = appearance.wallpaper.enabled,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                ) {
                    Column {
                        EtaPreferenceDivider(hasLeading = false)
                        EtaArrowPreference(
                            title = stringResource(R.string.appearance_wallpaper_dim),
                            summary = stringResource(R.string.appearance_wallpaper_dim_summary),
                            endActions = {
                                Text(
                                    text = "${(appearance.wallpaper.dim * 100).roundToInt()}%",
                                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                )
                            },
                            bottomAction = {
                                Slider(
                                    value = appearance.wallpaper.dim.coerceIn(
                                        MIN_WALLPAPER_DIM,
                                        MAX_WALLPAPER_DIM,
                                    ),
                                    onValueChange = { value ->
                                        update { current ->
                                            current.copy(wallpaper = current.wallpaper.copy(dim = value))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    valueRange = MIN_WALLPAPER_DIM..MAX_WALLPAPER_DIM,
                                    showKeyPoints = true,
                                    keyPoints = listOf(0f, 0.25f, 0.5f, 0.85f),
                                    magnetThreshold = 0.01f,
                                    hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                                )
                            },
                        )
                        EtaPreferenceDivider(hasLeading = false)
                        EtaArrowPreference(
                            title = stringResource(R.string.appearance_wallpaper_blur),
                            summary = stringResource(R.string.appearance_wallpaper_blur_summary),
                            endActions = {
                                Text(
                                    text = "${appearance.wallpaper.blurRadius.roundToInt()}dp",
                                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                )
                            },
                            bottomAction = {
                                Slider(
                                    value = appearance.wallpaper.blurRadius.coerceIn(
                                        MIN_WALLPAPER_BLUR,
                                        MAX_WALLPAPER_BLUR,
                                    ),
                                    onValueChange = { value ->
                                        update { current ->
                                            current.copy(wallpaper = current.wallpaper.copy(blurRadius = value))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    valueRange = MIN_WALLPAPER_BLUR..MAX_WALLPAPER_BLUR,
                                    showKeyPoints = true,
                                    keyPoints = listOf(0f, 10f, 20f, 40f),
                                    magnetThreshold = 0.6f,
                                    hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                                )
                            },
                        )
                        EtaPreferenceDivider(hasLeading = false)
                        EtaOverlayDropdownPreference(
                            title = stringResource(R.string.appearance_wallpaper_scale),
                            summary = wallpaperScaleLabels[appearance.wallpaper.scaleMode.ordinal],
                            items = wallpaperScaleLabels,
                            selectedIndex = appearance.wallpaper.scaleMode.ordinal,
                            onSelectedIndexChange = { index ->
                                wallpaperScaleModes.getOrNull(index)?.let { mode ->
                                    update { current ->
                                        current.copy(wallpaper = current.wallpaper.copy(scaleMode = mode))
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }

        item(key = "appearance_interface_title") {
            EtaPreferenceGroupTitle(text = stringResource(R.string.appearance_group_interface))
        }
        item(key = "appearance_interface_card") {
            EtaPreferenceGroup(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                EtaSwitchPreference(
                    title = stringResource(R.string.appearance_blur),
                    summary = stringResource(R.string.appearance_blur_summary),
                    checked = appearance.blurEnabled && blurSupported,
                    onCheckedChange = { enabled ->
                        update { current -> current.copy(blurEnabled = enabled) }
                    },
                    enabled = blurSupported,
                )
                AnimatedVisibility(
                    visible = appearance.blurEnabled && blurSupported,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                ) {
                    // AnimatedVisibility 的内容按层叠摆放，分割线和行必须包进 Column 才会上下排列。
                    Column {
                        EtaPreferenceDivider(hasLeading = false)
                        EtaOverlayDropdownPreference(
                            title = stringResource(R.string.appearance_blur_style),
                            summary = blurStyleSummaries[appearance.topBarBlurStyle.ordinal],
                            items = blurStyleLabels,
                            selectedIndex = appearance.topBarBlurStyle.ordinal,
                            onSelectedIndexChange = { index ->
                                blurStyles.getOrNull(index)?.let { style ->
                                    update { current -> current.copy(topBarBlurStyle = style) }
                                }
                            },
                        )
                    }
                }
                EtaPreferenceDivider(hasLeading = false)
                EtaSwitchPreference(
                    title = stringResource(R.string.appearance_swipe_dismiss),
                    summary = stringResource(R.string.appearance_swipe_dismiss_summary),
                    checked = appearance.swipeDismissEnabled,
                    onCheckedChange = { enabled ->
                        update { current -> current.copy(swipeDismissEnabled = enabled) }
                    },
                )
                EtaPreferenceDivider(hasLeading = false)
                EtaSwitchPreference(
                    title = stringResource(R.string.appearance_predictive_back),
                    summary = stringResource(R.string.appearance_predictive_back_summary),
                    checked = appearance.predictiveBackEnabled,
                    onCheckedChange = { enabled ->
                        update { current -> current.copy(predictiveBackEnabled = enabled) }
                    },
                )
                EtaPreferenceDivider(hasLeading = false)
                EtaArrowPreference(
                    title = stringResource(R.string.appearance_interface_scale),
                    summary = stringResource(R.string.appearance_interface_scale_summary),
                    endActions = {
                        Text(
                            text = "${scaleDraft.roundToInt()}%",
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    },
                    bottomAction = {
                        Slider(
                            value = scaleDraft.coerceIn(
                                MIN_INTERFACE_SCALE * 100f,
                                MAX_INTERFACE_SCALE * 100f,
                            ),
                            onValueChange = { scaleDraft = it },
                            modifier = Modifier.fillMaxWidth(),
                            valueRange = (MIN_INTERFACE_SCALE * 100f)..(MAX_INTERFACE_SCALE * 100f),
                            onValueChangeFinished = { commitScale(scaleDraft) },
                            showKeyPoints = true,
                            keyPoints = listOf(80f, 90f, 100f, 110f),
                            magnetThreshold = 0.01f,
                            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                        )
                    },
                    onClick = {
                        scaleInput = scaleDraft.roundToInt().toString()
                        showScaleDialog = true
                    },
                    holdDownState = showScaleDialog,
                )
            }
        }

        item(key = "appearance_personalize_title") {
            EtaPreferenceGroupTitle(text = stringResource(R.string.appearance_group_personalize))
        }
        item(key = "appearance_personalize_card") {
            EtaPreferenceGroup(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                EtaArrowPreference(
                    title = stringResource(R.string.appearance_bubble_corner),
                    summary = stringResource(R.string.appearance_bubble_corner_summary),
                    endActions = {
                        Text(
                            text = "${(appearance.bubbleCornerScale * 100).roundToInt()}%",
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    },
                    bottomAction = {
                        Slider(
                            value = appearance.bubbleCornerScale.coerceIn(
                                MIN_BUBBLE_CORNER_SCALE,
                                MAX_BUBBLE_CORNER_SCALE,
                            ),
                            onValueChange = { value ->
                                update { current ->
                                    current.copy(bubbleCornerScale = normalizeBubbleCornerScale(value))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            valueRange = MIN_BUBBLE_CORNER_SCALE..MAX_BUBBLE_CORNER_SCALE,
                            showKeyPoints = true,
                            keyPoints = listOf(0f, 0.5f, 1f, 1.6f),
                            magnetThreshold = 0.02f,
                            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                        )
                    },
                )
                EtaPreferenceDivider(hasLeading = false)
                EtaArrowPreference(
                    title = stringResource(R.string.appearance_home_greeting),
                    summary = appearance.homeGreeting.ifBlank {
                        stringResource(R.string.appearance_home_greeting_summary)
                    },
                    onClick = {
                        greetingInput = appearance.homeGreeting
                        showGreetingDialog = true
                    },
                    holdDownState = showGreetingDialog,
                )
            }
        }
    }

    val parsedScale = scaleInput.toIntOrNull()
    EtaWindowDialog(
        show = showScaleDialog,
        title = stringResource(R.string.appearance_interface_scale_dialog_title),
        summary = stringResource(R.string.appearance_interface_scale_dialog_summary),
        onDismissRequest = { showScaleDialog = false },
    ) {
        Column {
            TextField(
                value = scaleInput,
                onValueChange = { value -> scaleInput = value.filter(Char::isDigit).take(3) },
                label = stringResource(R.string.appearance_interface_scale_input_label),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            MiuixDialogActions(
                confirmText = stringResource(R.string.action_confirm),
                confirmEnabled = parsedScale != null && parsedScale in 80..110,
                onCancel = { showScaleDialog = false },
                onConfirm = {
                    parsedScale?.let { commitScale(it.toFloat()) }
                    showScaleDialog = false
                },
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }

    EtaWindowDialog(
        show = showGreetingDialog,
        title = stringResource(R.string.appearance_home_greeting),
        summary = stringResource(R.string.appearance_home_greeting_dialog_summary),
        onDismissRequest = { showGreetingDialog = false },
    ) {
        Column {
            TextField(
                value = greetingInput,
                onValueChange = { value -> greetingInput = value.take(MAX_HOME_GREETING_LENGTH) },
                label = stringResource(R.string.appearance_home_greeting_hint),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            MiuixDialogActions(
                confirmText = stringResource(R.string.action_confirm),
                onCancel = { showGreetingDialog = false },
                onConfirm = {
                    val normalized = normalizeHomeGreeting(greetingInput)
                    update { current -> current.copy(homeGreeting = normalized) }
                    showGreetingDialog = false
                },
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }

    EtaWindowDialog(
        show = showWallpaperError,
        title = stringResource(R.string.appearance_wallpaper_pick_failed),
        summary = stringResource(R.string.appearance_wallpaper_pick_failed_summary),
        onDismissRequest = { showWallpaperError = false },
    ) {
        MiuixDialogActions(
            confirmText = stringResource(R.string.action_confirm),
            onCancel = { showWallpaperError = false },
            onConfirm = { showWallpaperError = false },
        )
    }

    EtaWindowDialog(
        show = showAvatarError,
        title = stringResource(R.string.appearance_avatar_pick_failed),
        summary = stringResource(R.string.appearance_avatar_pick_failed_summary),
        onDismissRequest = { showAvatarError = false },
    ) {
        MiuixDialogActions(
            confirmText = stringResource(R.string.action_confirm),
            onCancel = { showAvatarError = false },
            onConfirm = { showAvatarError = false },
        )
    }

    // 新建主题档案
    EtaWindowDialog(
        show = showNewProfileDialog,
        title = stringResource(R.string.appearance_profile_new),
        summary = stringResource(R.string.appearance_profile_new_dialog_summary),
        onDismissRequest = { showNewProfileDialog = false },
    ) {
        Column {
            TextField(
                value = newProfileName,
                onValueChange = { value -> newProfileName = value.take(24) },
                label = stringResource(R.string.appearance_profile_name_hint),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            MiuixDialogActions(
                confirmText = stringResource(R.string.action_confirm),
                onCancel = { showNewProfileDialog = false },
                onConfirm = { createProfile() },
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }

    // 删除当前档案
    EtaWindowDialog(
        show = showDeleteDialog,
        title = stringResource(R.string.appearance_profile_delete),
        summary = stringResource(R.string.appearance_profile_delete_dialog_summary),
        onDismissRequest = { showDeleteDialog = false },
    ) {
        MiuixDialogActions(
            confirmText = stringResource(R.string.action_delete),
            destructive = true,
            onCancel = { showDeleteDialog = false },
            onConfirm = { deleteActiveProfile() },
            modifier = Modifier.padding(top = 16.dp),
        )
    }

    // 气泡样式编辑（用户/AI 独立）
    if (showBubbleDialog) {
        BubbleStyleDialog(
            profile = activeProfile,
            onDismiss = { showBubbleDialog = false },
            onSave = { updated -> saveProfile(updated) },
        )
    }
}
