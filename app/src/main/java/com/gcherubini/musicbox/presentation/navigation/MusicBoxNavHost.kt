package com.gcherubini.musicbox.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gcherubini.musicbox.di.AppContainer
import com.gcherubini.musicbox.presentation.detail.DetailScreen
import com.gcherubini.musicbox.presentation.detail.DetailViewModel
import com.gcherubini.musicbox.presentation.musiclist.MusicListScreen
import com.gcherubini.musicbox.presentation.musiclist.MusicListViewModel
import com.gcherubini.musicbox.presentation.welcome.WelcomeScreen

@Composable
fun MusicBoxNavHost(container: AppContainer) {
    val navController = rememberNavController()

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
            val vm: MusicListViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { MusicListViewModel(container.getMusics) }
                }
            )
            MusicListScreen(
                viewModel = vm,
                navController = navController
            )
        }
        composable(
            route = Screen.MusicDetail.route,
            arguments = listOf(navArgument(MUSIC_DETAIL_ARGUMENT_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val musicId = backStackEntry.arguments?.getString(MUSIC_DETAIL_ARGUMENT_ID).orEmpty()
            val vm: DetailViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { DetailViewModel(musicId, container.getMusicById) }
                }
            )
            DetailScreen(
                viewModel = vm,
                navController = navController
            )
        }
    }
}
