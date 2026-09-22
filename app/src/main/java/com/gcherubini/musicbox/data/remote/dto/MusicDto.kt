package com.gcherubini.musicbox.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MusicDto(
    val id: String,
    val title: String,
    val coverImageUrl: String,
    val label: String,
    val releaseDate: String,
    val genre: String,
    val artist: String,
    val spotifyTrack: String? = null
)
