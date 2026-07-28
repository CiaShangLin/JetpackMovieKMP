package com.shang.jetpackmoviekmp.feature.collect.ui

import com.shang.jetpackmoviekmp.model.MovieCardResult

/** 收藏頁的畫面狀態。 */
sealed class CollectUiState {

    /** 尚未訂閱資料或目前沒有可顯示的收藏。 */
    data object Empty : CollectUiState()

    /**
     * 收藏資料已取得。
     *
     * @property movieCollectList 目前的收藏電影清單。
     */
    data class Success(
        val movieCollectList: List<MovieCardResult>,
    ) : CollectUiState()
}
