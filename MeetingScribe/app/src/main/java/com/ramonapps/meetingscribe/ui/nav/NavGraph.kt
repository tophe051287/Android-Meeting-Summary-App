package com.ramonapps.meetingscribe.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ramonapps.meetingscribe.ui.detail.MeetingDetailScreen
import com.ramonapps.meetingscribe.ui.home.HomeScreen
import com.ramonapps.meetingscribe.ui.record.RecordScreen
import com.ramonapps.meetingscribe.ui.settings.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val RECORD = "record"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{meetingId}"
    fun detail(id: Long) = "detail/$id"
}

@Composable
fun MeetingScribeNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartRecording = { navController.navigate(Routes.RECORD) },
                onOpenMeeting = { id -> navController.navigate(Routes.detail(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.RECORD) {
            RecordScreen(onDone = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("meetingId") { type = NavType.LongType })
        ) { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getLong("meetingId") ?: 0L
            MeetingDetailScreen(meetingId = meetingId, onBack = { navController.popBackStack() })
        }
    }
}
