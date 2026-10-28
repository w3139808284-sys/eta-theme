package io.github.mangi.eta.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mangi.eta.R
import io.github.mangi.eta.agent.model.AgentFileReferencePromptCodec
import io.github.mangi.eta.agent.overlay.toolDisplayName
import io.github.mangi.eta.ui.markdown.MarkdownTone
import io.github.mangi.eta.ui.markdown.StaticMarkdown
import io.github.mangi.eta.ui.markdown.StreamingMarkdown
import io.github.mangi.eta.ui.markdown.StreamingMarkdownState
import io.github.mangi.eta.ui.markdown.THINKING_LINE_HEIGHT_SP
import io.github.mangi.eta.data.model.BubbleMaterial
import io.github.mangi.eta.ui.app.AppRadius
import io.github.mangi.eta.ui.app.LocalAppearanceSettings
import io.github.mangi.eta.ui.app.LocalAvatarSettings
import io.github.mangi.eta.ui.app.LocalBubbleStyles
import io.github.mangi.eta.ui.model.AgentChatMessageUi
import io.github.mangi.eta.ui.model.AgentMessageUi
import io.github.mangi.eta.ui.model.RunTraceMessageUi
import io.github.mangi.eta.ui.model.SuggestionChipsMessageUi
import io.github.mangi.eta.ui.model.SystemNoticeCode
import io.github.mangi.eta.ui.model.SystemNoticeMessageUi
import io.github.mangi.eta.ui.model.ThinkingMessageUi
import io.github.mangi.eta.ui.model.ToolActivityMessageUi
import io.github.mangi.eta.ui.model.ToolSummaryMessageUi
import io.github.mangi.eta.ui.model.UserMessageUi
import io.github.mangi.eta.ui.voice.SpeechReadAloudButton
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.RichTooltip
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipAnchorPosition
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.TooltipDefaults
import top.yukonga.miuix.kmp.basic.rememberTooltipState
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun rememberDataUrlBitmap(dataUrl: String) = remember(dataUrl) {
    decodeDataUrlBitmap(dataUrl)
}

internal fun decodeDataUrlBitmap(dataUrl: String): ImageBitmap? {
    val base64 = dataUrl.substringAfter("base64,", "")
    if (base64.isBlank()) return null
    return runCatching {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
}

/**
 * 等待首个文本片段时的轻量反馈。
 */
@Composable
fun AITypingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val delay = index * 150
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = delay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer(alpha = alpha)
                    .background(MiuixTheme.colorScheme.onSurfaceVariantSummary, CircleShape)
            )
        }
    }
}

/**
 * 只有正在执行的状态才持有无限动画。历史思考和工具条目保持静态，避免长会话里
 * 每个已完成节点都持续产生帧时钟与状态更新。
 */
