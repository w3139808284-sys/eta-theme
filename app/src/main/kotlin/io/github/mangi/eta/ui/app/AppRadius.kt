package io.github.mangi.eta.ui.app

import androidx.compose.ui.unit.dp

/**
 * 统一圆角常量：所有布局圆角必须引用此处的值，禁止在调用处散写数值。
 *
 * 用户气泡圆角（20）与 AI 卡片圆角（14）保持不同，这是归属区分而非一致性目标。
 */
object AppRadius {
    /** 输入框圆角。 */
    val bubble = 20.dp

    /** 思考卡片、运行轨迹、AI 正文容器圆角。 */
    val card = 14.dp

    /** 气泡尾巴角；本轮仅定义，不接线。 */
    val tail = 6.dp
}
