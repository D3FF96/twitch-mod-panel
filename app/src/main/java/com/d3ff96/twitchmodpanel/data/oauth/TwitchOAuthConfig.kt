package com.d3ff96.twitchmodpanel.data.oauth

import com.d3ff96.twitchmodpanel.BuildConfig
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Twitch OAuth 2.0 configuration.
 *
 * Primary login path: **Device Code Grant** (no custom redirect required for login).
 * Authorization Code + redirect URI remain available for future AppAuth / Custom Tabs.
 *
 * Client ID comes from BuildConfig (`local.properties` → `TWITCH_CLIENT_ID`).
 * Never put Client Secret in the app.
 */
object TwitchOAuthConfig {
    const val AUTHORIZATION_ENDPOINT = "https://id.twitch.tv/oauth2/authorize"
    const val TOKEN_ENDPOINT = "https://id.twitch.tv/oauth2/token"
    const val DEVICE_ENDPOINT = "https://id.twitch.tv/oauth2/device"
    const val VALIDATE_ENDPOINT = "https://id.twitch.tv/oauth2/validate"
    const val REVOKE_ENDPOINT = "https://id.twitch.tv/oauth2/revoke"
    const val HELIX_USERS = "https://api.twitch.tv/helix/users"

    /** RFC 8628 device_code grant type. */
    const val DEVICE_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code"

    val clientId: String get() = BuildConfig.TWITCH_CLIENT_ID
    val redirectUri: String get() = BuildConfig.OAUTH_REDIRECT_URI

    val scopes: List<String> = listOf(
        "openid",
        "user:read:email",
        "chat:read",
        "chat:edit",
        "moderator:manage:banned_users",
        "moderator:manage:chat_messages",
        "moderator:read:followers",
        "user:read:moderated_channels",
    )

    /** Space-delimited scopes as Twitch form bodies expect. */
    fun scopesFormValue(): String = scopes.joinToString(" ")

    /** URL-encoded space-delimited scopes (if a caller needs query-string form). */
    fun scopesUrlEncoded(): String =
        URLEncoder.encode(scopesFormValue(), StandardCharsets.UTF_8.name())

    fun isClientIdConfigured(): Boolean =
        clientId.isNotBlank() && clientId != "your_twitch_client_id_here"
}
