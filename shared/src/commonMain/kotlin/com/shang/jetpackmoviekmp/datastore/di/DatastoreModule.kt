package com.shang.jetpackmoviekmp.datastore.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.shang.jetpackmoviekmp.datastore.UserPreferenceDataSource
import com.shang.jetpackmoviekmp.network.provider.DatastoreLanguageProvider
import com.shang.jetpackmoviekmp.network.provider.LanguageProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/**
 * 提供使用者偏好設定 DataStore 相關依賴，並將 [LanguageProvider] 綁定到
 * datastore-backed 的實作（[DatastoreLanguageProvider]）。
 *
 * 安裝此 module 時，production DI 應搭配 `networkModule(isDebug, provideDefaultLanguageProvider = false)`，
 * 避免與固定預設的 `DefaultLanguageProvider` 綁定衝突。
 *
 * @param dataStore 平台建立的 preferences DataStore（見 `createUserPreferencesDataStore`）。
 */
fun datastoreModule(dataStore: DataStore<Preferences>) = module {
    single { dataStore }
    single { UserPreferenceDataSource(get()) }
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single<LanguageProvider> { DatastoreLanguageProvider(get(), get()) }
}
