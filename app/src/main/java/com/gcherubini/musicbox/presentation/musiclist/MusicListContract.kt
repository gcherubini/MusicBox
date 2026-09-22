package com.gcherubini.musicbox.presentation.musiclist

import com.gcherubini.musicbox.presentation.model.MusicUiModel

sealed interface MusicListIntent {
    data object Retry : MusicListIntent
    data class MusicClicked(val id: String) : MusicListIntent
}

sealed interface MusicListUiState {
    data object Loading : MusicListUiState
    data class Error(val message: String) : MusicListUiState
    data class Success(val musics: List<MusicUiModel>) : MusicListUiState
}

sealed interface MusicListEffect {
    data class OpenDetail(val id: String) : MusicListEffect
}
