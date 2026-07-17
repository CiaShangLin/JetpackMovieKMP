package com.shang.jetpackmoviekmp

import com.shang.jetpackmoviekmp.common.BaseHostUrlProvider
import com.shang.jetpackmoviekmp.common.LanguageProvider
import com.shang.jetpackmoviekmp.database.getTestDatabaseBuilder
import com.shang.jetpackmoviekmp.datastore.InMemoryPreferencesDataStore
import com.shang.jetpackmoviekmp.datastore.UserPreferenceDataSource
import com.shang.jetpackmoviekmp.network.datasource.MovieDataSource
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class InitKoinTest : KoinTest {

    private val userPreferenceDataSource: UserPreferenceDataSource by inject()
    private val languageProvider: LanguageProvider by inject()
    private val baseHostUrlProvider: BaseHostUrlProvider by inject()
    private val movieDataSource: MovieDataSource by inject()

    @BeforeTest
    fun setUp() {
        initKoin(
            dataStore = InMemoryPreferencesDataStore(),
            databaseBuilder = { getTestDatabaseBuilder() },
            isDebug = true,
        )
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun initKoin_resolves_userPreferenceDataSource() {
        assertNotNull(userPreferenceDataSource)
    }

    @Test
    fun initKoin_resolves_datastore_backed_languageProvider() {
        assertNotNull(languageProvider)
    }

    @Test
    fun initKoin_resolves_datastore_backed_baseHostUrlProvider() {
        assertNotNull(baseHostUrlProvider)
    }

    @Test
    fun initKoin_resolves_movieDataSource() {
        assertNotNull(movieDataSource)
    }
}
