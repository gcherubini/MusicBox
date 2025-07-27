package com.gcherubini.musicbox.screens.navigation

const val MUSIC_DETAIL_ARGUMENT_ID = "musicId"

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object MusicList : Screen("music_list")
    object MusicDetail : Screen("music_detail/{${MUSIC_DETAIL_ARGUMENT_ID}}") {
        fun createRoute(musicId: String) = "music_detail/$musicId"
    }
}
