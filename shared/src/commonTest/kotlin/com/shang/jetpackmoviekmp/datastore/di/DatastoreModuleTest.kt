package com.shang.jetpackmoviekmp.datastore.di

import com.shang.jetpackmoviekmp.common.BaseHostUrlProvider
import com.shang.jetpackmoviekmp.common.LanguageProvider
import com.shang.jetpackmoviekmp.common.di.commonModule
import com.shang.jetpackmoviekmp.datastore.InMemoryPreferencesDataStore
import com.shang.jetpackmoviekmp.datastore.UserPreferenceDataSource
import com.shang.jetpackmoviekmp.network.datasource.MovieDataSource
import com.shang.jetpackmoviekmp.network.di.networkModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.koinApplication
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertNotNull

class DatastoreModuleTest : KoinTest {

    private val userPreferenceDataSource: UserPreferenceDataSource by inject()
    private val languageProvider: LanguageProvider by inject()
    private val baseHostUrlProvider: BaseHostUrlProvider by inject()
    private val movieDataSource: MovieDataSource by inject()

    @BeforeTest
    fun setUp() {
        startKoin {
            modules(
                commonModule(),
                datastoreModule(InMemoryPreferencesDataStore()),
                networkModule(isDebug = true, provideDefaultLanguageProvider = false),
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun datastoreModule_resolves_userPreferenceDataSource() {
        assertNotNull(userPreferenceDataSource)
    }

    @Test
    fun datastoreModule_resolves_datastore_backed_languageProvider() {
        assertNotNull(languageProvider)
    }

    @Test
    fun datastoreModule_resolves_datastore_backed_baseHostUrlProvider() {
        assertNotNull(baseHostUrlProvider)
    }

    @Test
    fun networkModule_resolves_movieDataSource_when_wired_with_datastore_languageProvider() {
        assertNotNull(movieDataSource)
    }

    @Test
    fun datastoreModule_without_commonModule_fails_to_resolve_language_provider() {
        val koinApp = koinApplication {
            modules(datastoreModule(InMemoryPreferencesDataStore()))
        }

        assertFails {
            koinApp.koin.get<LanguageProvider>()
        }

        koinApp.close()
    }

    @Test
    fun datastoreModule_without_commonModule_fails_to_resolve_base_host_url_provider() {
        val koinApp = koinApplication {
            modules(datastoreModule(InMemoryPreferencesDataStore()))
        }

        assertFails {
            koinApp.koin.get<BaseHostUrlProvider>()
        }

        koinApp.close()
    }
}
