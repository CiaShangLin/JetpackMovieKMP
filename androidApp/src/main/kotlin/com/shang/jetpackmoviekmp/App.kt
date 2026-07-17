package com.shang.jetpackmoviekmp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shang.jetpackmoviekmp.datastore.UserPreferenceDataSource
import com.shang.jetpackmoviekmp.model.LanguageMode
import com.shang.jetpackmoviekmp.network.datasource.MovieDataSource
import com.shang.jetpackmoviekmp.network.model.NetworkResponse
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * App 根 composable，暫時作為 datastore + network 整合的簡易驗證畫面。
 *
 * 啟動時呼叫一次 [MovieDataSource.getConfiguration]；並提供一顆 button，
 * 讓使用者在繁體中文／英文間切換語言，切換後會透過 [UserPreferenceDataSource]
 * 持久化選取的語言，再重新呼叫一次 network，藉此驗證「datastore 語言設定 → network
 * request language」這條路徑已經接通。此畫面刻意維持簡單，不是正式的 settings 畫面。
 *
 * @param movieDataSource 用來驗證 network 呼叫的資料來源，預設由 Koin 注入。
 * @param userPreferenceDataSource 用來持久化語言選擇的資料來源，預設由 Koin 注入。
 */
@Composable
fun App(
    movieDataSource: MovieDataSource = koinInject(),
    userPreferenceDataSource: UserPreferenceDataSource = koinInject(),
) {
    AppContent(
        movieDataSource = movieDataSource,
        onSetLanguageMode = { userPreferenceDataSource.setLanguageMode(it) },
    )
}

/** [App] 的實作內容，把語言持久化行為抽成 [onSetLanguageMode] 回呼，方便 Preview 使用假資料。 */
@Composable
private fun AppContent(
    movieDataSource: MovieDataSource,
    onSetLanguageMode: suspend (LanguageMode) -> Unit,
) {
    var text by remember { mutableStateOf("Loading...") }
    var currentLanguageMode by remember { mutableStateOf(LanguageMode.TRADITIONAL_CHINESE) }
    val scope = rememberCoroutineScope()

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
                Button(onClick = {
                    val nextLanguageMode = if (currentLanguageMode == LanguageMode.ENGLISH) {
                        LanguageMode.TRADITIONAL_CHINESE
                    } else {
                        LanguageMode.ENGLISH
                    }
                    scope.launch {
                        text = try {
                            onSetLanguageMode(nextLanguageMode)
                            currentLanguageMode = nextLanguageMode
                            "language=$nextLanguageMode result=${movieDataSource.getConfiguration()}"
                        } catch (e: Exception) {
                            "language=$nextLanguageMode error=${e.message}"
                        }
                    }
                }) {
                    Text(text = "Toggle language (current: $currentLanguageMode)")
                }
                Text(text = text)
            }
        }
    }
}

@Preview
@Composable
private fun AppPreview() {
    AppContent(
        movieDataSource = object : MovieDataSource {
            override suspend fun getConfiguration() = NetworkResponse(code = 200, data = null)
            override suspend fun getMovieGenres() = NetworkResponse(code = 200, data = null)
            override suspend fun getDiscoverMovie(withGenres: String, page: Int) = NetworkResponse(code = 200, data = null)
            override suspend fun getMovieSearch(query: String, page: Int) = NetworkResponse(code = 200, data = null)
            override suspend fun getMovieDetail(id: Int) = NetworkResponse(code = 200, data = null)
            override suspend fun getMovieRecommendations(id: Int) = NetworkResponse(code = 200, data = null)
            override suspend fun getMovieActor(id: Int) = NetworkResponse(code = 200, data = null)
        },
        onSetLanguageMode = {},
    )
}
