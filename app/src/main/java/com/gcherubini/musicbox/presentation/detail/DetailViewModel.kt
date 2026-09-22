package com.gcherubini.musicbox.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcherubini.musicbox.domain.usecase.GetMusicByIdUseCase
import com.gcherubini.musicbox.presentation.mapper.toUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    private val musicId: String,
    private val getById: GetMusicByIdUseCase
) : ViewModel() {
    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init { load() }

    fun onIntent(intent: DetailIntent) {
        if (intent is DetailIntent.Retry) load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = DetailUiState.Loading
            try {
                val m = getById(musicId)?.toUiModel()
                _state.value = if (m == null) DetailUiState.Error("Música não encontrada")
                else DetailUiState.Success(m)
            } catch (e: Exception) {
                _state.value = DetailUiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }
}
