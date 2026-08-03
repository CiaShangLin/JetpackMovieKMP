package com.shang.jetpackmoviekmp.feature.detail.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shang.jetpackmoviekmp.common.AppResult
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.domain.usecase.GetMovieDetailUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetMovieRecommendUseCase
import com.shang.jetpackmoviekmp.model.MovieCardData
import com.shang.jetpackmoviekmp.model.asMovieCardResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 管理電影詳情、收藏狀態、演員與推薦電影資料的 UI 狀態。
 *
 * @param movieRepository 電影資料與收藏資料來源。
 * @param getMovieDetailUseCase 取得單一電影詳情的用例。
 * @param getMovieRecommendUseCase 取得電影推薦清單的用例。
 * @param movieId 目前顯示的電影識別碼。
 */
class MovieDetailViewModel(
    private val movieRepository: MovieRepository,
    private val getMovieDetailUseCase: GetMovieDetailUseCase,
    private val getMovieRecommendUseCase: GetMovieRecommendUseCase,
    private val movieId: Int,
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)

    /** 電影主詳情的載入狀態；失敗後可由 [retryMovieDetail] 重新載入。 */
    @OptIn(ExperimentalCoroutinesApi::class)
    val movieDetail = retryTrigger
        .flatMapLatest { getMovieDetailUseCase(movieId) }
        .map { result ->
            when (result) {
                is AppResult.Success -> MovieDetailUiState.Success(result.data)
                is AppResult.Failure -> MovieDetailUiState.Error(result.error)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MovieDetailUiState.Loading,
        )

    /** 目前電影是否已收藏。 */
    val movieCollect = movieRepository.getMovieCollectEntityById(movieId)
        .map { it?.isCollect == true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    /** 推薦電影區塊的載入狀態；失敗時由畫面隱藏該區塊。 */
    val movieRecommendations: StateFlow<DetailSectionState<List<com.shang.jetpackmoviekmp.model.MovieCardResult>>> =
        getMovieRecommendUseCase(movieId)
            .map { result ->
                when (result) {
                    is AppResult.Success -> DetailSectionState.Success(result.data)
                    is AppResult.Failure -> DetailSectionState.Error(result.error)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DetailSectionState.Loading,
            )

    /** 主要演員區塊的載入狀態；失敗時由畫面隱藏該區塊。 */
    val movieActors: StateFlow<DetailSectionState<List<com.shang.jetpackmoviekmp.model.MovieCastAndCrewBean.Cast>>> =
        movieRepository.getMovieActor(movieId)
            .map { result ->
                when (result) {
                    is AppResult.Success -> DetailSectionState.Success(result.data.cast)
                    is AppResult.Failure -> DetailSectionState.Error(result.error)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DetailSectionState.Loading,
            )

    /** 觸發電影主詳情重新載入。 */
    fun retryMovieDetail() {
        retryTrigger.value++
    }

    /**
     * 依目前收藏狀態新增或移除指定電影。
     *
     * @param data 要切換收藏狀態的電影卡片資料。
     */
    fun toggleCollect(data: MovieCardData) {
        viewModelScope.launch {
            if (data.movieCardIsCollect) {
                movieRepository.deleteMovieCollect(data.asMovieCardResult())
            } else {
                movieRepository.insertMovieCollect(data.asMovieCardResult())
            }
        }
    }
}
