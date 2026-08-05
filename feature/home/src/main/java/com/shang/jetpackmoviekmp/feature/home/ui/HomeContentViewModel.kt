package com.shang.jetpackmoviekmp.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
import com.shang.jetpackmoviekmp.domain.usecase.GetHomeMovieListUseCase
import com.shang.jetpackmoviekmp.model.MovieCardData
import com.shang.jetpackmoviekmp.model.MovieGenreBean
import com.shang.jetpackmoviekmp.model.asMovieCardResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 管理首頁指定分類的電影分頁片單與收藏切換。
 *
 * @property movieRepository 提供收藏資料的新增與刪除操作。
 * @property userDataRepository 提供語言設定變化訊號，語言切換時從第一頁重新載入片單。
 * @param getMovieGenreUseCase 建立會同步收藏狀態的分類電影分頁資料流。
 * @param movieGenre 目前顯示的電影分類。
 */
class HomeContentViewModel(
    private val movieRepository: MovieRepository,
    private val userDataRepository: UserDataRepository,
    getMovieGenreUseCase: GetHomeMovieListUseCase,
    movieGenre: MovieGenreBean.MovieGenre,
) : ViewModel() {

    /** 目前分類的電影分頁資料；`languageMode` 變化時捨棄舊分頁快取，從第一頁重新載入。 */
    @OptIn(ExperimentalCoroutinesApi::class)
    val movieList =
        userDataRepository.userData.map { it.languageMode }.distinctUntilChanged()
            .flatMapLatest {
                getMovieGenreUseCase(movieGenre.id.toString(), viewModelScope)
            }

    /**
     * 依電影目前的收藏狀態新增或刪除收藏資料。
     *
     * @param data 使用者操作的電影卡片資料。
     */
    fun toggleMovieCollectStatus(data: MovieCardData) {
        viewModelScope.launch(Dispatchers.IO) {
            if (data.movieCardIsCollect) {
                movieRepository.deleteMovieCollect(data.asMovieCardResult())
            } else {
                movieRepository.insertMovieCollect(data.asMovieCardResult())
            }
        }
    }
}
