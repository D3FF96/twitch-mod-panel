package com.d3ff96.twitchmodpanel.ui.login

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d3ff96.twitchmodpanel.R
import com.d3ff96.twitchmodpanel.domain.model.DeviceAuthSession

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onAuthenticated: () -> Unit,
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(ui.auth.isAuthenticated) {
        if (ui.auth.isAuthenticated) onAuthenticated()
    }
    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            val session = ui.deviceSession
            if (session != null) {
                DeviceCodePanel(
                    session = session,
                    onOpenBrowser = {
                        openVerificationUri(context, session.verificationUri)
                    },
                    onCopyCode = {
                        copyUserCode(context, session.userCode)
                    },
                    onCancel = viewModel::cancelDeviceAuth,
                )
            } else {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.login_client_id_hint),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Start,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (ui.auth.clientIdConfigured) "Client ID: задан ✓"
                    else "Client ID: не задан ✗",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (ui.auth.clientIdConfigured)
                        MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(24.dp))
                if (ui.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = viewModel::startOAuth,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.login_button))
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = viewModel::continueAsStub,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.login_stub_continue))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCodePanel(
    session: DeviceAuthSession,
    onOpenBrowser: () -> Unit,
    onCopyCode: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.login_device_hint),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = session.userCode,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp,
                modifier = Modifier
                    .clickable(onClick = onCopyCode)
                    .padding(8.dp),
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.login_device_tap_copy),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = session.statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator()
        }
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = onOpenBrowser,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.login_device_open))
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.login_device_cancel))
    }
}

private fun openVerificationUri(context: Context, uri: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: Exception) {
        Toast.makeText(context, "Не удалось открыть браузер", Toast.LENGTH_SHORT).show()
    }
}

private fun copyUserCode(context: Context, code: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("twitch_user_code", code))
    Toast.makeText(context, "Код скопирован", Toast.LENGTH_SHORT).show()
}
