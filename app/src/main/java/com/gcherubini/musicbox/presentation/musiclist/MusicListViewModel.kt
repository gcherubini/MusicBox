package com.gcherubini.musicbox.presentation.musiclist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcherubini.musicbox.domain.usecase.GetMusicsUseCase
import com.gcherubini.musicbox.presentation.mapper.toUiModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MusicListViewModel(private val getMusics: GetMusicsUseCase) : ViewModel() {
    private val _state = MutableStateFlow<MusicListUiState>(MusicListUiState.Loading)
    val state: StateFlow<MusicListUiState> = _state.asStateFlow()
    private val _effects = Channel<MusicListEffect>(Channel.BUFFERED)
    val effects: Flow<MusicListEffect> = _effects.receiveAsFlow()

    init { load() }

    fun onIntent(intent: MusicListIntent) {
        when (intent) {
            is MusicListIntent.Retry -> load()
            is MusicListIntent.MusicClicked -> viewModelScope.launch {
                _effects.send(MusicListEffect.OpenDetail(intent.id))
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = MusicListUiState.Loading
            try {
                _state.value = MusicListUiState.Success(getMusics().map { it.toUiModel() })
            } catch (e: Exception) {
                _state.value = MusicListUiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }
}
