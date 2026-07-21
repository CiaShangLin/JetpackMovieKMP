package com.shang.jetpackmoviekmp.feature.home.ui

import com.shang.jetpackmoviekmp.model.MovieGenreBean

sealed class HomeUiState {
    data object Loading : HomeUiState()

    data class Success(
        val movieGenres: MovieGenreBean,
    ) : HomeUiState()

    data class Error(val throwable: Throwable?) : HomeUiState()
}
