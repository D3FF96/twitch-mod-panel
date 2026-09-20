package com.d3ff96.twitchmodpanel.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d3ff96.twitchmodpanel.data.repository.AppContainer
import com.d3ff96.twitchmodpanel.domain.model.AuthState
import com.d3ff96.twitchmodpanel.domain.model.DeviceAuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val auth: AuthState = AuthState(),
    val deviceSession: DeviceAuthSession? = null,
    val message: String? = null,
    val isLoading: Boolean = false,
)

class LoginViewModel : ViewModel() {
    private val auth = AppContainer.authRepository

    private val _ui = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _ui.asStateFlow()

    val authState: StateFlow<AuthState> = auth.authState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AuthState(clientIdConfigured = auth.isClientIdConfigured()),
    )

    init {
        viewModelScope.launch {
            auth.authState.collect { a ->
                _ui.update { it.copy(auth = a, isLoading = false) }
            }
        }
        viewModelScope.launch {
            auth.deviceAuthSession.collect { session ->
                _ui.update {
                    it.copy(
                        deviceSession = session,
                        isLoading = session == null && it.isLoading,
                    )
                }
            }
        }
        viewModelScope.launch {
            auth.pendingError.collect { err ->
                if (err != null) {
                    _ui.update { it.copy(message = err, isLoading = false, deviceSession = null) }
                    auth.clearPendingError()
                }
            }
        }
    }

    fun startOAuth() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, message = null) }
            auth.startOAuth()
                .onSuccess {
                    _ui.update { it.copy(isLoading = false) }
                }
                .onFailure { e ->
                    _ui.update {
                        it.copy(
                            isLoading = false,
                            message = e.message ?: "Не удалось начать вход",
                        )
                    }
                }
        }
    }

    fun cancelDeviceAuth() {
        viewModelScope.launch {
            auth.cancelDeviceAuth()
            _ui.update { it.copy(deviceSession = null, isLoading = false) }
        }
    }

    fun continueAsStub() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true) }
            auth.cancelDeviceAuth()
            auth.signInAsStub()
            _ui.update { it.copy(isLoading = false) }
        }
    }

    fun clearMessage() {
        _ui.update { it.copy(message = null) }
    }
}
