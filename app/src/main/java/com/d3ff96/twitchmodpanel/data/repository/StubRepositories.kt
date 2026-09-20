package com.d3ff96.twitchmodpanel.data.repository

import android.content.Context
import com.d3ff96.twitchmodpanel.data.local.QuickCommandDataStore
import com.d3ff96.twitchmodpanel.data.oauth.TwitchOAuthConfig
import com.d3ff96.twitchmodpanel.data.remote.ChatStub
import com.d3ff96.twitchmodpanel.data.remote.HelixApiStub
import com.d3ff96.twitchmodpanel.data.remote.SevenTvApiStub
import com.d3ff96.twitchmodpanel.domain.model.AuthState
import com.d3ff96.twitchmodpanel.domain.model.Channel
import com.d3ff96.twitchmodpanel.domain.model.ChatMessage
import com.d3ff96.twitchmodpanel.domain.model.Emote
import com.d3ff96.twitchmodpanel.domain.model.ModAction
import com.d3ff96.twitchmodpanel.domain.model.ModActionType
import com.d3ff96.twitchmodpanel.domain.model.QuickCommand
import com.d3ff96.twitchmodpanel.domain.model.TwitchUser
import com.d3ff96.twitchmodpanel.domain.repository.AuthRepository
import com.d3ff96.twitchmodpanel.domain.repository.ChannelRepository
import com.d3ff96.twitchmodpanel.domain.repository.ChatRepository
import com.d3ff96.twitchmodpanel.domain.repository.EmoteRepository
import com.d3ff96.twitchmodpanel.domain.repository.QuickCommandRepository
import com.d3ff96.twitchmodpanel.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.UUID

class StubAuthRepository : AuthRepository {
    private val _state = MutableStateFlow(
        AuthState(clientIdConfigured = TwitchOAuthConfig.isClientIdConfigured())
    )
    override val authState: Flow<AuthState> = _state.asStateFlow()

    private val _device = MutableStateFlow<com.d3ff96.twitchmodpanel.domain.model.DeviceAuthSession?>(null)
    override val deviceAuthSession = _device.asStateFlow()

    private val _pendingError = MutableStateFlow<String?>(null)
    override val pendingError = _pendingError.asStateFlow()

    override fun clearPendingError() {
        _pendingError.value = null
    }

    override suspend fun startOAuth(): Result<Unit> {
        if (!isClientIdConfigured()) {
            return Result.failure(
                IllegalStateException(
                    "TWITCH_CLIENT_ID не задан. Скопируйте local.properties.example → local.properties"
                )
            )
        }
        // Prefer TwitchAuthRepository for real Device Code flow.
        return Result.failure(
            UnsupportedOperationException("Используйте TwitchAuthRepository (Device Code)")
        )
    }

    override suspend fun cancelDeviceAuth() {
        _device.value = null
    }

    override suspend fun handleOAuthRedirect(uri: String): Result<Unit> {
        _state.value = AuthState(
            isAuthenticated = true,
            accessToken = "stub_token_from_redirect",
            user = TwitchUser("10001", "stub_mod", "StubMod"),
            clientIdConfigured = isClientIdConfigured(),
        )
        return Result.success(Unit)
    }

    override suspend fun signInAsStub(): Result<Unit> {
        _state.value = AuthState(
            isAuthenticated = true,
            accessToken = "stub_offline_token",
            user = TwitchUser("10001", "stub_mod", "StubMod"),
            clientIdConfigured = isClientIdConfigured(),
        )
        return Result.success(Unit)
    }

    override suspend fun signOut() {
        _state.value = AuthState(clientIdConfigured = isClientIdConfigured())
    }

    override fun isClientIdConfigured(): Boolean = TwitchOAuthConfig.isClientIdConfigured()
}


class StubChannelRepository(
    private val helix: HelixApiStub = HelixApiStub(),
) : ChannelRepository {
    override suspend fun getModeratedChannels(): Result<List<Channel>> =
        runCatching { helix.getModeratedChannels() }

    override suspend fun getStreamStatus(channelId: String): Result<Channel> =
        runCatching { helix.getStreamStatus(channelId) }
}

