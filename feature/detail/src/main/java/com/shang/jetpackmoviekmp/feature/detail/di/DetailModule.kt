package com.shang.jetpackmoviekmp.feature.detail.di

import com.shang.jetpackmoviekmp.feature.detail.ui.MovieDetailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** 提供電影詳情功能所需依賴的 Koin 模組。 */
fun detailModule() = module {
    viewModel { params ->
        MovieDetailViewModel(
            movieRepository = get(),
            userDataRepository = get(),
            getMovieDetailUseCase = get(),
            getMovieRecommendUseCase = get(),
            movieId = params.get(),
        )
    }
}
