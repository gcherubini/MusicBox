package com.gcherubini.musicbox.presentation.detail

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gcherubini.musicbox.R
import com.gcherubini.musicbox.presentation.model.MusicUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val title = when (val s = uiState) {
        is DetailUiState.Success -> s.music.title
        else -> "Detalhe"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
        when (val s = uiState) {
            is DetailUiState.Loading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is DetailUiState.Error -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.onIntent(DetailIntent.Retry) }) {
                            Text("Tentar novamente")
                        }
                    }
                }
            }
            is DetailUiState.Success -> {
                DetailContent(
                    music = s.music,
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }
}

@Composable
private fun DetailContent(music: MusicUiModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(music.coverImageUrl)
                .crossfade(true)
                .build(),
            placeholder = painterResource(R.drawable.loading_image_placeholder),
            error = painterResource(R.drawable.image_not_loaded_placeholder),
            contentDescription = "Capa do álbum",
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(8.dp))
                .padding(end = 12.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Artist: ${music.artist}", style = MaterialTheme.typography.bodySmall)
        Text(text = "Label: ${music.label}", style = MaterialTheme.typography.bodySmall)
        Text(text = "Genre: ${music.genre}", style = MaterialTheme.typography.bodySmall)
        Text(text = "Release Date: ${music.releaseDate}", style = MaterialTheme.typography.bodySmall)

        music.spotifyTrack?.let {
            Button(onClick = {
                val webIntent = Intent(Intent.ACTION_VIEW, it.toUri())
                context.startActivity(webIntent)
            }) {
                Text("Play on Spotify")
            }
        }
    }
}
