package com.gcherubini.musicbox.data.remote.api

import com.gcherubini.musicbox.data.remote.dto.MusicDto
import retrofit2.http.GET
import retrofit2.http.Path

interface MusicApi {
    @GET("musics") suspend fun getMusics(): List<MusicDto>
    @GET("musics/{id}") suspend fun getMusicById(@Path("id") id: String): MusicDto
}
