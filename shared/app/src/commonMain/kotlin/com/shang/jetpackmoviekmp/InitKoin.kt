package com.shang.jetpackmoviekmp

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import com.shang.jetpackmoviekmp.common.di.commonModule
import com.shang.jetpackmoviekmp.data.di.dataModule
import com.shang.jetpackmoviekmp.database.AppDatabase
import com.shang.jetpackmoviekmp.database.di.databaseModule
import com.shang.jetpackmoviekmp.datastore.di.datastoreModule
import com.shang.jetpackmoviekmp.domain.di.domainModule
import com.shang.jetpackmoviekmp.network.di.networkModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * 兩個平台共用的 Koin 啟動進入點，統一安裝 [commonModule]、[datastoreModule]、[databaseModule]、
 * [networkModule]、[dataModule]、[domainModule]。
 *
 * `androidApp`、`iosApp` 都必須透過這個進入點（或 [initKoin] 的無 [appDeclaration] 版本）啟動 Koin，
 * 不得各自重複組裝 module 清單。
 *
 * @param dataStore 平台建立的 preferences DataStore（見 `createUserPreferencesDataStore`）。
 * @param databaseBuilder 建立平台 [RoomDatabase.Builder] 的 lambda（見 `getDatabaseBuilder`）；只在
 *   `databaseModule` 第一次實際 resolve `AppDatabase` 時才會被呼叫，避免在 app 啟動時就執行
 *   `getDatabasePath` 等檔案路徑解析。
 * @param isDebug 為 `true` 時啟用 network request logging。
 * @param appDeclaration 平台專屬的 Koin 設定（例如 Android 的 `androidContext(this)`）。
 */
internal fun initKoin(
    dataStore: DataStore<Preferences>,
    databaseBuilder: () -> RoomDatabase.Builder<AppDatabase>,
    isDebug: Boolean,
    appDeclaration: KoinAppDeclaration,
) {
    startKoin {
        appDeclaration()
        modules(
            commonModule(),
            datastoreModule(dataStore),
            databaseModule(databaseBuilder),
            networkModule(isDebug = isDebug, provideDefaultLanguageProvider = false),
            dataModule(),
            domainModule(),
            module {
                single { AppDiagnostics(getConfigurationUseCase = get(), userDataRepository = get()) }
            },
        )
    }
}

/**
 * 不需要平台專屬 Koin 設定時使用的簡化版本（例如 iOS，沒有等同 Android `androidContext` 的概念）。
 *
 * @param dataStore 平台建立的 preferences DataStore（見 `createUserPreferencesDataStore`）。
 * @param databaseBuilder 建立平台 [RoomDatabase.Builder] 的 lambda（見 `getDatabaseBuilder`）。
 * @param isDebug 為 `true` 時啟用 network request logging。
 */
internal fun initKoin(
    dataStore: DataStore<Preferences>,
    databaseBuilder: () -> RoomDatabase.Builder<AppDatabase>,
    isDebug: Boolean,
) {
    initKoin(dataStore = dataStore, databaseBuilder = databaseBuilder, isDebug = isDebug, appDeclaration = {})
}
