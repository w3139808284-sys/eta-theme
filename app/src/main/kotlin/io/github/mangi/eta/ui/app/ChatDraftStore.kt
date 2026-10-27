package io.github.mangi.eta.ui.app

import android.content.Context

/**
 * 按会话 ID 存的输入草稿。
 *
 * 输入框的文字在 Compose 里只活在本地 [androidx.compose.foundation.text.input.TextFieldState]，
 * 切会话或进程被回收后就没发出去的字会丢。这里补一层 SharedPreferences 落盘，
 * 切回来时按会话 ID 贴回输入框。
 */
internal object ChatDraftStore {
    private const val FILE = "eta_chat_drafts"
    private const val MAX_DRAFTS = 200

    fun load(context: Context, conversationId: String?): String {
        if (conversationId.isNullOrBlank()) return ""
        return runCatching { prefs(context).getString(conversationId, null) }.getOrNull().orEmpty()
    }

    fun save(context: Context, conversationId: String?, text: String) {
        if (conversationId.isNullOrBlank()) return
        runCatching {
            val prefs = prefs(context)
            if (text.isBlank()) {
                prefs.edit().remove(conversationId).apply()
            } else {
                prefs.edit().putString(conversationId, text).apply()
                val keys = prefs.all.keys.toList()
                if (keys.size > MAX_DRAFTS) {
                    keys.take(keys.size - MAX_DRAFTS).forEach { prefs.edit().remove(it).apply() }
                }
            }
        }
    }

    private fun prefs(context: Context) = context.applicationContext
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
