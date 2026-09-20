package com.d3ff96.twitchmodpanel.domain.model

data class TwitchUser(
    val id: String,
    val login: String,
    val displayName: String,
    val profileImageUrl: String? = null,
)

data class Channel(
    val id: String,
    val login: String,
    val displayName: String,
    val isLive: Boolean = false,
    val title: String = "",
    val viewerCount: Int = 0,
    val gameName: String = "",
)

data class ChatMessage(
    val id: String,
    val channelId: String,
    val userId: String,
    val userLogin: String,
    val displayName: String,
    val text: String,
    val timestampMs: Long,
    val badges: List<String> = emptyList(),
    val color: String? = null,
    val isDeleted: Boolean = false,
    val isModerator: Boolean = false,
    val isSubscriber: Boolean = false,
)

data class Emote(
    val id: String,
    val name: String,
    val url: String,
    val source: EmoteSource,
)

enum class EmoteSource { TWITCH, SEVENTV, BTTV, FFZ }

data class QuickCommand(
    val id: String,
    val name: String,
    val body: String,
)

enum class ModActionType { TIMEOUT, BAN, DELETE, UNBAN }

data class ModAction(
    val type: ModActionType,
    val channelId: String,
    val targetUserId: String,
    val targetLogin: String,
    val messageId: String? = null,
    val durationSeconds: Int? = null,
    val reason: String? = null,
)

data class AuthState(
    val isAuthenticated: Boolean = false,
    val accessToken: String? = null,
    val user: TwitchUser? = null,
    val clientIdConfigured: Boolean = false,
)

/** In-progress Device Code Grant UI state (null when idle). */
data class DeviceAuthSession(
    val userCode: String,
    val verificationUri: String,
    val expiresInSeconds: Int,
    val intervalSeconds: Int,
    val statusText: String = "Ожидание подтверждения на Twitch…",
)
