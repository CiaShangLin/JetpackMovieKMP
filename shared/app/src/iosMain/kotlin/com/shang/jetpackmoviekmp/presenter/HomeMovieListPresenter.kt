package com.shang.jetpackmoviekmp.presenter

import androidx.paging.CombinedLoadStates
import androidx.paging.ItemSnapshotList
import com.shang.jetpackmoviekmp.domain.usecase.GetHomeMovieListUseCase
import com.shang.jetpackmoviekmp.model.MovieCardResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
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

    /** 觀察 refresh／prepend／append 的載入中／失敗／完成狀態。 */
    val loadStateFlow: StateFlow<CombinedLoadStates?> get() = pagingDataPresenter.loadStateFlow

    /** 每次已呈現的清單內容更新時發出訊號，供 Swift 端觸發重新讀取 [snapshot]。 */
    val onPagesUpdatedFlow: Flow<Unit> get() = pagingDataPresenter.onPagesUpdatedFlow

    /** 取消內部持有的 [CoroutineScope]；呼叫端須在畫面消失時呼叫，避免協程洩漏。 */
    fun clear() {
        scope.cancel()
    }
}
