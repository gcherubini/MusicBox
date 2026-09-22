package com.gcherubini.musicbox.domain

import com.gcherubini.musicbox.domain.model.Music
import com.gcherubini.musicbox.domain.repository.MusicRepository
import com.gcherubini.musicbox.domain.usecase.GetMusicByIdUseCase
import com.gcherubini.musicbox.domain.usecase.GetMusicsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

private class FakeRepo(val items: List<Music>) : MusicRepository {
    override suspend fun getMusics() = items
    override suspend fun getMusicById(id: String) = items.find { it.id == id }
}

private fun sample() = Music("1", "Contact", "C", "Sintoniza", "2024-10-12", "Deep Tech", "Guilherme Cherubini", null)

class GetMusicsUseCaseTest {
    @Test fun `invoke retorna lista`() = runTest {
        assertEquals(1, GetMusicsUseCase(FakeRepo(listOf(sample())))().size)
    }
    @Test fun `invoke por id existente e inexistente`() = runTest {
        val uc = GetMusicByIdUseCase(FakeRepo(listOf(sample())))
        assertNotNull(uc("1")); assertNull(uc("xyz"))
    }
    @Test fun `Review Focus - coverImageUrl mock nao quebra domain`() = runTest {
        val m = sample().copy(coverImageUrl = "mock")
        assertEquals("mock", GetMusicsUseCase(FakeRepo(listOf(m)))().first().coverImageUrl)
    }
}
