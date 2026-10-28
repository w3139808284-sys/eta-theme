package io.github.mangi.eta.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.R
import io.github.mangi.eta.data.model.BubbleMaterial
import io.github.mangi.eta.data.model.BubbleStyle
import io.github.mangi.eta.data.model.MAX_BUBBLE_CORNER_RADIUS
import io.github.mangi.eta.data.model.MAX_PADDING
import io.github.mangi.eta.data.model.MIN_BUBBLE_CORNER_RADIUS
import io.github.mangi.eta.data.model.MIN_PADDING
import io.github.mangi.eta.data.model.ThemeProfile
import io.github.mangi.eta.data.model.normalizeBubbleCornerRadius
import io.github.mangi.eta.data.model.normalizePadding
import io.github.mangi.eta.ui.components.EtaChoiceChip
import io.github.mangi.eta.ui.components.EtaWindowDialog
import io.github.mangi.eta.ui.components.MiuixDialogActions
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 气泡样式编辑对话框：用户/AI 分别编辑，实时保存到主题档案。 */
@Composable
internal fun BubbleStyleDialog(
    profile: ThemeProfile,
    onDismiss: () -> Unit,
    onSave: (ThemeProfile) -> Unit,
) {
    var userStyle by remember(profile.id) { mutableStateOf(profile.bubbles.user) }
    var aiStyle by remember(profile.id) { mutableStateOf(profile.bubbles.ai) }

    EtaWindowDialog(
        show = true,
        title = stringResource(R.string.appearance_bubble_edit),
        onDismissRequest = onDismiss,
    ) {
        Column {
            Text(
                text = stringResource(R.string.appearance_bubble_user),
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
            )
            BubbleStyleEditor(
                style = userStyle,
                onStyleChange = { userStyle = it },
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Text(
                text = stringResource(R.string.appearance_bubble_ai),
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
            )
            BubbleStyleEditor(
                style = aiStyle,
                onStyleChange = { aiStyle = it },
            )

            MiuixDialogActions(
                confirmText = stringResource(R.string.action_save),
                onCancel = onDismiss,
                onConfirm = {
                    onSave(profile.copy(bubbles = profile.bubbles.copy(user = userStyle, ai = aiStyle)))
                    onDismiss()
                },
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

private val presetBubbleColors = listOf(
    null, // 跟随主题
    0xFF3482FF, 0xFF6750A4, 0xFFB0006D, 0xFF006D3B, 0xFF2C3E50,
    0xFFFFFFFF, 0xFF1F2937, 0xFFFF6B6B,
)

@Composable
private fun BubbleStyleEditor(
    style: BubbleStyle,
    onStyleChange: (BubbleStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 材质：用胶囊标签互斥选择，避免在对话框内嵌套下拉弹窗（需根 Scaffold，会崩）。
        // 标签与枚举一一对应；新增枚举值时必须同步补齐，否则会越界。
        val materialLabels = listOf(
            stringResource(R.string.appearance_bubble_material_solid),
            stringResource(R.string.appearance_bubble_material_glass),
            stringResource(R.string.appearance_bubble_material_liquid_glass),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text(
                text = stringResource(R.string.appearance_bubble_material),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            BubbleMaterial.entries.forEach { material ->
                // 用 getOrNull 兜底：万一将来枚举新增而标签未同步，也不会崩溃。
                val label = materialLabels.getOrNull(material.ordinal) ?: material.name
                EtaChoiceChip(
                    text = label,
                    selected = style.material == material,
                    onClick = { onStyleChange(style.copy(material = material)) },
                )
            }
        }
        // 颜色色板
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text = stringResource(R.string.appearance_bubble_color),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            presetBubbleColors.forEach { argb ->
                val selected = style.backgroundArgb == argb
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                argb == null -> MiuixTheme.colorScheme.surfaceContainerHigh
                                else -> Color(argb.toInt())
                            },
                        )
                        .clickable { onStyleChange(style.copy(backgroundArgb = argb)) }
                        .then(
                            if (selected) {
                                Modifier.border(
                                    width = 2.dp,
                                    color = MiuixTheme.colorScheme.primary,
                                    shape = CircleShape,
                                )
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (argb == null) {
                        Text(
                            text = "A",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
        // 圆角
        Slider(
            value = style.cornerRadiusDp.coerceIn(MIN_BUBBLE_CORNER_RADIUS, MAX_BUBBLE_CORNER_RADIUS),
            onValueChange = { value ->
                onStyleChange(style.copy(cornerRadiusDp = normalizeBubbleCornerRadius(value)))
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            valueRange = MIN_BUBBLE_CORNER_RADIUS..MAX_BUBBLE_CORNER_RADIUS,
            showKeyPoints = true,
            keyPoints = listOf(0f, 12f, 20f, 32f),
            magnetThreshold = 0.5f,
            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
        )
        Text(
            text = stringResource(R.string.appearance_bubble_corner_value, style.cornerRadiusDp.roundToInt()),
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        // 内边距
        Slider(
            value = style.paddingHorizontalDp.coerceIn(MIN_PADDING, MAX_PADDING),
            onValueChange = { value ->
                onStyleChange(
                    style.copy(
                        paddingHorizontalDp = normalizePadding(value),
                        paddingVerticalDp = normalizePadding(value * 0.75f),
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            valueRange = MIN_PADDING..MAX_PADDING,
            showKeyPoints = true,
            keyPoints = listOf(8f, 16f, 24f),
            magnetThreshold = 0.5f,
            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
        )
        Text(
            text = stringResource(R.string.appearance_bubble_padding_value, style.paddingHorizontalDp.roundToInt()),
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
