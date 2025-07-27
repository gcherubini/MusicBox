package com.gcherubini.musicbox.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object MusicList : Screen("music_list")
}
