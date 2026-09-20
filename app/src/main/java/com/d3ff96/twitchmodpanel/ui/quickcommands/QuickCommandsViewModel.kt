package com.d3ff96.twitchmodpanel.ui.quickcommands

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d3ff96.twitchmodpanel.data.repository.AppContainer
import com.d3ff96.twitchmodpanel.domain.model.QuickCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickCommandsUiState(
    val editing: QuickCommand? = null,
    val nameDraft: String = "",
    val bodyDraft: String = "",
    val showEditor: Boolean = false,
    val message: String? = null,
)

class QuickCommandsViewModel : ViewModel() {
    private val repo = AppContainer.quickCommandRepository

    val commands: StateFlow<List<QuickCommand>> = repo.observeCommands()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(QuickCommandsUiState())
    val uiState: StateFlow<QuickCommandsUiState> = _ui.asStateFlow()

    fun openCreate() {
        _ui.value = QuickCommandsUiState(showEditor = true)
    }

    fun openEdit(cmd: QuickCommand) {
        _ui.value = QuickCommandsUiState(
            editing = cmd,
            nameDraft = cmd.name,
            bodyDraft = cmd.body,
            showEditor = true,
        )
    }

    fun closeEditor() {
        _ui.value = QuickCommandsUiState()
    }

    fun onNameChange(v: String) {
        _ui.update { it.copy(nameDraft = v) }
    }

    fun onBodyChange(v: String) {
        _ui.update { it.copy(bodyDraft = v) }
    }

    fun save() {
        val s = _ui.value
        if (s.nameDraft.isBlank() || s.bodyDraft.isBlank()) return
        viewModelScope.launch {
            val cmd = s.editing?.copy(name = s.nameDraft.trim(), body = s.bodyDraft.trim())
                ?: AppContainer.newQuickCommand(s.nameDraft.trim(), s.bodyDraft.trim())
            repo.upsert(cmd)
            closeEditor()
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repo.delete(id)
        }
    }
}
