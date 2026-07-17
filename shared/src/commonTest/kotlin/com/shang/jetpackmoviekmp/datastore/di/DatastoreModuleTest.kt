package com.shang.jetpackmoviekmp.datastore.di

import com.shang.jetpackmoviekmp.datastore.InMemoryPreferencesDataStore
import com.shang.jetpackmoviekmp.datastore.UserPreferenceDataSource
import com.shang.jetpackmoviekmp.network.datasource.MovieDataSource
import com.shang.jetpackmoviekmp.network.di.networkModule
import com.shang.jetpackmoviekmp.network.provider.LanguageProvider
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class DatastoreModuleTest : KoinTest {

    private val userPreferenceDataSource: UserPreferenceDataSource by inject()
    private val languageProvider: LanguageProvider by inject()
    private val movieDataSource: MovieDataSource by inject()

    @BeforeTest
    fun setUp() {
        startKoin {
            modules(
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
    fun networkModule_resolves_movieDataSource_when_wired_with_datastore_languageProvider() {
        assertNotNull(movieDataSource)
    }
}
