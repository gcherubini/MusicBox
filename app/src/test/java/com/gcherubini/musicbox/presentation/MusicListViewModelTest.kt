package com.gcherubini.musicbox.presentation

import com.gcherubini.musicbox.domain.model.Music
import com.gcherubini.musicbox.domain.repository.MusicRepository
import com.gcherubini.musicbox.domain.usecase.GetMusicsUseCase
import com.gcherubini.musicbox.presentation.musiclist.MusicListEffect
import com.gcherubini.musicbox.presentation.musiclist.MusicListIntent
import com.gcherubini.musicbox.presentation.musiclist.MusicListUiState
import com.gcherubini.musicbox.presentation.musiclist.MusicListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.IOException

private fun listSample() = Music("1", "Contact", "C", "Sintoniza", "2024-10-12", "Deep Tech", "Gui", null)

@OptIn(ExperimentalCoroutinesApi::class)
class ListMainDispatcherRule : TestWatcher() {
    override fun starting(d: Description) { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    override fun finished(d: Description) { Dispatchers.resetMain() }
}

class MusicListViewModelTest {
    @get:Rule val rule = ListMainDispatcherRule()

    @Test fun `init emite Success e click emite OpenDetail 1x`() = runTest {
        val vm = MusicListViewModel(GetMusicsUseCase(object : MusicRepository {
            override suspend fun getMusics() = listOf(listSample())
            override suspend fun getMusicById(id: String) = listSample()
        }))
        advanceUntilIdle()
        assertTrue(vm.state.value is MusicListUiState.Success)
        val received = mutableListOf<MusicListEffect>()
        val job = launch { vm.effects.collect { received += it } }
        vm.onIntent(MusicListIntent.MusicClicked("1")); advanceUntilIdle()
        job.cancel()
        assertEquals(listOf(MusicListEffect.OpenDetail("1")), received)
    }

    @Test fun `falha de rede emite Error e Retry recupera`() = runTest {
        var fail = true
        val vm = MusicListViewModel(GetMusicsUseCase(object : MusicRepository {
            override suspend fun getMusics() = if (fail) throw IOException("sem rede") else listOf(listSample())
            override suspend fun getMusicById(id: String) = null
        }))
        advanceUntilIdle()
        assertTrue(vm.state.value is MusicListUiState.Error)
        fail = false; vm.onIntent(MusicListIntent.Retry); advanceUntilIdle()
        assertTrue(vm.state.value is MusicListUiState.Success)
    }
}
