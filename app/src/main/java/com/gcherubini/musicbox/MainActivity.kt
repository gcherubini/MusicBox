package com.gcherubini.musicbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gcherubini.musicbox.ui.theme.MusicBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WelcomeScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

private const val WELCOME_TEXT = "Welcome to Music Box!"
private val MarineGreen = Color(0xFF3DDC84)

@Composable
fun WelcomeScreen(modifier: Modifier = Modifier) {
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

        Button(onClick = {
            // Ação ao clicar no botão (ex: navegar para outra tela)
        }) {
            Text(text = "Explore")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MusicBoxTheme {
        WelcomeScreen()
    }
}