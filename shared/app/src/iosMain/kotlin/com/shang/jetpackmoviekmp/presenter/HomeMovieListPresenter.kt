package com.shang.jetpackmoviekmp.presenter

import androidx.paging.ItemSnapshotList
import androidx.paging.LoadState
import com.shang.jetpackmoviekmp.domain.usecase.GetHomeMovieListUseCase
import com.shang.jetpackmoviekmp.model.MovieCardResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

/**
 * iOS 專用的首頁電影清單分頁 Presenter。
 *
 * iOS 沒有 Android `viewModelScope` 這種呼叫端天然持有的協程作用域，也無法使用
 * Compose 的 `collectAsLazyPagingItems()`；本類別內部自行建立 [CoroutineScope]
 * 提供給既有 [GetHomeMovieListUseCase] 做 `cachedIn`，並用同一個 scope 持續收集
 * 分頁資料餵給內部的 [HomeMoviePagingDataPresenter]，藉此沿用 AndroidX Paging 3
 * 內建的分頁載入、`prefetchDistance`、`retry`／`refresh` 語意。
 *
 * 呼叫端（Swift）必須在畫面消失時呼叫 [clear]，取消內部協程，避免洩漏。
 */
class HomeMovieListPresenter(
    getHomeMovieListUseCase: GetHomeMovieListUseCase,
    withGenres: String,
    ioDispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val pagingDataPresenter = HomeMoviePagingDataPresenter()

    init {
        scope.launch {
            getHomeMovieListUseCase(withGenres, scope).collectLatest { pagingData ->
                pagingDataPresenter.collectFrom(pagingData)
            }
        }
    }

    /**
     * 存取指定 index 的電影項目，同時通知 Paging 3 依 `prefetchDistance` 判斷
     * 是否需要載入下一頁（比照 Compose `LazyPagingItems` 存取 item 的行為）。
     */
    fun get(index: Int): MovieCardResult? = pagingDataPresenter.get(index)

    /** 目前已呈現清單的完整快照，供 Swift 端轉成畫面用的陣列。 */
    fun snapshot(): ItemSnapshotList<MovieCardResult> = pagingDataPresenter.snapshot()

    /** 重試上一次失敗的載入請求，不會重建 `PagingSource`。 */
    fun retry() = pagingDataPresenter.retry()

    /** 建立新一代 `PagingSource`，對應下拉刷新。 */
    fun refresh() = pagingDataPresenter.refresh()

    /**
     * 觀察 refresh／append 的載入中／失敗／完成狀態。
     *
     * 內部把 `androidx.paging.LoadState`／`CombinedLoadStates` 轉成 [HomeMovieListLoadStates]
     * 再暴露出去，因為前者是第三方依賴型別，未列在 iOS framework 的 `export()` 清單內，
     * 無法直接跨到 Swift 使用（詳見 [HomeMovieListLoadState] 的 KDoc）。
     */
    val loadStateFlow: Flow<HomeMovieListLoadStates>
        get() = pagingDataPresenter.loadStateFlow.mapNotNull { combined ->
            combined?.let {
                HomeMovieListLoadStates(
                    refresh = it.refresh.toHomeMovieListLoadState(),
                    append = it.append.toHomeMovieListLoadState(),
                )
            }
        }

    /** 每次已呈現的清單內容更新時發出訊號，供 Swift 端觸發重新讀取 [snapshot]。 */
    val onPagesUpdatedFlow: Flow<Unit> get() = pagingDataPresenter.onPagesUpdatedFlow

    /** 取消內部持有的 [CoroutineScope]；呼叫端須在畫面消失時呼叫，避免協程洩漏。 */
    fun clear() {
        scope.cancel()
    }
}

private fun LoadState.toHomeMovieListLoadState(): HomeMovieListLoadState = when (this) {
    is LoadState.Loading -> HomeMovieListLoadState.Loading
    is LoadState.NotLoading -> HomeMovieListLoadState.Idle
    is LoadState.Error -> HomeMovieListLoadState.Error(message = error.message ?: "發生未知錯誤")
}
