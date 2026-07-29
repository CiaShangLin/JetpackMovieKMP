package com.shang.jetpackmoviekmp.feature.history.ui

import com.shang.jetpackmoviekmp.model.MovieCardResult

/** 歷史頁的畫面狀態。 */
sealed class HistoryUiState {

    /** 尚未訂閱資料或目前沒有可顯示的觀看紀錄。 */
    data object Empty : HistoryUiState()

    /**
     * 觀看紀錄已取得。
     *
     * @property historyList 目前可顯示的觀看紀錄清單。
     */
    data class Success(
        val historyList: List<MovieCardResult>,
    ) : HistoryUiState()
}
