package io.github.mangi.eta.ui.app

import android.content.Context
import io.github.mangi.eta.data.db.EtaDatabase
import io.github.mangi.eta.ui.model.AgentChatHomeUiState
import io.github.mangi.eta.ui.model.UserMessageUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 草稿按会话落盘的状态层回归测试。
 *
 * 覆盖：启动时自动选中会话恢复草稿、切会话恢复、会话隔离、主动清空后不还原、无草稿会话首次打开为空。
 *
 * 限制：无法在纯 JVM/Robolectric 单测里复现「进程重启 → DB 加载 → Compose 输入框首帧快照流」的完整链路，
 * 因此这里聚焦修复点本身——`AgentAppState` 初始化/重载时按会话 ID 补齐落盘草稿到 `homeState.input`，
 * 而不是只测 `ChatDraftStore.load()`。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "en-rUS")
class AgentAppStateDraftTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        EtaDatabase.closeForTests()
        context.deleteDatabase("eta.db")
        context.getSharedPreferences("eta_chat_drafts", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    private fun conversation(id: String, input: String = ""): AgentChatHomeUiState =
        AgentChatHomeUiState(
            messages = listOf(UserMessageUi(id = "$id-user", content = "message-$id")),
            input = input,
            isStreaming = false,
            thinkingEnabled = false,
        )

    private fun snapshot(
        selectedId: String?,
        conversations: Map<String, AgentChatHomeUiState>,
    ): AgentConversationStore.Snapshot = AgentConversationStore.Snapshot(
        selectedConversationId = selectedId,
        conversationsById = conversations,
        titles = conversations.keys.associateWith { id -> "title-$id" },
        updatedAt = conversations.keys.associateWith { id -> 1L },
    )

    @Test
    fun startupRestoresDraftForAutoSelectedConversation() {
        ChatDraftStore.save(context, "conv-draft", "重启前的草稿")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            val state = AgentAppState(
                context, scope,
                snapshot("conv-draft", mapOf("conv-draft" to conversation("conv-draft"))),
            )
            assertEquals("重启前的草稿", state.homeState.input)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun switchingConversationRestoresItsDraft() {
        ChatDraftStore.save(context, "conv-b", "会话B草稿")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            val state = AgentAppState(
                context, scope,
                snapshot("conv-a", mapOf("conv-a" to conversation("conv-a"), "conv-b" to conversation("conv-b"))),
            )
            assertEquals("", state.homeState.input)
            state.selectConversation("conv-b")
            assertEquals("会话B草稿", state.homeState.input)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun draftsAreIsolatedPerConversation() {
        ChatDraftStore.save(context, "conv-a", "会话A草稿")
        ChatDraftStore.save(context, "conv-b", "会话B草稿")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            val state = AgentAppState(
                context, scope,
                snapshot("conv-a", mapOf("conv-a" to conversation("conv-a"), "conv-b" to conversation("conv-b"))),
            )
            assertEquals("会话A草稿", state.homeState.input)
            state.selectConversation("conv-b")
            assertEquals("会话B草稿", state.homeState.input)
            state.selectConversation("conv-a")
            assertEquals("会话A草稿", state.homeState.input)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun clearingDraftKeepsInputEmptyAfterRestart() {
        ChatDraftStore.save(context, "conv-a", "已清空的草稿")
        ChatDraftStore.save(context, "conv-a", "") // 用户主动清空 → 落盘删除
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            val state = AgentAppState(
                context, scope,
                snapshot("conv-a", mapOf("conv-a" to conversation("conv-a"))),
            )
            assertEquals("", state.homeState.input)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun conversationWithoutDraftStartsEmpty() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            val state = AgentAppState(
                context, scope,
                snapshot("conv-a", mapOf("conv-a" to conversation("conv-a"))),
            )
            assertEquals("", state.homeState.input)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun existingNonBlankInputIsNotOverwrittenByDraft() {
        ChatDraftStore.save(context, "conv-a", "旧草稿")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            val state = AgentAppState(
                context, scope,
                snapshot("conv-a", mapOf("conv-a" to conversation("conv-a", input = "当前输入"))),
            )
            // 内存里已有非空输入时，不应被落盘草稿覆盖。
            assertEquals("当前输入", state.homeState.input)
        } finally {
            scope.cancel()
        }
    }
}
