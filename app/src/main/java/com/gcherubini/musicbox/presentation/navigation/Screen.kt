package com.gcherubini.musicbox.presentation.navigation

const val MUSIC_DETAIL_ARGUMENT_ID = "musicId"

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object MusicList : Screen("music_list")
    data object MusicDetail : Screen("music_detail/{$MUSIC_DETAIL_ARGUMENT_ID}") {
        fun createRoute(musicId: String) = "music_detail/$musicId"
    }
}
