package com.shang.jetpackmoviekmp

import com.shang.jetpackmoviekmp.common.BaseHostUrlProvider
import com.shang.jetpackmoviekmp.common.LanguageProvider
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
import com.shang.jetpackmoviekmp.database.getTestDatabaseBuilder
import com.shang.jetpackmoviekmp.datastore.InMemoryPreferencesDataStore
import com.shang.jetpackmoviekmp.datastore.UserPreferenceDataSource
import com.shang.jetpackmoviekmp.domain.usecase.GetConfigurationUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetHistoryMovieListUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetHomeMovieListUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetMovieDetailUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetMovieRecommendUseCase
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
    private val movieRepository: MovieRepository by inject()
    private val userDataRepository: UserDataRepository by inject()
    private val getConfigurationUseCase: GetConfigurationUseCase by inject()
    private val getHistoryMovieListUseCase: GetHistoryMovieListUseCase by inject()
    private val getHomeMovieListUseCase: GetHomeMovieListUseCase by inject()
    private val getMovieDetailUseCase: GetMovieDetailUseCase by inject()
    private val getMovieRecommendUseCase: GetMovieRecommendUseCase by inject()
    private val appDiagnostics: AppDiagnostics by inject()

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

    @Test
    fun initKoin_resolves_movieRepository() {
        assertNotNull(movieRepository)
    }

    @Test
    fun initKoin_resolves_userDataRepository() {
        assertNotNull(userDataRepository)
    }

    @Test
    fun initKoin_resolves_getConfigurationUseCase() {
        assertNotNull(getConfigurationUseCase)
    }

    @Test
    fun initKoin_resolves_getHistoryMovieListUseCase() {
        assertNotNull(getHistoryMovieListUseCase)
    }

    @Test
    fun initKoin_resolves_getHomeMovieListUseCase() {
        assertNotNull(getHomeMovieListUseCase)
    }

    @Test
    fun initKoin_resolves_getMovieDetailUseCase() {
        assertNotNull(getMovieDetailUseCase)
    }

    @Test
    fun initKoin_resolves_getMovieRecommendUseCase() {
        assertNotNull(getMovieRecommendUseCase)
    }

    @Test
    fun initKoin_resolves_appDiagnostics() {
        assertNotNull(appDiagnostics)
    }
}
