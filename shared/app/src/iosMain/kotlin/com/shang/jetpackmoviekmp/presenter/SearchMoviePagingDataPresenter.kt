package com.shang.jetpackmoviekmp.presenter

import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import com.shang.jetpackmoviekmp.model.MovieCardResult

/**
 * [SearchMovieListPresenter] 內部使用的 [PagingDataPresenter] 具體實作。
 *
 * 呈現的清單與 `LoadState` 等狀態皆由父類別內部的 `pageStore` 管理；
 * [presentPagingDataEvent] 僅作為 Paging 事件的必要 hook。
 */
internal class SearchMoviePagingDataPresenter : PagingDataPresenter<MovieCardResult>() {
    override suspend fun presentPagingDataEvent(event: PagingDataEvent<MovieCardResult>) = Unit
}
