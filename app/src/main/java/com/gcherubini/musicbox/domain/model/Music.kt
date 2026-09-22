package com.gcherubini.musicbox.domain.model

data class Music(
    val id: String,
    val title: String,
    val coverImageUrl: String,
    val label: String,
    val releaseDate: String,
    val genre: String,
    val artist: String,
    val spotifyTrack: String? = null
)
