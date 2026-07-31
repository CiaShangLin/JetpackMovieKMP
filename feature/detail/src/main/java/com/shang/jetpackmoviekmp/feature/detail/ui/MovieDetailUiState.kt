package com.shang.jetpackmoviekmp.feature.detail.ui

import com.shang.jetpackmoviekmp.model.MovieDetailBean

/** 電影詳情主內容的載入狀態。 */
sealed interface MovieDetailUiState {
    /** 正在載入電影詳情。 */
    data object Loading : MovieDetailUiState

    /** 已成功取得電影詳情。 */
    data class Success(val movie: MovieDetailBean) : MovieDetailUiState

    /** 取得電影詳情失敗。 */
    data class Error(val throwable: Throwable) : MovieDetailUiState
}

/** 電影詳情頁中可獨立載入區塊的狀態。 */
sealed interface DetailSectionState<out T> {
    /** 正在載入區塊內容。 */
    data object Loading : DetailSectionState<Nothing>

    /** 已成功取得區塊內容。 */
    data class Success<T>(val data: T) : DetailSectionState<T>

    /** 取得區塊內容失敗。 */
    data class Error(val throwable: Throwable) : DetailSectionState<Nothing>
}
