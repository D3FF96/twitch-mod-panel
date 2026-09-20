package com.d3ff96.twitchmodpanel.domain.repository

import com.d3ff96.twitchmodpanel.domain.model.AuthState
import com.d3ff96.twitchmodpanel.domain.model.Channel
import com.d3ff96.twitchmodpanel.domain.model.ChatMessage
import com.d3ff96.twitchmodpanel.domain.model.DeviceAuthSession
import com.d3ff96.twitchmodpanel.domain.model.Emote
import com.d3ff96.twitchmodpanel.domain.model.ModAction
import com.d3ff96.twitchmodpanel.domain.model.QuickCommand
import com.d3ff96.twitchmodpanel.domain.model.TwitchUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthState>
    /** Active Device Code session while polling; null when idle. */
    val deviceAuthSession: Flow<DeviceAuthSession?>

    /** One-shot Russian error message from device polling (denied / expired / network). */
    val pendingError: Flow<String?>

    /**
     * Starts Twitch Device Code Grant when Client ID is configured.
     * On success, [deviceAuthSession] is updated and polling begins until
     * tokens are obtained, cancelled, denied, or expired.
     */
    suspend fun startOAuth(): Result<Unit>

    /** Stops device-code polling and clears [deviceAuthSession]. */
    suspend fun cancelDeviceAuth()

    fun clearPendingError()

    /** Legacy redirect handler (Authorization Code); unused by Device Code flow. */
    suspend fun handleOAuthRedirect(uri: String): Result<Unit>

    /** Stub path: mark user as signed-in without real tokens (demo UI). */
    suspend fun signInAsStub(): Result<Unit>

    suspend fun signOut()
    fun isClientIdConfigured(): Boolean
}

interface ChannelRepository {
    suspend fun getModeratedChannels(): Result<List<Channel>>
    suspend fun getStreamStatus(channelId: String): Result<Channel>
}

interface ChatRepository {
    fun observeMessages(channelId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(channelId: String, text: String): Result<Unit>
    suspend fun performModAction(action: ModAction): Result<Unit>
    suspend fun connect(channelId: String): Result<Unit>
    suspend fun disconnect()
}

interface EmoteRepository {
    suspend fun getChannelEmotes(channelId: String): Result<List<Emote>>
    suspend fun getGlobalEmotes(): Result<List<Emote>>
}

interface QuickCommandRepository {
    fun observeCommands(): Flow<List<QuickCommand>>
    suspend fun upsert(command: QuickCommand): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
}

interface UserRepository {
    suspend fun getCurrentUser(): Result<TwitchUser>
}
