package com.shang.jetpackmoviekmp.common.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * 跨層共用 [CoroutineDispatcher] 的 qualifier，比照參考專案 Hilt 版本的
 * `CommonDispatcher` 限定注入哪個 dispatcher。目前只有 `data` 層需要 [IO]，
 * 其餘成員待有實際消費者時再新增。
 */
enum class CommonDispatcher { IO, }

/**
 * 提供跨層共用的 DI 依賴：application-level 的 [CoroutineScope]（供
 * `datastore`／`network` 底下需要背景收集 flow 的 provider，例如
 * `DatastoreLanguageProvider`、`DatastoreBaseHostUrlProvider`，使用），
 * 以及帶 [CommonDispatcher] qualifier 的 [CoroutineDispatcher]（供需要
 * 明確排程到 IO 的元件使用，例如 `data.repository.MovieRepositoryImpl`）。
 *
 * 安裝任何依賴這些 binding 的 module（例如 `datastoreModule`、`dataModule`）時，
 * 都必須一併安裝 `commonModule()`；這些 module 本身不得重複定義等價的
 * [CoroutineScope]／[CoroutineDispatcher] binding。
 */
fun commonModule() = module {
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single<CoroutineDispatcher>(qualifier = named(CommonDispatcher.IO)) { Dispatchers.IO }
}
