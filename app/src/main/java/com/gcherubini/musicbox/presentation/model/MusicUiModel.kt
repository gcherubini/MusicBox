package com.gcherubini.musicbox.presentation.model

data class MusicUiModel(
    val id: String,
    val title: String,
    val artist: String,
    val label: String,
    val releaseDate: String,
    val genre: String,
    val coverImageUrl: String,
    val spotifyTrack: String? = null
)
