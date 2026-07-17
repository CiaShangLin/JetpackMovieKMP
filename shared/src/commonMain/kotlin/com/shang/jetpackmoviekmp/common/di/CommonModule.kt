package com.shang.jetpackmoviekmp.common.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/**
 * 提供跨層共用的 DI 依賴，目前只有一個 application-level 的 [CoroutineScope]，
 * 供 `datastore`／`network` 底下需要背景收集 flow 的 provider（例如
 * `DatastoreLanguageProvider`、`DatastoreBaseHostUrlProvider`）使用。
 *
 * 安裝任何依賴這個 scope 的 module（例如 `datastoreModule`）時，
 * 都必須一併安裝 `commonModule()`。
 */
fun commonModule() = module {
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
}
