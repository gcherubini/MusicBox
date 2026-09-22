package com.gcherubini.musicbox.data.mapper

import com.gcherubini.musicbox.data.remote.dto.MusicDto
import com.gcherubini.musicbox.domain.model.Music

fun MusicDto.toDomain() = Music(id, title, coverImageUrl, label, releaseDate, genre, artist, spotifyTrack)
