package com.shang.jetpackmoviekmp.datastore.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.shang.jetpackmoviekmp.common.BaseHostUrlProvider
import com.shang.jetpackmoviekmp.common.LanguageProvider
import com.shang.jetpackmoviekmp.datastore.UserPreferenceDataSource
import com.shang.jetpackmoviekmp.network.provider.DatastoreBaseHostUrlProvider
import com.shang.jetpackmoviekmp.network.provider.DatastoreLanguageProvider
import org.koin.dsl.module

/**
 * 提供使用者偏好設定 DataStore 相關依賴，並將 [LanguageProvider]、[BaseHostUrlProvider]
 * 綁定到 datastore-backed 的實作（[DatastoreLanguageProvider]、[DatastoreBaseHostUrlProvider]）。
 *
 * 安裝此 module 時，production DI 應搭配 `networkModule(isDebug, provideDefaultLanguageProvider = false)`，
 * 避免與固定預設的 `DefaultLanguageProvider` 綁定衝突；[DatastoreLanguageProvider]、
 * [DatastoreBaseHostUrlProvider] 都依賴 `commonModule()` 提供的 `CoroutineScope`，
 * 因此本 module 必須搭配 `commonModule()` 一起安裝才能完整解析。
 *
 * @param dataStore 平台建立的 preferences DataStore（見 `createUserPreferencesDataStore`）。
 */
fun datastoreModule(dataStore: DataStore<Preferences>) = module {
    single { dataStore }
    single { UserPreferenceDataSource(get()) }
    single<LanguageProvider> { DatastoreLanguageProvider(get(), get()) }
    single<BaseHostUrlProvider> { DatastoreBaseHostUrlProvider(get(), get()) }
}
