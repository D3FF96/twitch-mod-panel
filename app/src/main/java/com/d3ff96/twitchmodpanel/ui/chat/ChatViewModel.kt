package com.d3ff96.twitchmodpanel.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.d3ff96.twitchmodpanel.data.repository.AppContainer
import com.d3ff96.twitchmodpanel.domain.model.ChatMessage
import com.d3ff96.twitchmodpanel.domain.model.Emote
import com.d3ff96.twitchmodpanel.domain.model.ModAction
import com.d3ff96.twitchmodpanel.domain.model.ModActionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val channelId: String,
    val channelLogin: String,
    val displayName: String,
    val isLive: Boolean,
    val draft: String = "",
    val emotes: List<Emote> = emptyList(),
    val statusMessage: String? = null,
    val selectedMessage: ChatMessage? = null,
    /** Login of user being replied to; composer is prefixed with `@login `. */
    val replyToLogin: String? = null,
    val profileMessage: ChatMessage? = null,
    val streamExpanded: Boolean = false,
    val streamAudioEnabled: Boolean = true,
    /** Request composer focus after starting a reply. */
    val requestComposerFocus: Boolean = false,
)

class ChatViewModel(
    channelId: String,
    channelLogin: String,
    displayName: String,
    isLive: Boolean,
) : ViewModel() {
    private val chat = AppContainer.chatRepository
    private val emotes = AppContainer.emoteRepository

    private val _ui = MutableStateFlow(
        ChatUiState(channelId, channelLogin, displayName, isLive)
    )
    val uiState: StateFlow<ChatUiState> = _ui.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = chat.observeMessages(channelId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            chat.connect(channelId)
            val global = emotes.getGlobalEmotes().getOrDefault(emptyList())
            val channel = emotes.getChannelEmotes(channelId).getOrDefault(emptyList())
            _ui.update { it.copy(emotes = global + channel) }
        }
    }

    fun onDraftChange(value: String) {
        _ui.update { state ->
            val reply = state.replyToLogin
            if (reply != null) {
                val prefix = "@$reply "
                if (!value.startsWith(prefix) && !value.startsWith("@$reply")) {
                    // User cleared the mention — drop reply mode
                    state.copy(draft = value, replyToLogin = null)
                } else {
                    state.copy(draft = value)
                }
            } else {
                state.copy(draft = value)
            }
        }
    }

    fun send() {
        val text = _ui.value.draft.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            chat.sendMessage(_ui.value.channelId, text)
            _ui.update { it.copy(draft = "", replyToLogin = null) }
        }
    }

    /** Sends a quick-command body immediately as StubMod (not just draft). */
    fun sendQuickReply(body: String) {
        val text = body.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            chat.sendMessage(_ui.value.channelId, text)
        }
    }

    fun selectMessage(msg: ChatMessage?) {
        _ui.update { it.copy(selectedMessage = msg) }
    }

    fun openProfile(msg: ChatMessage) {
        _ui.update { it.copy(profileMessage = msg) }
    }

    fun dismissProfile() {
        _ui.update { it.copy(profileMessage = null) }
    }

    /**
     * Prefills composer with `@userLogin ` (Twitch-style mention) and focuses the field.
     * Replaces any previous reply mention prefix.
     */
    fun startReply(msg: ChatMessage) {
        val login = msg.userLogin
        val prefix = "@$login "
        _ui.update { state ->
            val withoutOld = stripReplyPrefix(state.draft, state.replyToLogin)
            state.copy(
                replyToLogin = login,
                draft = prefix + withoutOld.trimStart(),
                requestComposerFocus = true,
                selectedMessage = null,
                profileMessage = null,
            )
        }
    }

    fun clearReply() {
        _ui.update { state ->
            val cleaned = stripReplyPrefix(state.draft, state.replyToLogin)
            state.copy(replyToLogin = null, draft = cleaned.trimStart())
        }
    }

    fun consumeComposerFocusRequest() {
        _ui.update { it.copy(requestComposerFocus = false) }
    }

    fun setStreamExpanded(expanded: Boolean) {
        _ui.update { it.copy(streamExpanded = expanded) }
    }

    fun toggleStreamExpanded() {
        _ui.update { it.copy(streamExpanded = !it.streamExpanded) }
    }

    fun setStreamAudioEnabled(enabled: Boolean) {
        _ui.update { it.copy(streamAudioEnabled = enabled) }
    }

    fun toggleStreamAudio() {
        _ui.update { it.copy(streamAudioEnabled = !it.streamAudioEnabled) }
    }

    fun modAction(type: ModActionType, durationSeconds: Int? = 600, fromMessage: ChatMessage? = null) {
        val msg = fromMessage ?: _ui.value.selectedMessage ?: return
        viewModelScope.launch {
            val action = ModAction(
                type = type,
                channelId = _ui.value.channelId,
                targetUserId = msg.userId,
                targetLogin = msg.userLogin,
                messageId = msg.id,
                durationSeconds = if (type == ModActionType.TIMEOUT) durationSeconds else null,
            )
            chat.performModAction(action)
                .onSuccess {
                    val status = when (type) {
                        ModActionType.TIMEOUT -> {
                            val mins = ((durationSeconds ?: 600) / 60).coerceAtLeast(1)
                            "Timeout @${msg.userLogin} ($mins мин)"
                        }
                        ModActionType.BAN -> "Ban @${msg.userLogin}"
                        ModActionType.UNBAN -> "Unban @${msg.userLogin}"
                        ModActionType.DELETE -> "Удалено сообщение от @${msg.userLogin}"
                    }
                    _ui.update {
                        it.copy(
                            selectedMessage = null,
                            profileMessage = null,
                            statusMessage = status,
                        )
                    }
                }
                .onFailure { e ->
                    _ui.update { it.copy(statusMessage = e.message) }
                }
        }
    }

    fun applyQuickReply(body: String) {
        _ui.update { it.copy(draft = body) }
    }

    fun clearStatus() {
        _ui.update { it.copy(statusMessage = null) }
    }

    override fun onCleared() {
        viewModelScope.launch { chat.disconnect() }
        super.onCleared()
    }

    companion object {
        private fun stripReplyPrefix(draft: String, replyLogin: String?): String {
            if (replyLogin == null) return draft
            val prefix = "@$replyLogin "
            return when {
                draft.startsWith(prefix) -> draft.removePrefix(prefix)
                draft.startsWith("@$replyLogin") -> draft.removePrefix("@$replyLogin").trimStart()
                else -> draft
            }
        }

        fun factory(
            channelId: String,
            login: String,
            displayName: String,
            isLive: Boolean,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(channelId, login, displayName, isLive) as T
        }
    }
}
