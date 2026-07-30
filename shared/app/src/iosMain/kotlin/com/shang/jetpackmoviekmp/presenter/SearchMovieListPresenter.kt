package com.shang.jetpackmoviekmp.presenter

import androidx.paging.ItemSnapshotList
import com.shang.jetpackmoviekmp.domain.usecase.GetSearchMovieListUseCase
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
 * iOS 專用的搜尋電影清單分頁 Presenter。
 *
 * 每個實例只管理一個 trim 後的搜尋關鍵字，並持有獨立的 [CoroutineScope] 收集
 * [GetSearchMovieListUseCase] 回傳的分頁資料。Swift 呼叫端必須在不再使用時呼叫
 * [clear]，取消內部協程並停止該 query 的分頁 stream。
 */
class SearchMovieListPresenter(
    getSearchMovieListUseCase: GetSearchMovieListUseCase,
    query: String,
    ioDispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val pagingDataPresenter = SearchMoviePagingDataPresenter()

    init {
        scope.launch {
            getSearchMovieListUseCase(query.trim(), scope).collectLatest { pagingData ->
                pagingDataPresenter.collectFrom(pagingData)
            }
        }
    }

    /**
     * 存取指定 index 的電影項目，同時通知 Paging 3 依 `prefetchDistance` 決定
     * 是否要載入下一頁。
     */
    fun get(index: Int): MovieCardResult? = pagingDataPresenter.get(index)

    /** 目前已呈現清單的完整快照，供 Swift 端轉成畫面使用的陣列。 */
    fun snapshot(): ItemSnapshotList<MovieCardResult> = pagingDataPresenter.snapshot()

    /** 重試上一次失敗的載入請求，不會重建目前的 `PagingSource`。 */
    fun retry() = pagingDataPresenter.retry()

    /** 建立新一代 `PagingSource`，對應 Paging 3 的 refresh 操作。 */
    fun refresh() = pagingDataPresenter.refresh()

    /**
     * 觀察 refresh 與 append 的 loading、idle、error 狀態。
     *
     * AndroidX Paging 的型別會在內部轉換為 Swift 可互通的
     * [SearchMovieListLoadStates]。
     */
    val loadStateFlow: Flow<SearchMovieListLoadStates>
        get() = pagingDataPresenter.loadStateFlow.mapNotNull { combined ->
            combined?.let {
                SearchMovieListLoadStates(
                    refresh = it.refresh.toSearchMovieListLoadState(),
                    append = it.append.toSearchMovieListLoadState(),
                )
            }
        }

    /** 每次已呈現的清單內容更新時發出訊號，供 Swift 端重新讀取 [snapshot]。 */
    val onPagesUpdatedFlow: Flow<Unit> get() = pagingDataPresenter.onPagesUpdatedFlow

    /** 取消內部 [CoroutineScope]，停止收集此 query 的分頁資料。 */
    fun clear() {
        scope.cancel()
    }
}
