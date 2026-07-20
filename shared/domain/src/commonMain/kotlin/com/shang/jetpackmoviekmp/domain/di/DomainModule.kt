package com.shang.jetpackmoviekmp.domain.di

import com.shang.jetpackmoviekmp.common.di.CommonDispatcher
import com.shang.jetpackmoviekmp.domain.usecase.GetConfigurationUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetHistoryMovieListUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetHomeMovieListUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetMovieDetailUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetMovieRecommendUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * 提供電影 UseCase 層依賴：5 個整合 `MovieRepository`／`UserDataRepository` 的 UseCase。
 *
 * 依賴既有 `dataModule()` 提供的 `MovieRepository`／`UserDataRepository`，以及
 * `commonModule()` 提供的 [CommonDispatcher.IO] qualified `CoroutineDispatcher`，
 * 安裝此 module 時必須一併安裝上述兩個 module。
 *
 * UseCase 皆用 `factory`（而非 `single`）綁定：本身沒有內部可變狀態，每次注入
 * 產生新實例即可，不需要在多個呼叫端之間共用同一個物件。
 */
fun domainModule() = module {
    factory {
        GetConfigurationUseCase(
            movieRepository = get(),
            userDataRepository = get(),
            ioDispatcher = get(qualifier = named(CommonDispatcher.IO)),
        )
    }
    factory {
        GetHistoryMovieListUseCase(
            movieRepository = get(),
            ioDispatcher = get(qualifier = named(CommonDispatcher.IO)),
        )
    }
    factory {
        GetHomeMovieListUseCase(
            movieRepository = get(),
            ioDispatcher = get(qualifier = named(CommonDispatcher.IO)),
        )
    }
    factory {
        GetMovieDetailUseCase(
            movieRepository = get(),
            ioDispatcher = get(qualifier = named(CommonDispatcher.IO)),
        )
    }
    factory {
        GetMovieRecommendUseCase(
            movieRepository = get(),
            ioDispatcher = get(qualifier = named(CommonDispatcher.IO)),
        )
    }
}
