package com.d3ff96.twitchmodpanel.ui.chat

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.d3ff96.twitchmodpanel.R
import com.d3ff96.twitchmodpanel.ui.theme.ModLiveRed

/**
 * Stream player area: collapsed mini strip (~56dp) or expanded video (~1/3 screen via parent weight).
 *
 * Prefer Twitch embed WebView:
 * `https://player.twitch.tv/?channel={login}&parent=localhost&muted=false`
 *
 * WebView stays in composition when collapsed (1.dp box) so audio can keep playing;
 * expand only reveals the video area.
 *
 * **parent / embed note:** Twitch requires `parent` to match the embedding origin.
 * On Android WebView this often needs a tweak (custom HTML asset with a known parent,
 * or parent values Twitch accepts). If the embed is blocked, the purple placeholder
 * behind the WebView still shows a fake "playing" state for scaffold demos.
 */
@Composable
fun StreamPanel(
    channelLogin: String,
    displayName: String,
    isLive: Boolean,
    expanded: Boolean,
    audioEnabled: Boolean,
    onToggleExpand: () -> Unit,
    onToggleAudio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Single WebView slot: full size when expanded, 1.dp when collapsed (keeps audio).
        Box(
            modifier = if (expanded) {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF0E0E10))
            } else {
                Modifier.size(1.dp)
            },
        ) {
            if (expanded) {
                StreamStubPlaceholder(
                    channelLogin = channelLogin,
                    isLive = isLive,
                    audioEnabled = audioEnabled,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            TwitchEmbedWebView(
                channelLogin = channelLogin,
                muted = !audioEnabled,
                modifier = if (expanded) Modifier.fillMaxSize() else Modifier.size(1.dp),
            )
        }
        StreamMiniStrip(
            displayName = displayName,
            isLive = isLive,
            expanded = expanded,
            audioEnabled = audioEnabled,
            onToggleExpand = onToggleExpand,
            onToggleAudio = onToggleAudio,
        )
    }
}

@Composable
private fun StreamMiniStrip(
    displayName: String,
    isLive: Boolean,
    expanded: Boolean,
    audioEnabled: Boolean,
    onToggleExpand: () -> Unit,
    onToggleAudio: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (isLive) stringResource(R.string.chat_live) else stringResource(R.string.chat_offline),
                color = if (isLive) ModLiveRed else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
            )
            Column {
                Text(
                    text = displayName.ifBlank { channelLogin },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = if (audioEnabled) {
                        stringResource(R.string.stream_audio_playing)
                    } else {
                        stringResource(R.string.stream_audio_muted)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        IconButton(onClick = onToggleAudio) {
            Icon(
                imageVector = if (audioEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                contentDescription = stringResource(
                    if (audioEnabled) R.string.stream_mute else R.string.stream_unmute,
                ),
            )
        }
        IconButton(onClick = onToggleExpand) {
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(
                    if (expanded) R.string.stream_collapse else R.string.stream_expand,
                ),
            )
        }
    }
}

@Composable
private fun StreamStubPlaceholder(
    channelLogin: String,
    isLive: Boolean,
    audioEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color(0xFF9147FF)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isLive) stringResource(R.string.stream_stub_playing) else stringResource(R.string.chat_offline),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = stringResource(R.string.stream_embed_hint, channelLogin),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp),
            )
            if (!audioEnabled) {
                Text(
                    text = stringResource(R.string.stream_audio_muted),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TwitchEmbedWebView(
    channelLogin: String,
    muted: Boolean,
    modifier: Modifier = Modifier,
) {
    val embedUrl = remember(channelLogin, muted) {
        "https://player.twitch.tv/?channel=$channelLogin&parent=localhost&muted=$muted"
    }
    key(channelLogin) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    loadUrl(embedUrl)
                }
            },
            update = { webView ->
                if (webView.url != embedUrl) {
                    webView.loadUrl(embedUrl)
                }
            },
        )
    }
}
