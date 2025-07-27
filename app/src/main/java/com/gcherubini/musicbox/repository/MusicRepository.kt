package com.gcherubini.musicbox.repository

import com.gcherubini.musicbox.model.Music
import kotlinx.coroutines.delay

object MusicRepository {
    suspend fun fetchMusicList(): List<Music> {
        delay(5000) // simula tempo de rede
        return listOf(
            Music(
                id = "1",
                title = "Contact",
                label = "Sintoniza",
                releaseDate = "2024-10-12",
                genre = "Deep Tech",
                coverImageUrl = "https://i1.sndcdn.com/artworks-Uspv5rImzny7MyXl-egaySw-t1080x1080.png",
                artist = "Guilherme Cherubini",
                spotifyTrack = "https://open.spotify.com/track/3gXhebY2YvsBHMvR28CVM2?si=75ce53cd1fe44ce0"
            ),
            Music(
                id = "2",
                title = "Futuretro",
                label = "Addiction 21",
                releaseDate = "2024-11-01",
                genre = "Electro House",
                coverImageUrl = "mock",
                artist = "n/a",
            ),
            Music(
                id = "3",
                title = "create your own heaven",
                label = "Addiction 21",
                releaseDate = "2024-11-18",
                genre = "Deep Tech",
                coverImageUrl = "https://i1.sndcdn.com/artworks-LAF9qoUAgC9HNsrw-7jYqeg-t1080x1080.jpg",
                artist = "da lighT",
                spotifyTrack = "https://open.spotify.com/track/6JHYRD1ocBipyDBgcMIyTp?si=7b725dbc835f4197"
            ),
            Music(
                id = "4",
                title = "Vai Vai",
                label = "Addiction 21",
                releaseDate = "2024-12-05",
                genre = "Minimal / Deep Tech",
                coverImageUrl = "mock",
                artist = "n/a"
            ),
            Music(
                id = "5",
                title = "Electric X",
                label = "Addiction 21",
                releaseDate = "2024-10-25",
                genre = "Festival Tech House",
                coverImageUrl = "mock",
                artist = "n/a"
            ),
            Music(
                id = "6",
                title = "Night Frequencies",
                label = "Lime Distro",
                releaseDate = "2024-09-28",
                genre = "Deep House",
                coverImageUrl = "mock",
                artist = "n/a"
            ),
            Music(
                id = "7",
                title = "Underground Glow",
                label = "Minimal Drive",
                releaseDate = "2024-08-15",
                genre = "Minimal",
                coverImageUrl = "mock",
                artist = "n/a"
            ),
            Music(
                id = "8",
                title = "Pulse Theory",
                label = "Groove Core",
                releaseDate = "2024-09-02",
                genre = "Tech House",
                coverImageUrl = "mock",
                artist = "n/a"
            ),
            Music(
                id = "9",
                title = "Low Lights",
                label = "Addiction 21",
                releaseDate = "2024-10-01",
                genre = "Deep / Dub Techno",
                coverImageUrl = "mock",
                artist = "n/a"
            ),
            Music(
                id = "10",
                title = "Analog Dreams",
                label = "Lime Distro",
                releaseDate = "2024-07-22",
                genre = "Electronica",
                coverImageUrl = "mock",
                artist = "n/a"
            )
        )
    }
}