@Composable
internal fun rememberActivePulse(
    active: Boolean,
    label: String,
): Float {
    if (!active) return 1f
    val transition = rememberInfiniteTransition(label = label)
    val alpha by transition.animateFloat(
        initialValue = 0.58f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(820, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "${label}_alpha",
    )
    return alpha
}

/** 消息角色归类：用户消息为 "user"，其余（AI/系统/工具/思考）统一为 "assistant"。 */
internal fun AgentChatMessageUi.chatRole(): String =
    if (this is UserMessageUi) "user" else "assistant"

@Composable
internal fun ChatMessageItem(
    message: AgentChatMessageUi,
    previousRole: String?,
    onSuggestionClick: (String) -> Unit,
    onRunTraceClick: () -> Unit,
    onOpenBrowser: () -> Unit,
    showBrowserShortcut: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    assistantOverlay: Boolean = false,
    retainedStreamingState: StreamingMarkdownState? = null,
    showCopyAction: Boolean = true,
    showMessageActions: Boolean = false,
    messageActionsEnabled: Boolean = true,
    isEditing: Boolean = false,
    onEditMessage: (String) -> Unit = {},
    onDeleteMessage: (String) -> Unit = {},
    onRegenerateMessage: (String) -> Unit = {},
    onSelectReplyCandidate: (String, Int) -> Unit = { _, _ -> },
) {
    // 垂直间距：与上一条同角色 4dp，不同角色或首条 16dp。
    val currentRole = message.chatRole()
    val isGroupHead = previousRole == null || previousRole != currentRole
    val topMargin = if (isGroupHead) 16.dp else 4.dp
    when (message) {
        is UserMessageUi -> UserMessageBubble(
            message = message,
            assistantOverlay = assistantOverlay,
            actionsEnabled = messageActionsEnabled,
            isEditing = isEditing,
            onEdit = { onEditMessage(message.id) },
            onDelete = { onDeleteMessage(message.id) },
            topMargin = topMargin,
            modifier = modifier,
        )
        is AgentMessageUi -> AgentMessageBlock(
            message = message,
            allowSpeech = true,
            assistantOverlay = assistantOverlay,
            retainedStreamingState = retainedStreamingState,
            showCopyAction = showCopyAction,
            showMessageActions = showMessageActions,
            messageActionsEnabled = messageActionsEnabled,
            onDelete = { onDeleteMessage(message.id) },
            onRegenerate = { onRegenerateMessage(message.id) },
            onEdit = { onEditMessage(message.id) },
            onSelectCandidate = { onSelectReplyCandidate(message.id, it) },
            topMargin = topMargin,
            isGroupHead = isGroupHead,
            modifier = modifier,
        )
        is SystemNoticeMessageUi -> if (message.code == SystemNoticeCode.ContextCompaction) {
            ContextCompactionMarker(message = message, modifier = modifier)
        } else {
            AgentMessageBlock(
                message = AgentMessageUi(
                    id = message.id,
                    content = buildString {
                        append(
                            stringResource(
                                when (message.code) {
                                    SystemNoticeCode.Stopped -> R.string.system_notice_stopped
                                    SystemNoticeCode.EmptyResult -> R.string.system_notice_empty_result
                                    SystemNoticeCode.ContextCompaction -> R.string.context_compaction
                                    SystemNoticeCode.ModelRetry -> R.string.system_notice_model_retry
                                    SystemNoticeCode.RuntimeFailed -> R.string.system_notice_runtime_failed
                                    SystemNoticeCode.Interrupted -> R.string.system_notice_interrupted
                                },
                            ),
                        )
                        message.detail?.takeIf(String::isNotBlank)?.let { detail ->
                            append("\n\n")
                            append(detail)
                        }
                    },
                    renderMarkdown = false,
                ),
                assistantOverlay = false,
                retainedStreamingState = null,
                showCopyAction = showCopyAction,
                showMessageActions = showMessageActions,
                messageActionsEnabled = messageActionsEnabled,
                onDelete = { onDeleteMessage(message.id) },
                onRegenerate = { onRegenerateMessage(message.id) },
                topMargin = topMargin,
                isGroupHead = isGroupHead,
                modifier = modifier,
            )
        }
        is ThinkingMessageUi -> ThinkingRow(
            message = message,
            retainedStreamingState = retainedStreamingState,
            modifier = modifier,
            compact = compact,
        )
        is RunTraceMessageUi -> RunTraceRow(message = message, onClick = onRunTraceClick, modifier = modifier)
        is ToolActivityMessageUi -> ToolActivityInline(
            message = message,
            onOpenBrowser = onOpenBrowser,
            showBrowserShortcut = showBrowserShortcut,
            modifier = modifier,
            compact = compact,
        )
        is ToolSummaryMessageUi -> ToolSummaryInline(message = message, modifier = modifier, compact = compact)
        is SuggestionChipsMessageUi -> SuggestionChipsRow(message = message, onSuggestionClick = onSuggestionClick, modifier = modifier)
    }
}

// ── 用户消息：轻盈美观气泡 ──────────────────────────────────────────────

@Composable
private fun UserMessageBubble(
    message: UserMessageUi,
    assistantOverlay: Boolean,
    actionsEnabled: Boolean,
    isEditing: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    topMargin: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val tooltipState = rememberTooltipState(isPersistent = true)
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(actionsEnabled) {
        if (!actionsEnabled) tooltipState.dismiss()
    }
    LaunchedEffect(tooltipState) {
        snapshotFlow { tooltipState.isVisible }.collect { visible ->
            if (visible) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    val visiblePrompt = remember(message.content) {
        AgentFileReferencePromptCodec.parse(message.content)
    }
    val overlayBubbleColor = if (MiuixTheme.colorScheme.background.luminance() < 0.5f) {
        Color(0xFF37393D)
    } else {
        Color(0xFFE5E7EA)
    }
    // 气泡样式来自激活主题档案；默认值与历史一致，未配置主题时行为完全不变。
    val userStyle = LocalBubbleStyles.current.user
    val cornerScale = LocalAppearanceSettings.current.bubbleCornerScale
    val baseCorner = userStyle.cornerRadiusDp.dp
    val bubbleCorner = (baseCorner * cornerScale).coerceAtLeast(0.dp)
    val bubbleTailCorner = (baseCorner * (0.3f * cornerScale)).coerceAtLeast(0.dp)
    val userBubbleColor = userStyle.backgroundArgb?.let { Color(it.toInt()) }
        ?: MiuixTheme.colorScheme.surfaceContainerHigh

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = if (assistantOverlay) 4.dp else topMargin,
            ),
    ) {
        val avatarSettings = LocalAvatarSettings.current
        val showAvatar = avatarSettings.userEnabled && !assistantOverlay
        val avatarReserve = if (showAvatar) avatarSettings.sizeDp.dp + AvatarGap else 0.dp
        val bubbleMaxWidth = minOf(
            (320.dp * userStyle.maxWidthFraction),
            (maxWidth - avatarReserve) * 0.78f,
        ).coerceAtLeast(160.dp)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top,
    ) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = TooltipAnchorPosition.Below,
            ),
            tooltip = {
                RichTooltip(insideMargin = PaddingValues(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        MessageTooltipAction(
                            icon = Icons.Rounded.ContentCopy,
                            label = stringResource(R.string.ui_copy_4edd1d),
                            onClick = {
                                @Suppress("DEPRECATION")
                                clipboardManager.setText(AnnotatedString(message.content))
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                tooltipState.dismiss()
                            },
                        )
                        MessageTooltipAction(
                            icon = Icons.Rounded.Edit,
                            label = stringResource(R.string.ui_edit_a7f814),
                            onClick = {
                                tooltipState.dismiss()
                                onEdit()
                            },
                        )
                        MessageTooltipAction(
                            icon = Icons.Rounded.Delete,
                            label = stringResource(R.string.ui_delete_3755f5),
                            onClick = {
                                tooltipState.dismiss()
                                onDelete()
                            },
                        )
                    }
                }
            },
            state = tooltipState,
            focusable = true,
            enableUserInput = actionsEnabled,
        ) {
            // Liquid Glass 需要采样气泡背后的背景层（不含气泡自身）。
            val backgroundBackdrop = LocalBackgroundBackdrop.current
            // AGSL 坐标系是渲染像素，圆角必须换算成 px 再传给 shader。
            val bubbleCornerPx = with(LocalDensity.current) { bubbleCorner.toPx() }
            Column(
                modifier = Modifier
                    .widthIn(max = bubbleMaxWidth)
                    .then(
                        if (assistantOverlay) {
                            Modifier.background(overlayBubbleColor, RoundedCornerShape(10.dp))
                        } else if (
                            userStyle.material == BubbleMaterial.GLASS ||
                            userStyle.material == BubbleMaterial.LIQUID_GLASS
                        ) {
                            // 不在此处 clip：GLASS 内部自己 clip，LIQUID_GLASS 的 textureBlur
                            // 用 shape 参数裁剪，双重裁剪会让圆角边缘出现锯齿/缺失。
                            Modifier.bubbleSurface(
                                material = userStyle.material,
                                cornerRadiusPx = bubbleCornerPx,
                                shape = RoundedCornerShape(
                                    topStart = bubbleCorner,
                                    topEnd = bubbleCorner,
                                    bottomEnd = if (isEditing) bubbleCorner else bubbleTailCorner,
                                    bottomStart = bubbleCorner,
                                ),
                                baseColor = userBubbleColor,
                                backdrop = backgroundBackdrop,
                            )
                        } else {
                            Modifier.squircleSurface(
                                color = userBubbleColor,
                                topStart = bubbleCorner,
                                topEnd = bubbleCorner,
                                // 描边只支持统一圆角，编辑态收起气泡尾角，让描边与填充贴合。
                                bottomEnd = if (isEditing) bubbleCorner else bubbleTailCorner,
                                bottomStart = bubbleCorner,
                            )
                        }
                    )
                    .then(
                        if (isEditing) {
                            Modifier.squircleBorder(
                                width = 1.dp,
                                color = MiuixTheme.colorScheme.primary,
                                cornerRadius = bubbleCorner,
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(
                        horizontal = if (assistantOverlay) 12.dp else userStyle.paddingHorizontalDp.dp,
                        vertical = if (assistantOverlay) 8.dp else userStyle.paddingVerticalDp.dp,
                    ),
            ) {
                if (message.images.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        message.images.forEach { dataUrl ->
                            val bitmap = rememberDataUrlBitmap(dataUrl)
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = stringResource(R.string.chat_image_attachment),
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                }
                if (visiblePrompt.references.isNotEmpty()) {
                    SentFileReferenceFlow(
                        references = visiblePrompt.references,
                        modifier = Modifier.padding(
                            bottom = if (visiblePrompt.request.isNotBlank()) 8.dp else 0.dp
                        ),
                    )
                }
                if (visiblePrompt.request.isNotBlank()) {
                    SelectionContainer {
                        Text(
                            text = visiblePrompt.request,
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
                if (message.isEdited) {
                    Text(
                        text = stringResource(R.string.ui_edited_c36776),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        if (showAvatar) {
            Spacer(Modifier.width(AvatarGap))
            MessageAvatar(
                settings = avatarSettings,
                imagePath = avatarSettings.userImagePath,
                fallbackName = stringResource(R.string.avatar_user_fallback),
            )
        }
    }
    }
}

@Composable
private fun MessageTooltipAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            // 下方文字已经命名该操作，图标不再重复朗读。
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MiuixTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface,
        )
    }
}

// ── 上下文压缩：时间线中的轻量胶囊标记 ─────────────────────────────────

/**
 * 压缩不是一轮对话结果，而是上下文维护事件；用居中胶囊标记与助手正文区分，
 * 进行中通过图标脉冲反馈，结束后保留压缩前后的 token 信息。
 */
@Composable
private fun ContextCompactionMarker(
    message: SystemNoticeMessageUi,
    modifier: Modifier = Modifier,
) {
    val pulseAlpha = rememberActivePulse(active = message.running, label = "compaction_pulse")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(MiuixTheme.colorScheme.surface)
                .border(
                    0.5.dp,
                    MiuixTheme.colorScheme.outline.copy(alpha = 0.5f),
                    RoundedCornerShape(percent = 50),
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Compress,
                contentDescription = null,
                modifier = Modifier
                    .size(12.dp)
                    .graphicsLayer(alpha = if (message.running) pulseAlpha else 1f),
                tint = if (message.running) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                },
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = message.detail?.takeIf(String::isNotBlank)
                    ?: stringResource(R.string.context_compaction),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Agent 结果 ───────────────────────────────────────────────────────

/**
 * AI 正文承载容器：为 StaticMarkdown / StreamingMarkdown 提供独立表面，
 * 避免正文裸字压在壁纸上。
 *
 * 样式来源：`BubbleStyleSet.ai`（backgroundArgb / cornerRadiusDp / padding* / material /
 * maxWidthFraction），未配置时回落到历史默认（surface 72% + AppRadius.card + 14/12）。
 * 文字色 textArgb 为空时沿用主题 onSurface，避免未配置时改变可读性。
 *
 * [nameLabel] 非空时在气泡正上方左对齐渲染名字标签，底边距气泡顶边 4dp。
 */
@Composable
private fun AiBodyContainer(
    modifier: Modifier = Modifier,
    nameLabel: String? = null,
    content: @Composable () -> Unit,
) {
    val aiStyle = LocalBubbleStyles.current.ai
    val aiMaxWidthFraction = aiStyle.maxWidthFraction
    val fillColor = aiStyle.backgroundArgb?.let { Color(it.toInt()) }
        ?: MiuixTheme.colorScheme.surface.copy(alpha = 0.72f)
    val corner = aiStyle.cornerRadiusDp.dp
    // AGSL 用像素坐标，圆角需换算成 px。
    val cornerPx = with(LocalDensity.current) { corner.toPx() }
    val horizontalPadding = aiStyle.paddingHorizontalDp.dp
    val verticalPadding = aiStyle.paddingVerticalDp.dp
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val aiMaxWidth = minOf(
            (320.dp * aiMaxWidthFraction),
            maxWidth * 0.78f,
        ).coerceAtLeast(240.dp)
        Column(modifier = Modifier.widthIn(max = aiMaxWidth)) {
            if (!nameLabel.isNullOrBlank()) {
                Text(
                    text = nameLabel,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (
                            aiStyle.material == BubbleMaterial.GLASS ||
                            aiStyle.material == BubbleMaterial.LIQUID_GLASS
                        ) {
                            // 同用户气泡：不在外层 clip，避免与 textureBlur 的 shape 双重裁剪。
                            Modifier.bubbleSurface(
                                material = aiStyle.material,
                                cornerRadiusPx = cornerPx,
                                shape = RoundedCornerShape(corner),
                                baseColor = fillColor,
                                backdrop = LocalBackgroundBackdrop.current,
                            )
                        } else {
                            Modifier.squircleSurface(
                                color = fillColor,
                                cornerRadius = corner,
                            )
                        },
                    )
                    .squircleBorder(
                        width = 1.dp,
                        color = MiuixTheme.colorScheme.outline.copy(alpha = 0.12f),
                        cornerRadius = corner,
                    )
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun AgentMessageBlock(
    message: AgentMessageUi,
    allowSpeech: Boolean = false,
    assistantOverlay: Boolean = false,
    retainedStreamingState: StreamingMarkdownState?,
    showCopyAction: Boolean,
    showMessageActions: Boolean,
    messageActionsEnabled: Boolean,
    onDelete: () -> Unit,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit = {},
    onSelectCandidate: (Int) -> Unit = {},
    topMargin: Dp = 0.dp,
    isGroupHead: Boolean = true,
    modifier: Modifier = Modifier,
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var copied by remember(message.id) { mutableStateOf(false) }
    val keepStreamingMarkdown = remember(message.id) { message.isStreaming }
    var streamingRevealComplete by remember(message.id) {
        mutableStateOf(!keepStreamingMarkdown)
    }
    // 渲染会话由列表层按 message.id 持有，item 滚出视口被销毁后滑回时复用同一
    // 会话；没有外部持有者时（如嵌套条目）退回组合内 remember，行为与之前一致。
    val streamingState = if (keepStreamingMarkdown) {
        retainedStreamingState ?: remember(message.id) { StreamingMarkdownState() }
    } else {
        null
    }
    val completedDocument = (streamingState ?: retainedStreamingState)
        ?.snapshot?.completedDocumentFor(message.content)
    val revealComplete = streamingRevealComplete && !message.isStreaming &&
        (streamingState == null || completedDocument != null)
    LaunchedEffect(retainedStreamingState, revealComplete, message.content) {
        retainedStreamingState?.revealedContent = message.content.takeIf { revealComplete }
    }
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(1_400)
            copied = false
        }
    }

    val character = LocalRoleplayChat.current.takeUnless { assistantOverlay }
    val isGreeting = character != null && message.id.startsWith(GreetingMessagePrefix)
    // AI 侧头像：仅在组首显示，避免连续回复时重复。
    val avatarSettings = LocalAvatarSettings.current
    val showAvatar = avatarSettings.aiEnabled && !assistantOverlay && isGroupHead
    // 名字标签：角色对话已有 CharacterReplyHeader，此处不再重复。
    val nameLabel = avatarSettings.aiName
        .takeIf { it.isNotBlank() && character == null && !assistantOverlay }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = topMargin,
            ),
        verticalAlignment = Alignment.Top,
    ) {
        if (showAvatar) {
            MessageAvatar(
                settings = avatarSettings,
                imagePath = avatarSettings.aiImagePath,
                fallbackName = stringResource(R.string.avatar_ai_fallback),
            )
            Spacer(Modifier.width(AvatarGap))
        }
        Column(modifier = Modifier.weight(1f)) {
        if (character != null && isGroupHead) {
            CharacterReplyHeader(character = character, isGreeting = isGreeting)
        }
        when {
            message.content.isBlank() && message.isStreaming -> {
                AITypingIndicator(
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            streamingState != null && !revealComplete -> {
                AiBodyContainer(nameLabel = nameLabel) {
                    StreamingMarkdown(
                        state = streamingState,
                        content = message.content,
                        isStreaming = message.isStreaming,
                        tone = MarkdownTone.Answer,
                        onRevealCompleteChange = { streamingRevealComplete = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            message.renderMarkdown -> {
                SelectionContainer {
                    AiBodyContainer(nameLabel = nameLabel) {
                        StaticMarkdown(
                            content = message.content,
                            tone = MarkdownTone.Answer,
                            parsed = completedDocument,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            message.content.isNotBlank() -> {
                SelectionContainer {
                    AiBodyContainer(nameLabel = nameLabel) {
                        Text(
                            text = message.content,
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // 回答显现完成后才出现操作行；淡入展开避免贴底滚动时整页突然跳动一行高度。
        AnimatedVisibility(
            visible = showCopyAction &&
                !message.isStreaming &&
                message.content.isNotBlank() &&
                revealComplete,
            enter = fadeIn(tween(160)) + expandVertically(tween(160)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(120)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // 抵消按钮的居中留白，与工作过程标题图标共用左侧中心线。
                    .offset(x = -AnswerActionCenterOffset)
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnswerActionButton(
                    icon = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                    contentDescription = stringResource(
                        if (copied) R.string.copy_copied else R.string.copy_answer,
                    ),
                    onClick = {
                        @Suppress("DEPRECATION")
                        clipboardManager.setText(AnnotatedString(message.content))
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        copied = true
                    },
                    tint = if (copied) MiuixTheme.colorScheme.primary else null,
                )
                if (allowSpeech) SpeechReadAloudButton(message.id, message.content)
                if (showMessageActions) {
                    if (message.characterEditable) {
                        AnswerActionButton(
                            icon = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.chat_edit_character_reply),
                            onClick = onEdit,
                            enabled = messageActionsEnabled,
                        )
                    }
                    TooltipBox(text = stringResource(R.string.ui_regenerate_2e1905), enabled = messageActionsEnabled) {
                        AnswerActionButton(
                            icon = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.ui_regenerate_reply_84a7d9),
                            onClick = onRegenerate,
                            enabled = messageActionsEnabled,
                        )
                    }
                    TooltipBox(text = stringResource(R.string.ui_delete_3755f5), enabled = messageActionsEnabled) {
                        AnswerActionButton(
                            icon = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.ui_delete_this_conversation_3f351b),
                            onClick = onDelete,
                            enabled = messageActionsEnabled,
                        )
                    }
                    if (message.characterEditable && message.candidateCount > 1) {
                        Spacer(Modifier.weight(1f))
                        CandidateSwitcher(
                            selected = message.selectedCandidate,
                            count = message.candidateCount,
                            enabled = messageActionsEnabled,
                            onSelect = onSelectCandidate,
                        )
                    }
                }
            }
        }
    }
    }
}

/** 开场白消息由开始对话时写入，标识前缀与写入方保持一致。 */
private const val GreetingMessagePrefix = "greeting-"

/**
 * 角色回复的署名行：小头像加角色名，开场白额外标注“故事开始”。
 * 正文保持与普通回答同宽，避免为头像缩进后长段落在窄屏上过于拥挤。
 */
@Composable
private fun CharacterReplyHeader(character: RoleplayChatUi, isGreeting: Boolean) {
    Row(
        modifier = Modifier.padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CharacterAvatar(
            name = character.characterName,
            avatarPath = character.avatarPath,
            size = 28.dp,
            shape = CircleShape,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = character.characterName,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (isGreeting) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.chat_character_story_start),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

/** 思考卡片与运行记录等全宽描边卡片共用的描边强度，相邻时轮廓一致。 */
private const val ChatCardOutlineAlpha = 0.5f

/** 回答操作按钮的触控尺寸；行整体左移半个尺寸减去图标半宽，使图标落在工作过程图标的中心线上。 */
internal val AnswerActionSize = 36.dp
private val AnswerActionCenterOffset = 11.dp

/** 回答下方的轻量操作：图标显式着色，因此禁用态需要自己换成禁用色，否则看起来仍可点。 */
@Composable
internal fun AnswerActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color? = null,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        minWidth = AnswerActionSize,
        minHeight = AnswerActionSize,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = when {
                !enabled -> MiuixTheme.colorScheme.disabledOnSurface
                tint != null -> tint
                else -> MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.75f)
            },
        )
    }
}

@Composable
private fun CandidateSwitcher(
    selected: Int,
    count: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 2.dp),
    ) {
        AnswerActionButton(
            icon = Icons.Rounded.ChevronLeft,
            contentDescription = stringResource(R.string.chat_previous_candidate),
            onClick = { onSelect(selected - 1) },
            enabled = enabled && selected > 0,
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Text(
            text = "${selected + 1}/$count",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 28.dp),
        )
        AnswerActionButton(
            icon = Icons.Rounded.ChevronRight,
            contentDescription = stringResource(R.string.chat_next_candidate),
            onClick = { onSelect(selected + 1) },
            enabled = enabled && selected < count - 1,
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

// ── 思考过程 ─────────────────────────────────────────────────────────

@Composable
internal fun ThinkingRow(
    message: ThinkingMessageUi,
    retainedStreamingState: StreamingMarkdownState?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    // null 表示跟随思考生命周期；用户点过之后只认手动选择。
    var manualExpanded by rememberSaveable(message.id) { mutableStateOf<Boolean?>(null) }
    val keepStreamingMarkdown = remember(message.id) { message.isStreaming }
    val streamingState = if (keepStreamingMarkdown) {
        retainedStreamingState ?: remember(message.id) { StreamingMarkdownState() }
    } else {
        null
    }
    val completedDocument = (streamingState ?: retainedStreamingState)
        ?.snapshot?.completedDocumentFor(message.content)
    // 展开态必须在组合期同步推导：若经 LaunchedEffect 晚一帧收起，思考结束的那一帧尾部窗口
    // 已解除而正文仍展开，会以全文高度闪现一帧，再从该高度执行收起动画。
    val expanded = manualExpanded ?: message.isStreaming
    // 自动模式下高度上限覆盖到收起动画结束，退出过程不会先撑开再收拢。
    val tailWindow = manualExpanded == null

    val pulseAlpha = rememberActivePulse(
        active = message.isStreaming,
        label = "thinking_pulse",
    )

    // compact 模式渲染在工作过程时间线内，节点徽章已承担图标，这里只保留文字与展开控制。
    val aiCardCorner = AppRadius.card
    val containerModifier = if (compact) {
        modifier.fillMaxWidth()
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .squircleSurface(
                color = MiuixTheme.colorScheme.surface,
                cornerRadius = aiCardCorner,
            )
            .squircleBorder(
                width = 1.dp,
                color = MiuixTheme.colorScheme.outline.copy(alpha = ChatCardOutlineAlpha),
                cornerRadius = aiCardCorner,
            )
    }

    Column(modifier = containerModifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    // 尾部窗口下点击视为要看全文，保持展开并放开高度限制。
                    manualExpanded = (tailWindow && expanded) || !expanded
                }
                .then(
                    if (compact) {
                        Modifier.heightIn(min = WorkRowHeight)
                    } else {
                        Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!compact) {
                Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier
                        .size(15.dp)
                        .graphicsLayer(alpha = if (message.isStreaming) pulseAlpha else 1f),
                    tint = if (message.isStreaming) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            ShimmerText(
                active = message.isStreaming,
                text = if (message.isStreaming) {
                    stringResource(R.string.reasoning_in_progress)
                } else {
                    message.elapsedSeconds?.takeIf { it > 0 }?.let { seconds ->
                        pluralStringResource(
                            R.plurals.reasoning_completed_seconds,
                            seconds,
                            seconds,
                        )
                    } ?: stringResource(R.string.reasoning_completed)
                },
                style = MiuixTheme.textStyles.body2,
                color = if (message.isStreaming) {
                    MiuixTheme.colorScheme.onSurface
                } else {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                },
                modifier = Modifier.weight(1f, fill = !compact),
            )
            ExpandChevron(
                expanded = expanded,
                contentDescription = stringResource(
                    if (expanded) R.string.reasoning_collapse else R.string.reasoning_expand,
                ),
                modifier = Modifier.padding(start = 4.dp, end = if (compact) 8.dp else 0.dp),
            )
        }

        AnimatedVisibility(visible = expanded && message.content.isNotBlank()) {
            Column {
                if (!compact) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .height(0.5.dp)
                            .background(MiuixTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    )
                }
                val contentModifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (compact) 0.dp else 12.dp,
                        end = if (compact) 8.dp else 12.dp,
                        top = if (compact) 0.dp else 8.dp,
                        bottom = if (compact) 8.dp else 12.dp,
                    )
                if (streamingState != null &&
                    (message.isStreaming || completedDocument == null || tailWindow)
                ) {
                    ThinkingTailWindow(
                        enabled = tailWindow,
                        modifier = contentModifier,
                    ) {
                        StreamingMarkdown(
                            state = streamingState,
                            content = message.content,
                            isStreaming = message.isStreaming,
                            tone = MarkdownTone.Thinking,
                        )
                    }
                } else {
                    StaticMarkdown(
                        content = message.content,
                        tone = MarkdownTone.Thinking,
                        parsed = completedDocument,
                        modifier = contentModifier,
                    )
                }
            }
        }
    }
}

/**
 * 进行中的思考只露出最近几行：内容按底部对齐超出部分向上裁掉，顶部渐隐提示还有更早内容。
 * 高度封顶后增量不再推高工作过程卡片，列表也不必随每次思考快照重新跟底。
 */
@Composable
private fun ThinkingTailWindow(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        Box(modifier) { content() }
        return
    }
    val maxHeight = with(LocalDensity.current) {
        (THINKING_TAIL_WINDOW_LINES * THINKING_LINE_HEIGHT_SP).sp.toDp()
    }
    Box(
        modifier = modifier
            .heightIn(max = maxHeight)
            .clipToBounds()
            // 离屏合成只覆盖这块有界区域，用于让渐隐遮罩作用在文字 alpha 上。
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                if (size.height >= maxHeight.toPx() - 1f) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.35f to Color.Black,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.Bottom, unbounded = true),
        ) {
            content()
        }
    }
}

private const val THINKING_TAIL_WINDOW_LINES = 4

// ── Run trace：轻量入口行 ─────────────────────────────────────────────

@Composable
private fun RunTraceRow(
    message: RunTraceMessageUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val aiCardCorner = AppRadius.card
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .squircleSurface(color = MiuixTheme.colorScheme.surface, cornerRadius = aiCardCorner)
            .squircleBorder(
                width = 1.dp,
                color = MiuixTheme.colorScheme.outline.copy(alpha = ChatCardOutlineAlpha),
                cornerRadius = aiCardCorner,
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MiuixTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.ui_available_capacity_743337),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.7f),
        )
    }
}

// ── 工具摘要 ──────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToolSummaryInline(
    message: ToolSummaryMessageUi,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (compact) 10.dp else 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        message.tools.forEach { tool ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MiuixTheme.colorScheme.surface)
                    .border(
                        0.5.dp,
                        MiuixTheme.colorScheme.outline.copy(alpha = 0.5f),
                        RoundedCornerShape(10.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = iconForTool(tool),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MiuixTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = toolDisplayName(tool),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
    }
}

// ── 建议语 ────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionChipsRow(
    message: SuggestionChipsMessageUi,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        message.prompts.forEach { prompt ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MiuixTheme.colorScheme.surface)
                    .border(
                        0.5.dp,
                        MiuixTheme.colorScheme.outline.copy(alpha = 0.55f),
                        RoundedCornerShape(10.dp),
                    )
                    .clickable { onSuggestionClick(prompt) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MiuixTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = prompt,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
