package com.shang.jetpackmoviekmp

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.shang.jetpackmoviekmp.common.di.commonModule
import com.shang.jetpackmoviekmp.datastore.di.datastoreModule
import com.shang.jetpackmoviekmp.network.di.networkModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * 兩個平台共用的 Koin 啟動進入點，統一安裝 [commonModule]、[datastoreModule]、[networkModule]。
 *
 * `androidApp`、`iosApp` 都必須透過這個進入點（或 [initKoin] 的無 [appDeclaration] 版本）啟動 Koin，
 * 不得各自重複組裝 module 清單。
 *
 * @param dataStore 平台建立的 preferences DataStore（見 `createUserPreferencesDataStore`）。
 * @param isDebug 為 `true` 時啟用 network request logging。
 * @param appDeclaration 平台專屬的 Koin 設定（例如 Android 的 `androidContext(this)`）。
 */
fun initKoin(
    dataStore: DataStore<Preferences>,
    isDebug: Boolean,
    appDeclaration: KoinAppDeclaration,
) {
    startKoin {
        appDeclaration()
        modules(
            commonModule(),
            datastoreModule(dataStore),
            networkModule(isDebug = isDebug, provideDefaultLanguageProvider = false),
        )
    }
}

/**
 * 不需要平台專屬 Koin 設定時使用的簡化版本（例如 iOS，沒有等同 Android `androidContext` 的概念）。
 *
 * @param dataStore 平台建立的 preferences DataStore（見 `createUserPreferencesDataStore`）。
 * @param isDebug 為 `true` 時啟用 network request logging。
 */
fun initKoin(dataStore: DataStore<Preferences>, isDebug: Boolean) {
    initKoin(dataStore = dataStore, isDebug = isDebug, appDeclaration = {})
}
