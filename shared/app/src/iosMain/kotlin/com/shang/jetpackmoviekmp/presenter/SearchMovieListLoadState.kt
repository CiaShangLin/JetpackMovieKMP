package com.shang.jetpackmoviekmp.presenter

import androidx.paging.LoadState

/**
 * [SearchMovieListPresenter] 對外暴露的載入狀態。
 *
 * 這是提供 Swift 使用的自有型別；AndroidX Paging 的 [LoadState] 不會透過 iOS
 * framework 的公開 API 暴露。
 */
sealed interface SearchMovieListLoadState {
    data object Idle : SearchMovieListLoadState
    data object Loading : SearchMovieListLoadState
    data class Error(val message: String) : SearchMovieListLoadState
}

/** [SearchMovieListPresenter.loadStateFlow] 對外暴露的整體載入狀態。 */
data class SearchMovieListLoadStates(
    val refresh: SearchMovieListLoadState,
    val append: SearchMovieListLoadState,
)

internal fun LoadState.toSearchMovieListLoadState(): SearchMovieListLoadState = when (this) {
    is LoadState.Loading -> SearchMovieListLoadState.Loading
    is LoadState.NotLoading -> SearchMovieListLoadState.Idle
    is LoadState.Error -> SearchMovieListLoadState.Error(message = error.message ?: "發生未知錯誤")
}
