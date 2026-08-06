package com.shang.jetpackmoviekmp.feature.collect.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shang.jetpackmoviekmp.core.designsystem.component.JMLazyVerticalGrid
import com.shang.jetpackmoviekmp.core.ui.MovieCard
import com.shang.jetpackmoviekmp.feature.collect.R
import com.shang.jetpackmoviekmp.model.MovieCardData
import com.shang.jetpackmoviekmp.model.MovieCardResult
import com.shang.jetpackmoviekmp.model.asMovieCardData
import org.koin.compose.viewmodel.koinViewModel

/**
 * 顯示收藏頁，並依收藏資料狀態切換空狀態或電影清單。
 *
 * @param viewModel 提供收藏畫面狀態與操作的 ViewModel。
 * @param onMovieClick 使用者點擊收藏電影卡片時的回呼。
 */
@Composable
fun CollectScreen(viewModel: CollectViewModel = koinViewModel(), onMovieClick: (MovieCardData) -> Unit) {
    val state by viewModel.allMovieCollect.collectAsState()

    when (state) {
        CollectUiState.Empty -> CollectEmptyScreen()
        is CollectUiState.Success -> {
            val movies = (state as CollectUiState.Success).movieCollectList
            if (movies.isEmpty()) {
                CollectEmptyScreen()
            } else {
                CollectSuccessScreen(
                    movieCollectList = movies,
                    onMovieClick = onMovieClick,
                    onCollectClick = viewModel::removeMovieCollect,
                )
            }
        }
    }
}

/** 顯示沒有收藏電影時的提示內容。 */
@Composable
fun CollectEmptyScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.icon_empty),
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .padding(bottom = 16.dp),
        )
        Text(
            text = stringResource(R.string.collect_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * 顯示收藏電影的網格清單。
 *
 * @param movieCollectList 要顯示的收藏電影。
 * @param onMovieClick 使用者點擊電影卡片時的回呼。
 * @param onCollectClick 使用者點擊收藏按鈕時的回呼。
 */
@Composable
fun CollectSuccessScreen(
    movieCollectList: List<MovieCardResult>,
    onMovieClick: (MovieCardData) -> Unit,
    onCollectClick: (MovieCardData) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.collect_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurface,
            thickness = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        )
        JMLazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(movieCollectList, key = { it.id }) { movie ->
                MovieCard(
                    data = movie.asMovieCardData(),
                    onMovieClick = onMovieClick,
                    onCollectClick = onCollectClick,
                )
            }
        }
    }
}
