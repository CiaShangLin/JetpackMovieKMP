package com.shang.jetpackmoviekmp.data.di

import com.shang.jetpackmoviekmp.common.di.commonModule
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
import com.shang.jetpackmoviekmp.database.di.databaseModule
import com.shang.jetpackmoviekmp.database.getTestDatabaseBuilder
import com.shang.jetpackmoviekmp.datastore.InMemoryPreferencesDataStore
import com.shang.jetpackmoviekmp.datastore.di.datastoreModule
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
 * `DatabaseModuleResolutionTest` 的作法），因此可在 commonTest 各平台安全執行。
 */
class DataModuleTest : KoinTest {

    private val movieRepository: MovieRepository by inject()
    private val userDataRepository: UserDataRepository by inject()

    @BeforeTest
    fun setUp() {
        startKoin {
            modules(
                commonModule(),
                networkModule(isDebug = true, provideDefaultLanguageProvider = false),
                databaseModule { getTestDatabaseBuilder() },
                datastoreModule(InMemoryPreferencesDataStore()),
                dataModule(),
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun dataModule_resolves_movieRepository() {
        assertNotNull(movieRepository)
    }

    @Test
    fun dataModule_resolves_userDataRepository() {
        assertNotNull(userDataRepository)
    }

    @Test
    fun dataModule_without_commonModule_fails_to_resolve_movieRepository() {
        // MovieRepositoryImpl 依賴 commonModule() 提供的 CommonDispatcher.IO qualified
        // CoroutineDispatcher；缺少 commonModule() 時應該直接無法 resolve，而不是等到
        // app 啟動才在別處炸掉。
        val koinApp = koinApplication {
            modules(
                networkModule(isDebug = true, provideDefaultLanguageProvider = false),
                databaseModule { getTestDatabaseBuilder() },
                datastoreModule(InMemoryPreferencesDataStore()),
                dataModule(),
            )
        }

        assertFails {
            koinApp.koin.get<MovieRepository>()
        }

        koinApp.close()
    }
}
