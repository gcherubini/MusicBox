package com.gcherubini.musicbox.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gcherubini.musicbox.R
import com.gcherubini.musicbox.model.Music
import com.gcherubini.musicbox.viewmodel.MusicViewModel
import com.gcherubini.musicbox.viewmodel.MusicUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicListScreen(
    viewModel: MusicViewModel,
    navController: NavController,
) {
    // ✅ Chama apenas uma vez quando a tela for composta
    LaunchedEffect(Unit) {
        if (viewModel.uiState.value !is MusicUiState.Success) {
            viewModel.fetchMusics()
        }
    }

    val uiState by viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explore") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }

    ) { padding ->
        when (uiState) {
            is MusicUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is MusicUiState.Error -> {
                val message = (uiState as MusicUiState.Error).message
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(message)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.fetchMusics() }) {
                            Text("Tentar novamente")
                        }
                    }
                }
            }

            is MusicUiState.Success -> {
                val musics = (uiState as MusicUiState.Success).musics
                LazyColumn(
                    contentPadding = padding,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(musics) { music ->
                        MusicItem(music) {
                            navController.navigate("music_detail/${music.id}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MusicItem(music: Music, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable { onClick() } // Tornar o item clicável
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