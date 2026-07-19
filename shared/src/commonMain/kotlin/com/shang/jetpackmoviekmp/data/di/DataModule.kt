package com.shang.jetpackmoviekmp.data.di

import com.shang.jetpackmoviekmp.common.di.CommonDispatcher
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.data.repository.MovieRepositoryImpl
import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
import com.shang.jetpackmoviekmp.data.repository.UserDataRepositoryImpl
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * 提供電影資料整合層依賴：[MovieRepository]、[UserDataRepository]。
 *
 * 依賴既有 `networkModule`／`databaseModule`／`datastoreModule` 提供的
 * `MovieDataSource`／DAO／`UserPreferenceDataSource`，以及 `commonModule()`
 * 提供的 [CommonDispatcher.IO] qualified [CoroutineDispatcher]，安裝此 module
 * 時必須一併安裝上述四個 module。
 */
fun dataModule() = module {
    single<MovieRepository> {
        MovieRepositoryImpl(
            movieDataSource = get(),
            movieCollectDao = get(),
            movieHistoryDao = get(),
            ioDispatcher = get(qualifier = named(CommonDispatcher.IO)),
        )
    }
    single<UserDataRepository> {
        UserDataRepositoryImpl(userPreferenceDataSource = get())
    }
}
