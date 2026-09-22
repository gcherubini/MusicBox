package com.gcherubini.musicbox.data.repository

import com.gcherubini.musicbox.data.mapper.toDomain
import com.gcherubini.musicbox.data.remote.api.MusicApi
import com.gcherubini.musicbox.domain.model.Music
import com.gcherubini.musicbox.domain.repository.MusicRepository
import retrofit2.HttpException

class MusicRepositoryImpl(private val api: MusicApi) : MusicRepository {
    override suspend fun getMusics() = api.getMusics().map { it.toDomain() }
    override suspend fun getMusicById(id: String): Music? = try {
        api.getMusicById(id).toDomain()
    } catch (e: HttpException) {
        if (e.code() == 404) null else throw e
    }
}