class StubChatRepository(
    private val chat: ChatStub = ChatStub(),
) : ChatRepository {
    override fun observeMessages(channelId: String): Flow<List<ChatMessage>> = chat.messages

    override suspend fun sendMessage(channelId: String, text: String): Result<Unit> =
        runCatching {
            chat.sendLocal(channelId, text)
        }

    override suspend fun performModAction(action: ModAction): Result<Unit> =
        runCatching {
            val login = action.targetLogin
            when (action.type) {
                ModActionType.DELETE -> {
                    action.messageId?.let { chat.markDeleted(it) }
                    chat.appendSystemNotice(
                        action.channelId,
                        "[Mod] Удалено сообщение от @$login",
                    )
                }
                ModActionType.TIMEOUT -> {
                    val mins = ((action.durationSeconds ?: 600) / 60).coerceAtLeast(1)
                    chat.appendSystemNotice(
                        action.channelId,
                        "[Mod] Timeout @$login ($mins мин)",
                    )
                }
                ModActionType.BAN -> {
                    chat.muteUser(action.targetUserId)
                    chat.appendSystemNotice(
                        action.channelId,
                        "[Mod] Ban @$login",
                    )
                }
                ModActionType.UNBAN -> {
                    chat.unmuteUser(action.targetUserId)
                    chat.appendSystemNotice(
                        action.channelId,
                        "[Mod] Unban @$login",
                    )
                }
            }
            Unit
        }

    override suspend fun connect(channelId: String): Result<Unit> =
        runCatching { chat.connect(channelId) }

    override suspend fun disconnect() {
        chat.disconnect()
    }
}

class StubEmoteRepository(
    private val sevenTv: SevenTvApiStub = SevenTvApiStub(),
) : EmoteRepository {
    override suspend fun getChannelEmotes(channelId: String): Result<List<Emote>> =
        runCatching { sevenTv.getChannelEmotes(channelId) }

    override suspend fun getGlobalEmotes(): Result<List<Emote>> =
        runCatching { sevenTv.getGlobalEmotes() }
}

class DataStoreQuickCommandRepository(
    context: Context,
) : QuickCommandRepository {
    private val store = QuickCommandDataStore(context)
    private var seeded = false

    override fun observeCommands(): Flow<List<QuickCommand>> = store.commands

    override suspend fun upsert(command: QuickCommand): Result<Unit> = runCatching {
        ensureSeeded()
        val current = store.commands.first().ifEmpty { QuickCommandDataStore.defaultCommands() }
        val next = current.filterNot { it.id == command.id } + command
        store.save(next.sortedBy { it.name.lowercase() })
    }

    override suspend fun delete(id: String): Result<Unit> = runCatching {
        ensureSeeded()
        val current = store.commands.first()
        store.save(current.filterNot { it.id == id })
    }

    private suspend fun ensureSeeded() {
        if (seeded) return
        val existing = store.commands.first()
        if (existing.isEmpty()) {
            store.save(QuickCommandDataStore.defaultCommands())
        }
        seeded = true
    }
}

class StubUserRepository(
    private val helix: HelixApiStub = HelixApiStub(),
) : UserRepository {
    override suspend fun getCurrentUser(): Result<TwitchUser> =
        runCatching { helix.getCurrentUser() }
}

/** Simple service locator for MVP (no DI framework). */
object AppContainer {
    lateinit var authRepository: AuthRepository
        private set
    lateinit var channelRepository: ChannelRepository
        private set
    lateinit var chatRepository: ChatRepository
        private set
    lateinit var emoteRepository: EmoteRepository
        private set
    lateinit var quickCommandRepository: QuickCommandRepository
        private set
    lateinit var userRepository: UserRepository
        private set

    fun init(context: Context) {
        authRepository = TwitchAuthRepository(context.applicationContext)
        channelRepository = StubChannelRepository()
        chatRepository = StubChatRepository()
        emoteRepository = StubEmoteRepository()
        quickCommandRepository = DataStoreQuickCommandRepository(context.applicationContext)
        userRepository = StubUserRepository()
        // Defaults are written lazily on first upsert/delete; UI also falls back via
        // DataStore parse() returning defaultCommands() when JSON is empty.
    }

    fun newQuickCommand(name: String, body: String) =
        QuickCommand(id = UUID.randomUUID().toString(), name = name, body = body)
}
