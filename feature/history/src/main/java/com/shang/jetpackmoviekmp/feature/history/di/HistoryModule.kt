package com.shang.jetpackmoviekmp.feature.history.di

import com.shang.jetpackmoviekmp.feature.history.ui.HistoryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * 提供歷史頁 [HistoryViewModel] 的 Koin module。
 *
 * @return 歷史 feature 所需的 Koin module。
 */
fun historyModule() = module {
    viewModel {
        HistoryViewModel(
            getHistoryMovieListUseCase = get(),
            movieRepository = get(),
        )
    }
}
