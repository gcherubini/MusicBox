package com.gcherubini.musicbox.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
fun DetailScreen(
    music: Music,
    modifier: Modifier = Modifier,
    navController: NavController,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(music.title) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
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
                    .size(220.dp)
                    .clip(RoundedCornerShape(8.dp)) // cantos arredondados
                    .padding(end = 12.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))  // Espaço maior entre imagem e título
            Text(text = "Artist: ${music.artist}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Label: ${music.label}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Genre: ${music.genre}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Release Date: ${music.releaseDate}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

