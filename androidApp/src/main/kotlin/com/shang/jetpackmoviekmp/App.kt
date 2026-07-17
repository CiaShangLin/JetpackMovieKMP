package com.shang.jetpackmoviekmp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shang.jetpackmoviekmp.network.MovieDataSourceImpl

@Composable
fun App() {
    val impl = remember { MovieDataSourceImpl() }
    var text by remember { mutableStateOf("Loading...") }

    LaunchedEffect(Unit) {
        text = try {
            impl.getConfiguration().toString()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = text)
            }
        }
    }
}

@Preview
@Composable
private fun AppPreview() {
    App()
}
