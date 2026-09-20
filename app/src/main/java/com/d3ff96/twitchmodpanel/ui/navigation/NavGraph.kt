package com.d3ff96.twitchmodpanel.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.d3ff96.twitchmodpanel.ui.channelpicker.ChannelPickerScreen
import com.d3ff96.twitchmodpanel.ui.channelpicker.ChannelPickerViewModel
import com.d3ff96.twitchmodpanel.ui.chat.ChatScreen
import com.d3ff96.twitchmodpanel.ui.chat.ChatViewModel
import com.d3ff96.twitchmodpanel.ui.login.LoginScreen
import com.d3ff96.twitchmodpanel.ui.login.LoginViewModel
import com.d3ff96.twitchmodpanel.ui.quickcommands.QuickCommandsScreen
import com.d3ff96.twitchmodpanel.ui.quickcommands.QuickCommandsViewModel

object Routes {
    const val LOGIN = "login"
    const val CHANNELS = "channels"
    const val CHAT = "chat/{channelId}/{channelLogin}/{displayName}/{isLive}"
    const val QUICK = "quick"

    const val QUICK_REPLY_KEY = "quick_reply"

    fun chat(channelId: String, login: String, displayName: String, isLive: Boolean): String =
        "chat/$channelId/$login/$displayName/$isLive"
}

@Composable
fun TwitchModNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.LOGIN,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            val vm: LoginViewModel = viewModel()
            LoginScreen(
                viewModel = vm,
                onAuthenticated = {
                    navController.navigate(Routes.CHANNELS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.CHANNELS) {
            val vm: ChannelPickerViewModel = viewModel()
            ChannelPickerScreen(
                viewModel = vm,
                onChannelSelected = { ch ->
                    navController.navigate(
                        Routes.chat(ch.id, ch.login, ch.displayName, ch.isLive)
                    )
                },
                onOpenQuickCommands = { navController.navigate(Routes.QUICK) },
            )
        }
        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("channelId") { type = NavType.StringType },
                navArgument("channelLogin") { type = NavType.StringType },
                navArgument("displayName") { type = NavType.StringType },
                navArgument("isLive") { type = NavType.BoolType },
            ),
        ) { entry ->
            val channelId = entry.arguments?.getString("channelId").orEmpty()
            val login = entry.arguments?.getString("channelLogin").orEmpty()
            val displayName = entry.arguments?.getString("displayName").orEmpty()
            val isLive = entry.arguments?.getBoolean("isLive") ?: false
            val factory = remember(channelId) {
                ChatViewModel.factory(channelId, login, displayName, isLive)
            }
            val vm: ChatViewModel = viewModel(factory = factory)

            // Observe quick-reply body set by QuickCommandsScreen via savedStateHandle.
            val quickReplyFlow = remember(entry) {
                entry.savedStateHandle.getStateFlow<String?>(Routes.QUICK_REPLY_KEY, null)
            }
            val quickReply by quickReplyFlow.collectAsStateWithLifecycle()
            LaunchedEffect(quickReply) {
                val body = quickReply ?: return@LaunchedEffect
                vm.sendQuickReply(body)
                entry.savedStateHandle[Routes.QUICK_REPLY_KEY] = null
            }

            ChatScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenQuickCommands = { navController.navigate(Routes.QUICK) },
            )
        }
        composable(Routes.QUICK) {
            val vm: QuickCommandsViewModel = viewModel()
            // Prefer previous entry if it is chat (has quick_reply handle under chat).
            val previous = navController.previousBackStackEntry
            val fromChat = previous?.destination?.route?.startsWith("chat/") == true
            QuickCommandsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onUseCommand = if (fromChat) {
                    { cmd ->
                        previous?.savedStateHandle?.set(Routes.QUICK_REPLY_KEY, cmd.body)
                        navController.popBackStack()
                    }
                } else {
                    null
                },
            )
        }
    }
}
