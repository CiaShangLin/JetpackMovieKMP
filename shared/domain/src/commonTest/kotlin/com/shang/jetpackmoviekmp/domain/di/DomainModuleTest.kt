package com.shang.jetpackmoviekmp.domain.di

import com.shang.jetpackmoviekmp.common.di.commonModule
import com.shang.jetpackmoviekmp.data.di.dataModule
import com.shang.jetpackmoviekmp.database.di.databaseModule
import com.shang.jetpackmoviekmp.database.getTestDatabaseBuilder
import com.shang.jetpackmoviekmp.datastore.InMemoryPreferencesDataStore
import com.shang.jetpackmoviekmp.datastore.di.datastoreModule
import com.shang.jetpackmoviekmp.domain.usecase.GetConfigurationUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetHistoryMovieListUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetHomeMovieListUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetMovieDetailUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetMovieRecommendUseCase
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

/**
 * 只驗證 Koin resolve 是否成功，不觸發實際網路呼叫或 DAO 讀寫（比照
 * `DataModuleTest` 的作法），因此可在 commonTest 各平台安全執行。
 */
class DomainModuleTest : KoinTest {

    private val getConfigurationUseCase: GetConfigurationUseCase by inject()
    private val getHistoryMovieListUseCase: GetHistoryMovieListUseCase by inject()
    private val getHomeMovieListUseCase: GetHomeMovieListUseCase by inject()
    private val getMovieDetailUseCase: GetMovieDetailUseCase by inject()
    private val getMovieRecommendUseCase: GetMovieRecommendUseCase by inject()

    @BeforeTest
    fun setUp() {
        startKoin {
            modules(
                commonModule(),
                networkModule(isDebug = true, provideDefaultLanguageProvider = false),
                databaseModule { getTestDatabaseBuilder() },
                datastoreModule(InMemoryPreferencesDataStore()),
                dataModule(),
                domainModule(),
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun domainModule_resolves_getConfigurationUseCase() {
        assertNotNull(getConfigurationUseCase)
    }

    @Test
    fun domainModule_resolves_getHistoryMovieListUseCase() {
        assertNotNull(getHistoryMovieListUseCase)
    }

    @Test
    fun domainModule_resolves_getHomeMovieListUseCase() {
        assertNotNull(getHomeMovieListUseCase)
    }

    @Test
    fun domainModule_resolves_getMovieDetailUseCase() {
        assertNotNull(getMovieDetailUseCase)
    }

    @Test
    fun domainModule_resolves_getMovieRecommendUseCase() {
        assertNotNull(getMovieRecommendUseCase)
    }

    @Test
    fun domainModule_without_dataModule_fails_to_resolve_getConfigurationUseCase() {
        // GetConfigurationUseCase 依賴 dataModule() 提供的 MovieRepository／UserDataRepository；
        // 缺少 dataModule() 時應該直接無法 resolve（比照 DataModuleTest 的既有測試慣例）。
        val koinApp = koinApplication {
            modules(
                commonModule(),
                networkModule(isDebug = true, provideDefaultLanguageProvider = false),
                databaseModule { getTestDatabaseBuilder() },
                datastoreModule(InMemoryPreferencesDataStore()),
                domainModule(),
            )
        }

        assertFails {
            koinApp.koin.get<GetConfigurationUseCase>()
        }

        koinApp.close()
    }
}
