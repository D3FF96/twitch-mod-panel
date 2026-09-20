package com.d3ff96.twitchmodpanel.ui.channelpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d3ff96.twitchmodpanel.data.repository.AppContainer
import com.d3ff96.twitchmodpanel.domain.model.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChannelPickerUiState(
    val channels: List<Channel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class ChannelPickerViewModel : ViewModel() {
    private val repo = AppContainer.channelRepository
    private val _ui = MutableStateFlow(ChannelPickerUiState(isLoading = true))
    val uiState: StateFlow<ChannelPickerUiState> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }
            repo.getModeratedChannels()
                .onSuccess { list ->
                    _ui.update { it.copy(channels = list, isLoading = false) }
                }
                .onFailure { e ->
                    _ui.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}
