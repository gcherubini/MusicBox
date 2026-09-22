package com.gcherubini.musicbox.presentation.detail

import com.gcherubini.musicbox.presentation.model.MusicUiModel

sealed interface DetailIntent {
    data object Retry : DetailIntent
}

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Error(val message: String) : DetailUiState
    data class Success(val music: MusicUiModel) : DetailUiState
}
