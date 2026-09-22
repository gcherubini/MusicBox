package com.gcherubini.musicbox.domain.usecase

import com.gcherubini.musicbox.domain.repository.MusicRepository

class GetMusicByIdUseCase(private val repo: MusicRepository) {
    suspend operator fun invoke(id: String) = repo.getMusicById(id)
}
