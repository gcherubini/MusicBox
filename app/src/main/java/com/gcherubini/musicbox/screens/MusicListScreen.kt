package com.gcherubini.musicbox.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gcherubini.musicbox.R
import com.gcherubini.musicbox.model.Music


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicListScreen(
    musicList: List<Music>,
    modifier: Modifier = Modifier,
    navController: NavController,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explorar") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }

    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(musicList) { music ->
                MusicItem(music = music)
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun MusicItem(music: Music) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(music.coverImageUrl)
                .crossfade(true) // anima a troca da imagem
                .build(),
            placeholder = painterResource(R.drawable.loading_image_placeholder), // crie um drawable placeholder
            error = painterResource(R.drawable.image_not_loaded_placeholder), // drawable para erro no carregamento
            contentDescription = "Capa do álbum",
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp)) // cantos arredondados
                .padding(end = 12.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(text = music.title, style = MaterialTheme.typography.titleMedium)
            Text(text = "Artist: ${music.artist}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Label: ${music.label}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Genre: ${music.genre}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Release Date: ${music.releaseDate}", style = MaterialTheme.typography.bodySmall)
        }
    }
}