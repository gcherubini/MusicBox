package com.gcherubini.musicbox.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gcherubini.musicbox.R
import com.gcherubini.musicbox.ui.theme.MarineGreen

private const val WELCOME_TEXT = "Welcome to Music Box!"

@Composable
fun WelcomeScreen(modifier: Modifier = Modifier, onExploreClick: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MarineGreen),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.music_box), // substitua pelo nome correto do seu drawable
            contentDescription = "Logo do MusicBox",
            modifier = Modifier.size(120.dp) // ajuste o tamanho como preferir
        )
        Spacer(modifier = Modifier.height(16.dp)) // espaço entre imagem e texto
        Text(
            text = WELCOME_TEXT,
            style = MaterialTheme.typography.headlineMedium // pode customizar como quiser
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onExploreClick) {
            Text(text = "Explore")
        }
    }
}