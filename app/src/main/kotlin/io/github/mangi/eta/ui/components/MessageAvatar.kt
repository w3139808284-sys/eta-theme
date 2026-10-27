package io.github.mangi.eta.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.data.model.AvatarSettings
import io.github.mangi.eta.data.model.AvatarShape

/**
 * 消息头像：复用 [CharacterAvatar] 的解码缓存与字标回退，只负责按设置换算形状与尺寸。
 *
 * 无图片时用 fallbackName 生成稳定字标，保证未设置头像时也有视觉占位。
 */
@Composable
internal fun MessageAvatar(
    settings: AvatarSettings,
    imagePath: String?,
    fallbackName: String,
    modifier: Modifier = Modifier,
) {
    val size = settings.sizeDp.dp
    val shape: Shape = when (settings.shape) {
        AvatarShape.CIRCLE -> CircleShape
        AvatarShape.ROUNDED -> RoundedCornerShape(settings.cornerRadiusDp.dp)
    }
    CharacterAvatar(
        name = fallbackName,
        avatarPath = imagePath,
        size = size,
        shape = shape,
        modifier = modifier.size(size),
    )
}

/** 头像与气泡之间的水平间距。 */
internal val AvatarGap = 8.dp
