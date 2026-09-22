package com.gcherubini.musicbox.di

import com.gcherubini.musicbox.data.remote.api.ApiConfig
import com.gcherubini.musicbox.data.remote.api.MusicApi
import com.gcherubini.musicbox.data.repository.MusicRepositoryImpl
import com.gcherubini.musicbox.domain.usecase.GetMusicByIdUseCase
import com.gcherubini.musicbox.domain.usecase.GetMusicsUseCase
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AppContainer {
    private val json = Json { ignoreUnknownKeys = true }
    private val api: MusicApi = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .addConverterFactory(
            json.asConverterFactory("application/json".toMediaType())
        )
        .build().create(MusicApi::class.java)
    private val repo = MusicRepositoryImpl(api)
    val getMusics = GetMusicsUseCase(repo)
    val getMusicById = GetMusicByIdUseCase(repo)
}
