package com.shang.jetpackmoviekmp.database.di

import com.shang.jetpackmoviekmp.database.AppDatabase
import com.shang.jetpackmoviekmp.database.dao.MovieCollectDao
import com.shang.jetpackmoviekmp.database.dao.MovieHistoryDao
import com.shang.jetpackmoviekmp.database.entity.testMovieCollectEntity
import com.shang.jetpackmoviekmp.database.getTestDatabaseBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DatabaseModuleTest : KoinTest {

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
        appDatabase.close()
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

    @Test
    fun databaseModule_daos_share_the_same_appDatabase_instance() = runTest {
        val entity = testMovieCollectEntity()

        movieCollectDao.insertMovieCollect(entity)

        // 透過同一個 appDatabase 另外拿一份 DAO 也應該看得到剛才寫入的資料，
        // 證明 Koin resolve 出的 movieCollectDao 背後就是這個 appDatabase 單例。
        assertEquals(listOf(entity), appDatabase.createMovieCollectDao().getAllMovies().first())
    }
}
