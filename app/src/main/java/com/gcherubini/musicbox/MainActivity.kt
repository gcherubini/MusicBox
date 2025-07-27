package com.gcherubini.musicbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gcherubini.musicbox.model.Music
import com.gcherubini.musicbox.navigation.Screen
import com.gcherubini.musicbox.screens.MusicListScreen
import com.gcherubini.musicbox.screens.WelcomeScreen
import com.gcherubini.musicbox.ui.theme.MusicBoxTheme

private val musicsList = listOf(
    Music(
        title = "Contact",
        label = "Sintoniza",
        releaseDate = "2024-10-12",
        genre = "Deep Tech",
        coverImageUrl = "https://i1.sndcdn.com/artworks-Uspv5rImzny7MyXl-egaySw-t1080x1080.png",
        artist = "Guilherme Cherubini"
    ),
    Music(
        title = "Futuretro",
        label = "Addiction 21",
        releaseDate = "2024-11-01",
        genre = "Electro House",
        coverImageUrl = "mock",
        artist = "n/a"
    ),
    Music(
        title = "create your own heaven",
        label = "Addiction 21",
        releaseDate = "2024-11-18",
        genre = "Deep Tech",
        coverImageUrl = "https://i1.sndcdn.com/artworks-LAF9qoUAgC9HNsrw-7jYqeg-t1080x1080.jpg",
        artist = "da lighT"
    ),
    Music(
        title = "Vai Vai",
        label = "Addiction 21",
        releaseDate = "2024-12-05",
        genre = "Minimal / Deep Tech",
        coverImageUrl = "mock",
        artist = "n/a"
    ),
    Music(
        title = "Electric X",
        label = "Addiction 21",
        releaseDate = "2024-10-25",
        genre = "Festival Tech House",
        coverImageUrl = "mock",
        artist = "n/a"
    ),
    Music(
        title = "Night Frequencies",
        label = "Lime Distro",
        releaseDate = "2024-09-28",
        genre = "Deep House",
        coverImageUrl = "mock",
        artist = "n/a"
    ),
    Music(
        title = "Underground Glow",
        label = "Minimal Drive",
        releaseDate = "2024-08-15",
        genre = "Minimal",
        coverImageUrl = "mock",
        artist = "n/a"
    ),
    Music(
        title = "Pulse Theory",
        label = "Groove Core",
        releaseDate = "2024-09-02",
        genre = "Tech House",
        coverImageUrl = "mock",
        artist = "n/a"
    ),
    Music(
        title = "Low Lights",
        label = "Addiction 21",
        releaseDate = "2024-10-01",
        genre = "Deep / Dub Techno",
        coverImageUrl = "mock",
        artist = "n/a"
    ),
    Music(
        title = "Analog Dreams",
        label = "Lime Distro",
        releaseDate = "2024-07-22",
        genre = "Electronica",
        coverImageUrl = "mock",
        artist = "n/a"
    )
)


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicBoxTheme {
                val navController = rememberNavController()

                // NAV HOST serve para gerenciar telas da aplicação,
                // abaixo temos as duas primeiras telas WelcomeScreen e MusicListScreen
                NavHost(
                    navController = navController,
                    startDestination = Screen.Welcome.route
                ) {
                    composable(Screen.Welcome.route) {
                        WelcomeScreen(
                            onExploreClick = {
                                navController.navigate(Screen.MusicList.route)
                            }
                        )
                    }
                    composable(Screen.MusicList.route) {
                        MusicListScreen(
                            musicList = musicsList,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}
