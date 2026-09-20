package com.d3ff96.twitchmodpanel.data.oauth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class DeviceCodeResponse(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresIn: Int,
    val interval: Int,
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String?,
    val expiresIn: Int?,
    val scope: String?,
    val tokenType: String?,
)

data class TwitchUserDto(
    val id: String,
    val login: String,
    val displayName: String,
    val profileImageUrl: String?,
)

sealed class DeviceTokenPollResult {
    data class Success(val tokens: TokenResponse) : DeviceTokenPollResult()
    data object AuthorizationPending : DeviceTokenPollResult()
    data object SlowDown : DeviceTokenPollResult()
    data class Denied(val message: String) : DeviceTokenPollResult()
    data class Expired(val message: String) : DeviceTokenPollResult()
    data class Error(val message: String) : DeviceTokenPollResult()
}

/**
 * OkHttp client for Twitch Device Code Grant + Helix user lookup.
 * All network work runs on [Dispatchers.IO].
 */
class TwitchDeviceCodeClient(
    private val http: OkHttpClient = defaultClient(),
) {
    suspend fun requestDeviceCode(clientId: String, scopes: String): DeviceCodeResponse =
        withContext(Dispatchers.IO) {
            val body = FormBody.Builder()
                .add("client_id", clientId)
                .add("scopes", scopes)
                .build()
            val request = Request.Builder()
                .url(TwitchOAuthConfig.DEVICE_ENDPOINT)
                .post(body)
                .header("Accept", "application/json")
                .build()
            http.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException(parseErrorMessage(raw, response.code, "не удалось получить код устройства"))
                }
                val json = JSONObject(raw)
                DeviceCodeResponse(
                    deviceCode = json.getString("device_code"),
                    userCode = json.getString("user_code"),
                    verificationUri = json.optString(
                        "verification_uri",
                        json.optString("verification_uri_complete", "https://www.twitch.tv/activate"),
                    ),
                    expiresIn = json.optInt("expires_in", 1800),
                    interval = json.optInt("interval", 5).coerceAtLeast(1),
                )
            }
        }

    suspend fun pollToken(
        clientId: String,
        scopes: String,
        deviceCode: String,
    ): DeviceTokenPollResult = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("client_id", clientId)
            .add("scopes", scopes)
            .add("device_code", deviceCode)
            .add("grant_type", TwitchOAuthConfig.DEVICE_GRANT_TYPE)
            .build()
        val request = Request.Builder()
            .url(TwitchOAuthConfig.TOKEN_ENDPOINT)
            .post(body)
            .header("Accept", "application/json")
            .build()
        try {
            http.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(raw)
                    return@withContext DeviceTokenPollResult.Success(
                        TokenResponse(
                            accessToken = json.getString("access_token"),
                            refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() },
                            expiresIn = if (json.has("expires_in")) json.getInt("expires_in") else null,
                            scope = json.optString("scope").takeIf { it.isNotBlank() },
                            tokenType = json.optString("token_type").takeIf { it.isNotBlank() },
                        ),
                    )
                }
                val json = runCatching { JSONObject(raw) }.getOrNull()
                // Twitch returns HTTP 400 with JSON message: authorization_pending | slow_down | …
                val message = (json?.optString("message").orEmpty()).lowercase()
                when {
                    message == "authorization_pending" ||
                        message.contains("authorization_pending") ->
                        DeviceTokenPollResult.AuthorizationPending

                    message == "slow_down" || message.contains("slow_down") ->
                        DeviceTokenPollResult.SlowDown

                    message == "access_denied" || message.contains("access_denied") ->
                        DeviceTokenPollResult.Denied("Авторизация отклонена пользователем")

                    message == "expired_token" || message.contains("expired") ->
                        DeviceTokenPollResult.Expired("Код устройства истёк. Запросите новый.")

                    else -> DeviceTokenPollResult.Error(
                        parseErrorMessage(raw, response.code, "ошибка обмена кода на токен"),
                    )
                }
            }
        } catch (e: IOException) {
            DeviceTokenPollResult.Error("Сетевая ошибка: ${e.message ?: "нет соединения"}")
        }
    }

    /** Optional token validation (Twitch validate endpoint). */
    suspend fun validateToken(accessToken: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(TwitchOAuthConfig.VALIDATE_ENDPOINT)
            .get()
            .header("Authorization", "OAuth $accessToken")
            .build()
        try {
            http.newCall(request).execute().use { it.isSuccessful }
        } catch (_: IOException) {
            false
        }
    }

    suspend fun fetchCurrentUser(clientId: String, accessToken: String): TwitchUserDto =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(TwitchOAuthConfig.HELIX_USERS)
                .get()
                .header("Authorization", "Bearer $accessToken")
                .header("Client-Id", clientId)
                .build()
            http.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException(parseErrorMessage(raw, response.code, "не удалось получить профиль"))
                }
                val data = JSONObject(raw).getJSONArray("data")
                if (data.length() == 0) {
                    throw IOException("Helix /users вернул пустой список")
                }
                val u = data.getJSONObject(0)
                TwitchUserDto(
                    id = u.getString("id"),
                    login = u.getString("login"),
                    displayName = u.optString("display_name", u.getString("login")),
                    profileImageUrl = u.optString("profile_image_url").takeIf { it.isNotBlank() },
                )
            }
        }

    private fun parseErrorMessage(raw: String, code: Int, fallback: String): String {
        val fromJson = runCatching {
            val j = JSONObject(raw)
            j.optString("message").ifBlank { j.optString("status") }
        }.getOrNull()
        return when {
            !fromJson.isNullOrBlank() -> fromJson
            raw.isNotBlank() -> "HTTP $code: ${raw.take(120)}"
            else -> "HTTP $code: $fallback"
        }
    }

    companion object {
        fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
    }
}
