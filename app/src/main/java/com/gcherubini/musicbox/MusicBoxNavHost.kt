package com.gcherubini.musicbox

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gcherubini.musicbox.screens.navigation.Screen
import com.gcherubini.musicbox.screens.DetailScreen
import com.gcherubini.musicbox.screens.MusicListScreen
import com.gcherubini.musicbox.screens.WelcomeScreen
import com.gcherubini.musicbox.screens.navigation.MUSIC_DETAIL_ARGUMENT_ID
import com.gcherubini.musicbox.viewmodel.MusicUiState
import com.gcherubini.musicbox.viewmodel.MusicViewModel

@Composable
fun MusicBoxNavHost() {
    val navController = rememberNavController()
    val viewModel: MusicViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onExploreClick = {
                    navController.navigate(Screen.MusicList.route)
                }
            )
        }
        composable(Screen.MusicList.route) {
            MusicListScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
        composable(
            route = Screen.MusicDetail.route,
            arguments = listOf(navArgument(MUSIC_DETAIL_ARGUMENT_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val musicId = backStackEntry.arguments?.getString("musicId")

            val uiState = viewModel.uiState.value
            val music = if (uiState is MusicUiState.Success) {
                uiState.musics.find { it.id == musicId }
            } else null

            music?.let {
                DetailScreen(
                    music = music,
                    navController = navController
                )
            }
        }
    }
}
