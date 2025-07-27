package com.gcherubini.musicbox.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcherubini.musicbox.model.Music
import com.gcherubini.musicbox.repository.MusicRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.State

sealed class MusicUiState {
    object Loading : MusicUiState()
    data class Success(val musics: List<Music>) : MusicUiState()
    data class Error(val message: String) : MusicUiState()
}

class MusicViewModel : ViewModel() {

    private val _uiState = mutableStateOf<MusicUiState>(MusicUiState.Loading)
    val uiState: State<MusicUiState> = _uiState

    internal fun fetchMusics() {
        viewModelScope.launch {
            _uiState.value = MusicUiState.Loading
            try {
                val result = MusicRepository.fetchMusicList()
                _uiState.value = MusicUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = MusicUiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }
}