package com.gcherubini.musicbox.screens.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object MusicList : Screen("music_list")
    object MusicDetail : Screen("music_detail/{musicId}") {
        fun createRoute(musicId: String) = "music_detail/$musicId"
    }
}
