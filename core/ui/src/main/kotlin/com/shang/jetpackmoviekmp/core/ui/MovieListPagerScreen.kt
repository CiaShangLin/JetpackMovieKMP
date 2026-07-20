package com.shang.jetpackmoviekmp.core.ui

import androidx.compose.runtime.Composable
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems

/**
 * 根據 Paging 載入狀態顯示 loading、error 或主要內容。
 *
 * @param T 分頁資料型別。
 * @param pager Paging Compose 的資料容器。
 * @param content 載入完成後要顯示的內容。
 */
@Composable
fun <T : Any> MovieListPagerScreen(pager: LazyPagingItems<T>, content: @Composable () -> Unit) {
    when {
        pager.loadState.refresh is LoadState.Loading -> LoadingScreen()
        pager.loadState.refresh is LoadState.Error -> ErrorScreen()
        pager.loadState.append is LoadState.Loading -> LoadingScreen()
        pager.loadState.append is LoadState.Error -> ErrorScreen()
        pager.loadState.prepend is LoadState.Loading -> LoadingScreen()
        pager.loadState.prepend is LoadState.Error -> ErrorScreen()
        else -> content()
    }
}
