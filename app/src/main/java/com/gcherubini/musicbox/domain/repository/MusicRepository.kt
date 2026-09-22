package com.gcherubini.musicbox.domain.repository

import com.gcherubini.musicbox.domain.model.Music

interface MusicRepository {
    suspend fun getMusics(): List<Music>
    suspend fun getMusicById(id: String): Music?
}
