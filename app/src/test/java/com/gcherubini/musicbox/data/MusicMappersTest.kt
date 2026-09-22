package com.gcherubini.musicbox.data

import com.gcherubini.musicbox.data.mapper.toDomain
import com.gcherubini.musicbox.data.remote.dto.MusicDto
import org.junit.Assert.*
import org.junit.Test

class MusicMappersTest {
    @Test fun `toDomain mapeia todos os campos e preserva spotify null`() {
        val dto = MusicDto("1", "Contact", "C", "Sintoniza", "2024-10-12", "Deep Tech", "Gui", null)
        val d = dto.toDomain()
        assertEquals("1", d.id); assertEquals("Contact", d.title); assertNull(d.spotifyTrack)
    }
    @Test fun `toDomain preserva spotifyTrack e cover mock`() {
        val dto = MusicDto("2", "Futuretro", "mock", "Addiction 21", "2024-11-01", "Electro House", "n/a", "https://open.spotify.com/x")
        val d = dto.toDomain()
        assertEquals("mock", d.coverImageUrl); assertNotNull(d.spotifyTrack)
    }
}
