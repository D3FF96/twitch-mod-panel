package com.d3ff96.twitchmodpanel.ui.quickcommands

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d3ff96.twitchmodpanel.R
import com.d3ff96.twitchmodpanel.domain.model.QuickCommand
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCommandsScreen(
    viewModel: QuickCommandsViewModel,
    onBack: () -> Unit,
    onUseCommand: ((QuickCommand) -> Unit)? = null,
) {
    val commands by viewModel.commands.collectAsStateWithLifecycle()
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val needChatMsg = stringResource(R.string.quick_commands_need_chat)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quick_commands_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openCreate) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.quick_commands_add))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(commands, key = { it.id }) { cmd ->
                QuickCommandCard(
                    command = cmd,
                    onEdit = { viewModel.openEdit(cmd) },
                    onDelete = { viewModel.delete(cmd.id) },
                    onUse = {
                        if (onUseCommand != null) {
                            onUseCommand(cmd)
                        } else {
                            scope.launch {
                                snackbar.showSnackbar(needChatMsg)
                            }
                        }
                    },
                )
            }
        }
    }

    if (ui.showEditor) {
        AlertDialog(
            onDismissRequest = viewModel::closeEditor,
            title = {
                Text(
                    if (ui.editing == null) stringResource(R.string.quick_commands_add)
                    else stringResource(R.string.quick_commands_edit)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ui.nameDraft,
                        onValueChange = viewModel::onNameChange,
                        label = { Text(stringResource(R.string.quick_commands_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = ui.bodyDraft,
                        onValueChange = viewModel::onBodyChange,
                        label = { Text(stringResource(R.string.quick_commands_body)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::save) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeEditor) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun QuickCommandCard(
    command: QuickCommand,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUse: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUse),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(command.name, style = MaterialTheme.typography.titleMedium)
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.quick_commands_edit))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.quick_commands_delete))
                    }
                }
            }
            Text(
                command.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onUse) {
                Text(stringResource(R.string.quick_commands_use))
            }
        }
    }
}
