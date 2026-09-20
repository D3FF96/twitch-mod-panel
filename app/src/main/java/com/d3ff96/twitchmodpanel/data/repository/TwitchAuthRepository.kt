package com.d3ff96.twitchmodpanel.data.repository

import android.content.Context
import com.d3ff96.twitchmodpanel.data.local.SecureTokenStore
import com.d3ff96.twitchmodpanel.data.oauth.DeviceTokenPollResult
import com.d3ff96.twitchmodpanel.data.oauth.TwitchDeviceCodeClient
import com.d3ff96.twitchmodpanel.data.oauth.TwitchOAuthConfig
import com.d3ff96.twitchmodpanel.domain.model.AuthState
import com.d3ff96.twitchmodpanel.domain.model.DeviceAuthSession
import com.d3ff96.twitchmodpanel.domain.model.TwitchUser
import com.d3ff96.twitchmodpanel.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Real Twitch Device Code OAuth + stub sign-in for demo UI without Client ID.
 */
class TwitchAuthRepository(
    context: Context,
    private val client: TwitchDeviceCodeClient = TwitchDeviceCodeClient(),
    private val tokenStore: SecureTokenStore = SecureTokenStore(context),
) : AuthRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(
        AuthState(clientIdConfigured = TwitchOAuthConfig.isClientIdConfigured()),
    )
    override val authState: Flow<AuthState> = _state.asStateFlow()

    private val _deviceSession = MutableStateFlow<DeviceAuthSession?>(null)
    override val deviceAuthSession: Flow<DeviceAuthSession?> = _deviceSession.asStateFlow()

    private val _pendingError = MutableStateFlow<String?>(null)
    override val pendingError: Flow<String?> = _pendingError.asStateFlow()

    private var pollJob: Job? = null

    init {
        scope.launch {
            val access = tokenStore.accessToken ?: return@launch
            if (!TwitchOAuthConfig.isClientIdConfigured()) return@launch
            runCatching {
                withContext(Dispatchers.IO) {
                    client.validateToken(access)
                    client.fetchCurrentUser(TwitchOAuthConfig.clientId, access)
                }
            }.onSuccess { user ->
                _state.value = AuthState(
                    isAuthenticated = true,
                    accessToken = access,
                    user = TwitchUser(user.id, user.login, user.displayName, user.profileImageUrl),
                    clientIdConfigured = true,
                )
            }.onFailure {
                tokenStore.clear()
            }
        }
    }

    override fun isClientIdConfigured(): Boolean = TwitchOAuthConfig.isClientIdConfigured()

    override fun clearPendingError() {
        _pendingError.value = null
    }

    override suspend fun startOAuth(): Result<Unit> {
        if (!isClientIdConfigured()) {
            return Result.failure(
                IllegalStateException(
                    "TWITCH_CLIENT_ID не задан. Скопируйте local.properties.example → local.properties",
                ),
            )
        }
        cancelDeviceAuth()
        return try {
            val device = withContext(Dispatchers.IO) {
                client.requestDeviceCode(
                    clientId = TwitchOAuthConfig.clientId,
                    scopes = TwitchOAuthConfig.scopesFormValue(),
                )
            }
            _deviceSession.value = DeviceAuthSession(
                userCode = device.userCode,
                verificationUri = device.verificationUri,
                expiresInSeconds = device.expiresIn,
                intervalSeconds = device.interval,
                statusText = "Откройте Twitch и введите код",
            )
            startPolling(device.deviceCode, device.interval, device.expiresIn)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                Exception("Сетевая ошибка при запросе кода: ${e.message ?: "нет соединения"}"),
            )
        }
    }

    private fun startPolling(deviceCode: String, initialInterval: Int, expiresIn: Int) {
        pollJob?.cancel()
        pollJob = scope.launch(Dispatchers.IO) {
            var intervalSec = initialInterval.coerceAtLeast(1)
            val deadline = System.currentTimeMillis() + expiresIn * 1000L
            while (isActive) {
                if (System.currentTimeMillis() >= deadline) {
                    publishDeviceError("Код устройства истёк. Запросите новый.")
                    return@launch
                }
                delay(intervalSec * 1000L)
                if (!isActive) return@launch

                when (
                    val result = client.pollToken(
                        clientId = TwitchOAuthConfig.clientId,
                        scopes = TwitchOAuthConfig.scopesFormValue(),
                        deviceCode = deviceCode,
                    )
                ) {
                    is DeviceTokenPollResult.AuthorizationPending -> {
                        _deviceSession.value = _deviceSession.value?.copy(
                            statusText = "Ожидание подтверждения на Twitch…",
                        )
                    }
                    is DeviceTokenPollResult.SlowDown -> {
                        intervalSec += 5
                        _deviceSession.value = _deviceSession.value?.copy(
                            intervalSeconds = intervalSec,
                            statusText = "Слишком часто — ждём ${intervalSec}с…",
                        )
                    }
                    is DeviceTokenPollResult.Success -> {
                        val tokens = result.tokens
                        runCatching { client.validateToken(tokens.accessToken) }
                        val userDto = try {
                            client.fetchCurrentUser(TwitchOAuthConfig.clientId, tokens.accessToken)
                        } catch (e: Exception) {
                            publishDeviceError(
                                "Токен получен, но профиль недоступен: ${e.message ?: "ошибка Helix"}",
                            )
                            return@launch
                        }
                        tokenStore.saveTokens(tokens.accessToken, tokens.refreshToken)
                        _deviceSession.value = null
                        _state.value = AuthState(
                            isAuthenticated = true,
                            accessToken = tokens.accessToken,
                            user = TwitchUser(
                                id = userDto.id,
                                login = userDto.login,
                                displayName = userDto.displayName,
                                profileImageUrl = userDto.profileImageUrl,
                            ),
                            clientIdConfigured = true,
                        )
                        return@launch
                    }
                    is DeviceTokenPollResult.Denied -> {
                        publishDeviceError(result.message)
                        return@launch
                    }
                    is DeviceTokenPollResult.Expired -> {
                        publishDeviceError(result.message)
                        return@launch
                    }
                    is DeviceTokenPollResult.Error -> {
                        publishDeviceError(result.message)
                        return@launch
                    }
                }
            }
        }
    }

    private fun publishDeviceError(message: String) {
        _deviceSession.value = null
        _pendingError.value = message
    }

    override suspend fun cancelDeviceAuth() {
        pollJob?.cancel()
        pollJob = null
        _deviceSession.value = null
    }

    override suspend fun handleOAuthRedirect(uri: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Используйте Device Code flow"))
    }

    override suspend fun signInAsStub(): Result<Unit> {
        cancelDeviceAuth()
        _state.value = AuthState(
            isAuthenticated = true,
            accessToken = "stub_offline_token",
            user = TwitchUser("10001", "stub_mod", "StubMod"),
            clientIdConfigured = isClientIdConfigured(),
        )
        return Result.success(Unit)
    }

    override suspend fun signOut() {
        cancelDeviceAuth()
        tokenStore.clear()
        _state.value = AuthState(clientIdConfigured = isClientIdConfigured())
    }
}
