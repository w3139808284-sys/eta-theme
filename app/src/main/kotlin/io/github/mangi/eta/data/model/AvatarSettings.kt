package io.github.mangi.eta.data.model

import kotlinx.serialization.Serializable

/** 头像形状：圆形 / 圆角方形。 */
@Serializable
enum class AvatarShape(val persistedValue: String) {
    CIRCLE("circle"),
    ROUNDED("rounded");

    companion object {
        fun fromPersistedValue(value: String?): AvatarShape =
            entries.firstOrNull { it.persistedValue == value } ?: CIRCLE
    }
}

const val MIN_AVATAR_SIZE = 24f
const val MAX_AVATAR_SIZE = 48f
const val DEFAULT_AVATAR_SIZE = 32f

const val MIN_AVATAR_CORNER = 0f
const val MAX_AVATAR_CORNER = 16f
const val DEFAULT_AVATAR_CORNER = 8f

/**
 * 双方头像设置：用户与 AI 各自独立开关，共享形状与尺寸。
 *
 * 图片本体保存在 App 私有目录（见 AvatarStore），这里只持久化路径与显示参数，
 * 因此不会把用户相册图片写进主题 JSON 或备份。
 */
@Serializable
data class AvatarSettings(
    /** 用户头像开关；默认关闭，保持历史无头像的观感。 */
    val userEnabled: Boolean = false,
    /** AI 头像开关；默认关闭。 */
    val aiEnabled: Boolean = false,
    /** 用户头像文件绝对路径；null 表示未设置（回退为字标）。 */
    val userImagePath: String? = null,
    /** AI 头像文件绝对路径；null 表示未设置。 */
    val aiImagePath: String? = null,
    val shape: AvatarShape = AvatarShape.CIRCLE,
    val sizeDp: Float = DEFAULT_AVATAR_SIZE,
    val cornerRadiusDp: Float = DEFAULT_AVATAR_CORNER,
    /** AI 名字标签；空串表示不显示。 */
    val aiName: String = "",
) {
    fun normalized(): AvatarSettings = copy(
        sizeDp = normalizeAvatarSize(sizeDp),
        cornerRadiusDp = normalizeAvatarCorner(cornerRadiusDp),
        aiName = normalizeAiName(aiName),
    )
}

const val MAX_AI_NAME_LENGTH = 24

fun normalizeAiName(value: String): String =
    value.trim().replace('\n', ' ').take(MAX_AI_NAME_LENGTH)

fun normalizeAvatarSize(value: Float): Float =
    if (value.isFinite()) value.coerceIn(MIN_AVATAR_SIZE, MAX_AVATAR_SIZE) else DEFAULT_AVATAR_SIZE

fun normalizeAvatarCorner(value: Float): Float =
    if (value.isFinite()) value.coerceIn(MIN_AVATAR_CORNER, MAX_AVATAR_CORNER) else DEFAULT_AVATAR_CORNER
