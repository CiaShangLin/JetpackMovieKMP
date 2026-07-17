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
import com.shang.jetpackmoviekmp.network.datasource.MovieDataSource
import com.shang.jetpackmoviekmp.network.model.NetworkResponse
import org.koin.compose.koinInject

@Composable
fun App(movieDataSource: MovieDataSource = koinInject()) {
    var text by remember { mutableStateOf("Loading...") }

    LaunchedEffect(Unit) {
        text = try {
            movieDataSource.getConfiguration().toString()
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
    App(
        movieDataSource = object : MovieDataSource {
            override suspend fun getConfiguration() = NetworkResponse(code = 200, data = null)
            override suspend fun getMovieGenres() = NetworkResponse(code = 200, data = null)
            override suspend fun getDiscoverMovie(withGenres: String, page: Int) = NetworkResponse(code = 200, data = null)
            override suspend fun getMovieSearch(query: String, page: Int) = NetworkResponse(code = 200, data = null)
            override suspend fun getMovieDetail(id: Int) = NetworkResponse(code = 200, data = null)
            override suspend fun getMovieRecommendations(id: Int) = NetworkResponse(code = 200, data = null)
            override suspend fun getMovieActor(id: Int) = NetworkResponse(code = 200, data = null)
        },
    )
}
