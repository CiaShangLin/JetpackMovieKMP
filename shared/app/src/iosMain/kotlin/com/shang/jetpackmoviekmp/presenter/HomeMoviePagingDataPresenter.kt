package com.shang.jetpackmoviekmp.presenter

import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import com.shang.jetpackmoviekmp.model.MovieCardResult

/**
 * [HomeMovieListPresenter] 內部使用的 [PagingDataPresenter] 具體實作。
 *
 * 呈現的清單、`LoadState` 等狀態皆由父類別內部的 `pageStore` 管理，此處不需要
 * 自行儲存清單；[presentPagingDataEvent] 只是 Paging 內部事件的通知 hook，
 * 對外可觀察的狀態變化已經由父類別的 `loadStateFlow`／`onPagesUpdatedFlow` 提供。
 */
internal class HomeMoviePagingDataPresenter : PagingDataPresenter<MovieCardResult>() {
    override suspend fun presentPagingDataEvent(event: PagingDataEvent<MovieCardResult>) = Unit
}
