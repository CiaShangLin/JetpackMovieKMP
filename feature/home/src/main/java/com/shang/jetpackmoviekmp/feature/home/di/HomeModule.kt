package com.shang.jetpackmoviekmp.feature.home.di

import com.shang.jetpackmoviekmp.feature.home.ui.HomeContentViewModel
import com.shang.jetpackmoviekmp.feature.home.ui.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * 提供 `feature/home` 的 ViewModel 依賴：[HomeViewModel]、[HomeContentViewModel]。
 *
 * 依賴既有 `dataModule()` 提供的 `MovieRepository`／`UserDataRepository`、`domainModule()`
 * 提供的 `GetHomeMovieListUseCase`，安裝此 module 時必須一併安裝上述兩個 module。
 *
 * [HomeContentViewModel] 建構時需要的 `movieGenre` 只有在畫面渲染時才知道，透過 Koin
 * 參數化注入（`parametersOf`）於呼叫端傳入，取代來源專案原本的 Hilt Assisted Inject。
 */
fun homeModule() = module {
    viewModel {
        HomeViewModel(
            userDataRepository = get(),
            movieRepository = get(),
        )
    }
    viewModel { params ->
        HomeContentViewModel(
            movieRepository = get(),
            getMovieGenreUseCase = get(),
            movieGenre = params.get(),
        )
    }
}
