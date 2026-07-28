package com.shang.jetpackmoviekmp.feature.collect.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.model.MovieCardData
import com.shang.jetpackmoviekmp.model.asMovieCardResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 管理收藏電影清單與取消收藏操作的 ViewModel。
 *
 * @property movieRepository 收藏資料的存取來源。
 */
class CollectViewModel(
    private val movieRepository: MovieRepository,
) : ViewModel() {

    /** 目前收藏清單對應的可觀察畫面狀態。 */
    val allMovieCollect = movieRepository.getAllMovieCollect()
        .map { movieCollectList ->
            CollectUiState.Success(movieCollectList)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CollectUiState.Empty,
        )

    /**
     * 移除使用者指定的收藏電影。
     *
     * @param data 使用者操作的電影卡片資料。
     */
    fun removeMovieCollect(data: MovieCardData) {
        viewModelScope.launch(Dispatchers.IO) {
            movieRepository.deleteMovieCollect(data.asMovieCardResult())
        }
    }
}
