package com.shang.jetpackmoviekmp.datastore.provider

import com.shang.jetpackmoviekmp.datastore.InMemoryPreferencesDataStore
import com.shang.jetpackmoviekmp.datastore.UserPreferenceDataSource
import com.shang.jetpackmoviekmp.model.ConfigurationBean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DatastoreBaseHostUrlProviderTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getBaseHostUrl_returns_empty_string_before_any_persisted_configuration() = runTest {
        val dataSource = UserPreferenceDataSource(InMemoryPreferencesDataStore())
        val provider = DatastoreBaseHostUrlProvider(dataSource, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        assertEquals("", provider.getBaseHostUrl())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getBaseHostUrl_reflects_persisted_configuration_base_url() = runTest {
        val dataSource = UserPreferenceDataSource(InMemoryPreferencesDataStore())
        val provider = DatastoreBaseHostUrlProvider(dataSource, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        dataSource.setConfiguration(
            ConfigurationBean(images = ConfigurationBean.Images(baseUrl = "https://image.tmdb.org/t/p/")),
        )

        assertEquals("https://image.tmdb.org/t/p/", provider.getBaseHostUrl())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getBaseHostUrl_appends_trailing_slash_when_missing() = runTest {
        val dataSource = UserPreferenceDataSource(InMemoryPreferencesDataStore())
        val provider = DatastoreBaseHostUrlProvider(dataSource, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        dataSource.setConfiguration(
            ConfigurationBean(images = ConfigurationBean.Images(baseUrl = "https://image.tmdb.org/t/p")),
        )

        assertEquals("https://image.tmdb.org/t/p/", provider.getBaseHostUrl())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getBaseHostUrl_keeps_empty_string_when_persisted_base_url_is_empty() = runTest {
        val dataSource = UserPreferenceDataSource(InMemoryPreferencesDataStore())
        val provider = DatastoreBaseHostUrlProvider(dataSource, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        dataSource.setConfiguration(ConfigurationBean(images = ConfigurationBean.Images(baseUrl = "")))

        assertEquals("", provider.getBaseHostUrl())
    }
}
