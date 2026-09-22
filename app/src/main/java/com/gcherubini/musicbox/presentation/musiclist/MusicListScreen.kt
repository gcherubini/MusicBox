package com.gcherubini.musicbox.presentation.musiclist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gcherubini.musicbox.R
import com.gcherubini.musicbox.presentation.model.MusicUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicListScreen(
    viewModel: MusicListViewModel,
    navController: NavController,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MusicListEffect.OpenDetail ->
                    navController.navigate(
                        com.gcherubini.musicbox.presentation.navigation.Screen.MusicDetail
                            .createRoute(effect.id)
                    )
            }
        }
    }

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
        when (val s = uiState) {
            is MusicListUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is MusicListUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.onIntent(MusicListIntent.Retry) }) {
                            Text("Tentar novamente")
                        }
                    }
                }
            }
            is MusicListUiState.Success -> {
                LazyColumn(
                    contentPadding = padding,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(s.musics, key = { it.id }) { music ->
                        MusicItem(music) {
                            viewModel.onIntent(MusicListIntent.MusicClicked(music.id))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MusicItem(music: MusicUiModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable { onClick() }
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
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp))
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
