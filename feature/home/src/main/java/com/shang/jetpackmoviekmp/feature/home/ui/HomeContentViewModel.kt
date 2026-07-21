package com.shang.jetpackmoviekmp.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shang.jetpackmoviekmp.core.ui.MovieCardData
import com.shang.jetpackmoviekmp.core.ui.asMovieCardResult
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.domain.usecase.GetHomeMovieListUseCase
import com.shang.jetpackmoviekmp.model.MovieGenreBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeContentViewModel(
    private val movieRepository: MovieRepository,
    getMovieGenreUseCase: GetHomeMovieListUseCase,
    movieGenre: MovieGenreBean.MovieGenre,
) : ViewModel() {

    val movieList =
        getMovieGenreUseCase(movieGenre.id.toString(), viewModelScope)

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
