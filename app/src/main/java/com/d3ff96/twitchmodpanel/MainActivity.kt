package com.d3ff96.twitchmodpanel

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.d3ff96.twitchmodpanel.data.repository.AppContainer
import com.d3ff96.twitchmodpanel.ui.navigation.TwitchModNavHost
import com.d3ff96.twitchmodpanel.ui.theme.TwitchModPanelTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthIntent(intent)
        setContent {
            TwitchModPanelTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TwitchModNavHost()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "twitchmodpanel" && data.host == "oauth") {
            lifecycleScope.launch {
                AppContainer.authRepository.handleOAuthRedirect(data.toString())
            }
        }
    }
}
