package com.shang.jetpackmoviekmp.feature.history.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.domain.usecase.GetHistoryMovieListUseCase
import com.shang.jetpackmoviekmp.model.MovieCardData
import com.shang.jetpackmoviekmp.model.asMovieCardResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 管理觀看歷史、收藏切換與清空操作的 ViewModel。
 *
 * @property getHistoryMovieListUseCase 提供已合併收藏狀態的觀看紀錄清單。
 * @property movieRepository 執行收藏與觀看歷史異動的資料來源。
 */
class HistoryViewModel(
    private val getHistoryMovieListUseCase: GetHistoryMovieListUseCase,
    private val movieRepository: MovieRepository,
) : ViewModel() {

    /** 目前觀看歷史對應的可觀察畫面狀態。 */
    val historyMovies = getHistoryMovieListUseCase()
        .map { historyList ->
            if (historyList.isEmpty()) {
                HistoryUiState.Empty
            } else {
                HistoryUiState.Success(historyList)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState.Empty,
        )

    /** 依目前收藏狀態切換指定電影的收藏。 */
    fun toggleMovieCollect(data: MovieCardData) {
        viewModelScope.launch(Dispatchers.IO) {
            if (data.movieCardIsCollect) {
                movieRepository.deleteMovieCollect(data.asMovieCardResult())
            } else {
                movieRepository.insertMovieCollect(data.asMovieCardResult())
            }
        }
    }

    /** 清空所有觀看歷史。 */
    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            movieRepository.deleteAllMovieHistory()
        }
    }
}
