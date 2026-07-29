package com.shang.jetpackmoviekmp.feature.history.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.shang.jetpackmoviekmp.feature.history.R
import com.shang.jetpackmoviekmp.model.MovieCardData
import com.shang.jetpackmoviekmp.model.MovieCardResult
import com.shang.jetpackmoviekmp.model.asMovieCardData
import org.koin.compose.viewmodel.koinViewModel

/**
 * 顯示觀看歷史，並依狀態切換空狀態或電影清單。
 *
 * @param viewModel 提供歷史畫面狀態與操作的 ViewModel。
 */
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = koinViewModel()) {
    val state by viewModel.historyMovies.collectAsState()

    when (state) {
        HistoryUiState.Empty -> HistoryEmptyScreen()
        is HistoryUiState.Success -> HistorySuccessScreen(
            historyList = (state as HistoryUiState.Success).historyList,
            onCollectClick = viewModel::toggleMovieCollect,
            onClearClick = viewModel::clearHistory,
        )
    }
}

/** 顯示沒有觀看紀錄時的提示內容。 */
@Composable
fun HistoryEmptyScreen() {
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
            text = stringResource(R.string.history_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * 顯示觀看歷史的網格清單與清空操作。
 *
 * @param historyList 要顯示的觀看紀錄清單。
 * @param onCollectClick 使用者點擊收藏按鈕時的回呼。
 * @param onClearClick 使用者點擊清空按鈕時的回呼。
 */
@Composable
fun HistorySuccessScreen(
    historyList: List<MovieCardResult>,
    onCollectClick: (MovieCardData) -> Unit,
    onClearClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.titleLarge,
            )
            TextButton(onClick = onClearClick) {
                Text(text = stringResource(R.string.history_clear))
            }
        }
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
            items(historyList) { movie ->
                MovieCard(
                    data = movie.asMovieCardData(),
                    onCollectClick = onCollectClick,
                )
            }
        }
    }
}
