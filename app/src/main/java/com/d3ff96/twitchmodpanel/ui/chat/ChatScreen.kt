package com.d3ff96.twitchmodpanel.ui.chat

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d3ff96.twitchmodpanel.R
import com.d3ff96.twitchmodpanel.domain.model.ChatMessage
import com.d3ff96.twitchmodpanel.domain.model.ModActionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onOpenQuickCommands: () -> Unit,
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val composerFocus = remember { FocusRequester() }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }
    LaunchedEffect(ui.statusMessage) {
        ui.statusMessage?.let {
            snackbar.showSnackbar(it, duration = SnackbarDuration.Long)
            viewModel.clearStatus()
        }
    }
    LaunchedEffect(ui.requestComposerFocus) {
        if (ui.requestComposerFocus) {
            runCatching { composerFocus.requestFocus() }
            viewModel.consumeComposerFocusRequest()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(ui.displayName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenQuickCommands) {
                        Icon(Icons.Default.Bolt, contentDescription = stringResource(R.string.quick_commands_title))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Expanded ≈ 1/3 screen (weight 1 vs chat weight 2); collapsed = mini strip only.
            // Single composition keeps WebView (and audio) alive across expand/collapse.
            StreamPanel(
                channelLogin = ui.channelLogin,
                displayName = ui.displayName,
                isLive = ui.isLive,
                expanded = ui.streamExpanded,
                audioEnabled = ui.streamAudioEnabled,
                onToggleExpand = viewModel::toggleStreamExpanded,
                onToggleAudio = viewModel::toggleStreamAudio,
                modifier = if (ui.streamExpanded) {
                    Modifier.fillMaxWidth().weight(1f)
                } else {
                    Modifier.fillMaxWidth()
                },
            )
            HorizontalDivider()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(if (ui.streamExpanded) 2f else 1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageRow(
                        message = msg,
                        onNicknameClick = { viewModel.openProfile(msg) },
                        onBodyClick = { viewModel.startReply(msg) },
                        onLongClick = { viewModel.selectMessage(msg) },
                        onOverflowClick = { viewModel.selectMessage(msg) },
                    )
                }
            }
            ui.replyToLogin?.let { replyLogin ->
                ReplyChip(
                    login = replyLogin,
                    onClear = viewModel::clearReply,
                )
            }
            ComposerBar(
                draft = ui.draft,
                onDraftChange = viewModel::onDraftChange,
                onSend = viewModel::send,
                focusRequester = composerFocus,
            )
        }
    }

    ui.selectedMessage?.let { msg ->
        ModActionsDialog(
            message = msg,
            onDismiss = { viewModel.selectMessage(null) },
            onReply = { viewModel.startReply(msg) },
            onAction = { type -> viewModel.modAction(type) },
        )
    }

    ui.profileMessage?.let { msg ->
        ProfileBottomSheet(
            message = msg,
            onDismiss = viewModel::dismissProfile,
            onReply = { viewModel.startReply(msg) },
            onTimeout = {
                viewModel.modAction(ModActionType.TIMEOUT, fromMessage = msg)
            },
            onBan = {
                viewModel.modAction(ModActionType.BAN, fromMessage = msg)
            },
        )
    }
}

@Composable
private fun ReplyChip(login: String, onClear: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.chat_reply_chip, login),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.chat_reply_clear),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatMessageRow(
    message: ChatMessage,
    onNicknameClick: () -> Unit,
    onBodyClick: () -> Unit,
    onLongClick: () -> Unit,
    onOverflowClick: () -> Unit,
) {
    val nameColor = message.color?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
        ?: MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onBodyClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message.displayName,
            color = nameColor,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clickable(onClick = onNicknameClick),
        )
        Text(": ", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (message.isDeleted)
                MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onOverflowClick,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.mod_actions),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ComposerBar(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    focusRequester: FocusRequester,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            placeholder = { Text(stringResource(R.string.chat_composer_hint)) },
            singleLine = true,
        )
        IconButton(onClick = onSend, enabled = draft.isNotBlank()) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.chat_send))
        }
    }
}

@Composable
private fun ModActionsDialog(
    message: ChatMessage,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onAction: (ModActionType) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(message.displayName) },
        text = {
            Column {
                Text(message.text, style = MaterialTheme.typography.bodySmall)
                Text("@${message.userLogin}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Column {
                TextButton(onClick = onReply) {
                    Text(stringResource(R.string.chat_reply))
                }
                TextButton(onClick = { onAction(ModActionType.TIMEOUT) }) {
                    Text(stringResource(R.string.mod_timeout))
                }
                TextButton(onClick = { onAction(ModActionType.BAN) }) {
                    Text(stringResource(R.string.mod_ban))
                }
                TextButton(onClick = { onAction(ModActionType.DELETE) }) {
                    Text(stringResource(R.string.mod_delete))
                }
                TextButton(onClick = { onAction(ModActionType.UNBAN) }) {
                    Text(stringResource(R.string.mod_unban))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        icon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileBottomSheet(
    message: ChatMessage,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onTimeout: () -> Unit,
    onBan: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val nameColor = message.color?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
        ?: MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = message.displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = nameColor,
            )
            Text(
                text = "@${message.userLogin}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (message.badges.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.profile_badges, message.badges.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = stringResource(R.string.profile_stub_bio),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            TextButton(
                onClick = {
                    val uri = Uri.parse("https://www.twitch.tv/${message.userLogin}")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        // No browser — ignore; do not crash.
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.profile_open_twitch))
            }
            TextButton(onClick = onReply, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.chat_reply))
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onTimeout, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.mod_timeout))
                }
                TextButton(onClick = onBan, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.mod_ban))
                }
            }
        }
    }
}
