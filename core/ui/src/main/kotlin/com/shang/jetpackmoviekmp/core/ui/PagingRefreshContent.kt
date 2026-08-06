package com.shang.jetpackmoviekmp.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems

/**
 * 依 [LazyPagingItems.loadState] 的 `refresh` 狀態整頁顯示 Loading、Error 或實際內容。
 *
 * @param T 分頁資料型別。
 * @param pagingItems Paging Compose 的資料容器。
 * @param onRetry 使用者於 Error 畫面觸發重試時呼叫。
 * @param modifier 套用在整頁狀態畫面上的修飾符。
 * @param content 載入完成後要顯示的實際內容。
 */
@Composable
fun <T : Any> PagingRefreshContent(
    pagingItems: LazyPagingItems<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when (val refreshState = pagingItems.loadState.refresh) {
        is LoadState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingScreen()
            }
        }

        is LoadState.Error -> {
            ErrorScreen(
                modifier = modifier,
                throwable = refreshState.error,
                onRetry = onRetry,
            )
        }

        is LoadState.NotLoading -> content()
    }
}
