package com.gcherubini.musicbox.presentation

import com.gcherubini.musicbox.domain.model.Music
import com.gcherubini.musicbox.domain.repository.MusicRepository
import com.gcherubini.musicbox.domain.usecase.GetMusicByIdUseCase
import com.gcherubini.musicbox.presentation.detail.DetailIntent
import com.gcherubini.musicbox.presentation.detail.DetailUiState
import com.gcherubini.musicbox.presentation.detail.DetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.IOException

private fun detailSample() = Music("1", "Contact", "C", "Sintoniza", "2024-10-12", "Deep Tech", "Gui", null)

@OptIn(ExperimentalCoroutinesApi::class)
class DetailMainDispatcherRule : TestWatcher() {
    override fun starting(d: Description) { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    override fun finished(d: Description) { Dispatchers.resetMain() }
}

class DetailViewModelTest {
    @get:Rule val rule = DetailMainDispatcherRule()

    @Test fun `id inexistente emite Error Musica nao encontrada`() = runTest {
        val vm = DetailViewModel("xyz", GetMusicByIdUseCase(object : MusicRepository {
            override suspend fun getMusics() = emptyList<Music>()
            override suspend fun getMusicById(id: String) = null
        }))
        advanceUntilIdle()
        val s = vm.state.value as DetailUiState.Error
        assertEquals("Música não encontrada", s.message)
    }

    @Test fun `Retry apos falha recupera para Success`() = runTest {
        var fail = true
        val vm = DetailViewModel("1", GetMusicByIdUseCase(object : MusicRepository {
            override suspend fun getMusics() = emptyList<Music>()
            override suspend fun getMusicById(id: String) =
                if (fail) throw IOException("sem rede") else detailSample()
        }))
        advanceUntilIdle(); assertTrue(vm.state.value is DetailUiState.Error)
        fail = false; vm.onIntent(DetailIntent.Retry); advanceUntilIdle()
        assertTrue(vm.state.value is DetailUiState.Success)
    }
}
