package com.shang.jetpackmoviekmp.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState

/**
 * 供 `LazyGridScope.item {}`（或等效 Lazy 容器 scope）呼叫的行內分頁載入狀態 Footer。
 *
 * 依傳入的單一 [LoadState]（呼叫端傳入 `append` 或 `prepend` 方向）顯示行內 Loading、
 * 行內 Error（含重試）或到底提示，不遮蔽已顯示的清單內容。
 *
 * @param loadState 呼叫端傳入的 `loadState.append` 或 `loadState.prepend`。
 * @param onRetry 使用者於行內 Error 觸發重試時呼叫。
 * @param modifier 套用在 Footer 上的修飾符。
 */
@Composable
fun PagingLoadStateFooter(
    loadState: LoadState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (loadState) {
        is LoadState.Loading -> {
            Box(
                modifier = modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                LoadingScreen(size = 40.dp)
            }
        }

        is LoadState.Error -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(loadState.error.toErrorText()),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.retry_button_text))
                }
            }
        }

        is LoadState.NotLoading -> {
            if (loadState.endOfPaginationReached) {
                Text(
                    text = stringResource(R.string.paging_no_more_data_text),
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
