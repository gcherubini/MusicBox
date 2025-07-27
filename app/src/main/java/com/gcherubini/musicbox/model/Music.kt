package com.gcherubini.musicbox.model

data class Music(
    val id: String,
    val title: String,
    val coverImageUrl: String,
    val label: String,
    val releaseDate: String,
    val genre: String,
    val artist: String,
)
