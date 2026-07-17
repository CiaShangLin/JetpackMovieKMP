package com.shang.jetpackmoviekmp.database.di

import com.shang.jetpackmoviekmp.database.AppDatabase
import com.shang.jetpackmoviekmp.database.dao.MovieCollectDao
import com.shang.jetpackmoviekmp.database.dao.MovieHistoryDao
import com.shang.jetpackmoviekmp.database.getTestDatabaseBuilder
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * 只驗證 Koin resolve 是否成功（不觸發實際 DAO 讀寫），因此不需要平台真正可用的
 * SQLite native library，可在 commonTest 各平台安全執行。實際讀寫行為驗證見
 * `shared/src/iosTest/.../database/di/DatabaseModuleTest.kt`
 * （Android host test 環境沒有 Android ABI 對應的 `sqliteJni` native library 可載入，
 * 見 tasks.md 7.4 的環境限制紀錄）。
 */
class DatabaseModuleResolutionTest : KoinTest {

    private val appDatabase: AppDatabase by inject()
    private val movieCollectDao: MovieCollectDao by inject()
    private val movieHistoryDao: MovieHistoryDao by inject()

    @BeforeTest
    fun setUp() {
        startKoin {
            modules(databaseModule { getTestDatabaseBuilder() })
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun databaseModule_resolves_appDatabase() {
        assertNotNull(appDatabase)
    }

    @Test
    fun databaseModule_resolves_movieCollectDao() {
        assertNotNull(movieCollectDao)
    }

    @Test
    fun databaseModule_resolves_movieHistoryDao() {
        assertNotNull(movieHistoryDao)
    }
}
