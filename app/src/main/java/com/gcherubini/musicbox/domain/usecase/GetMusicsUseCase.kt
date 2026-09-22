package com.gcherubini.musicbox.domain.usecase

import com.gcherubini.musicbox.domain.repository.MusicRepository

class GetMusicsUseCase(private val repo: MusicRepository) {
    suspend operator fun invoke() = repo.getMusics()
}
