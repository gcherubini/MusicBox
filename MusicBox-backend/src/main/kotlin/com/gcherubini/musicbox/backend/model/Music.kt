package com.gcherubini.musicbox.backend.model
import kotlinx.serialization.Serializable

@Serializable
data class Music(
    val id: String = "",
    val title: String,
    val coverImageUrl: String,
    val label: String,
    val releaseDate: String,
    val genre: String,
    val artist: String,
    val spotifyTrack: String? = null
)
