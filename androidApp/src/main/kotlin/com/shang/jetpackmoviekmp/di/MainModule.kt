package com.shang.jetpackmoviekmp.di

import com.shang.jetpackmoviekmp.ui.MainViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * 提供 [MainViewModel] 的 Koin module，於 `JetpackMovieApplication` 啟動時載入。
 *
 * 依賴既有 `domainModule()` 提供的 `GetConfigurationUseCase`、`dataModule()` 提供的
 * `UserDataRepository`，安裝此 module 時必須一併安裝上述兩個 module。
 */
fun mainModule() = module {
    viewModel {
        MainViewModel(
            getConfigurationUseCase = get(),
            userDataRepository = get(),
        )
    }
}
