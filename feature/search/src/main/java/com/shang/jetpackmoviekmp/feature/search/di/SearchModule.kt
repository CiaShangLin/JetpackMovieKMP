package com.shang.jetpackmoviekmp.feature.search.di

import com.shang.jetpackmoviekmp.feature.search.ui.SearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** 提供搜尋頁 [SearchViewModel] 的 Koin module。 */
fun searchModule() = module {
    viewModel {
        SearchViewModel(
            movieRepository = get(),
            userDataRepository = get(),
            getSearchMovieListUseCase = get(),
        )
    }
}
