package com.shang.jetpackmoviekmp.datastore.provider

import com.shang.jetpackmoviekmp.datastore.InMemoryPreferencesDataStore
import com.shang.jetpackmoviekmp.datastore.UserPreferenceDataSource
import com.shang.jetpackmoviekmp.model.LanguageMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DatastoreLanguageProviderTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getLanguageCode_reflects_system_default_before_any_persisted_preference() = runTest {
        val dataSource = UserPreferenceDataSource(InMemoryPreferencesDataStore())
        val provider = DatastoreLanguageProvider(dataSource, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        // 尚未持久化任何 preference 時，UserData.getDefault() 使用 LanguageMode.SYSTEM_DEFAULT，
        // 因此預期值取決於執行環境的 system language，而非固定寫死 "zh-TW"。
        assertEquals(LanguageMode.SYSTEM_DEFAULT.toLanguageCode(), provider.getLanguageCode())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getLanguageCode_reflects_persisted_english() = runTest {
        val dataSource = UserPreferenceDataSource(InMemoryPreferencesDataStore())
        val provider = DatastoreLanguageProvider(dataSource, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        dataSource.setLanguageMode(LanguageMode.ENGLISH)

        assertEquals("en-US", provider.getLanguageCode())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getLanguageCode_reflects_persisted_traditional_chinese() = runTest {
        val dataSource = UserPreferenceDataSource(InMemoryPreferencesDataStore())
        val provider = DatastoreLanguageProvider(dataSource, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        dataSource.setLanguageMode(LanguageMode.ENGLISH)
        dataSource.setLanguageMode(LanguageMode.TRADITIONAL_CHINESE)

        assertEquals("zh-TW", provider.getLanguageCode())
    }
}
