package com.gcherubini.musicbox.presentation.mapper

import com.gcherubini.musicbox.domain.model.Music
import com.gcherubini.musicbox.presentation.model.MusicUiModel

fun Music.toUiModel() = MusicUiModel(
    id = id,
    title = title,
    artist = artist,
    label = label,
    releaseDate = releaseDate,
    genre = genre,
    coverImageUrl = coverImageUrl,
    spotifyTrack = spotifyTrack
)
