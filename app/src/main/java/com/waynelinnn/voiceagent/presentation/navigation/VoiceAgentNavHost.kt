package com.waynelinnn.voiceagent.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.waynelinnn.voiceagent.presentation.history.ConversationDetailRoute
import com.waynelinnn.voiceagent.presentation.history.HistoryRoute
import com.waynelinnn.voiceagent.presentation.home.HomeRoute
import com.waynelinnn.voiceagent.presentation.settings.ModelSettingsRoute

object Routes {
    const val HOME = "home"
    const val MODEL_SETTINGS = "model_settings"
    const val HISTORY = "history"
    const val CONVERSATION = "conversation/{sessionId}"

    fun conversation(sessionId: Long): String = "conversation/$sessionId"
}

@Composable
fun VoiceAgentNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            HomeRoute(
                onOpenModelSettings = {
                    navController.navigate(Routes.MODEL_SETTINGS)
                },
                onOpenHistory = {
                    navController.navigate(Routes.HISTORY)
                },
            )
        }
        composable(Routes.MODEL_SETTINGS) {
            ModelSettingsRoute(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.HISTORY) {
            HistoryRoute(
                onBack = { navController.popBackStack() },
                onOpenSession = { sessionId ->
                    navController.navigate(Routes.conversation(sessionId))
                },
            )
        }
        composable(
            route = Routes.CONVERSATION,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.LongType },
            ),
        ) {
            ConversationDetailRoute(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
