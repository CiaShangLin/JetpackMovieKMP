package com.shang.jetpackmoviekmp.database.dao

import com.shang.jetpackmoviekmp.database.AppDatabase
import com.shang.jetpackmoviekmp.database.entity.testMovieCollectEntity
import com.shang.jetpackmoviekmp.database.getRoomDatabase
import com.shang.jetpackmoviekmp.database.getTestDatabaseBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MovieCollectDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: MovieCollectDao

    @BeforeTest
    fun setUp() {
        database = getRoomDatabase(getTestDatabaseBuilder())
        dao = database.createMovieCollectDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertMovieCollect_appears_in_getAllMovies() = runTest {
        dao.insertMovieCollect(testMovieCollectEntity())

        assertEquals(listOf(testMovieCollectEntity()), dao.getAllMovies().first())
    }

    @Test
    fun getAllMovies_returns_newest_collect_first_then_sorts_equal_timestamps_by_id() = runTest {
        val oldest = testMovieCollectEntity(id = 3).copy(timestamp = 100)
        val equalTimestampHigherId = testMovieCollectEntity(id = 4).copy(timestamp = 200)
        val newest = testMovieCollectEntity(id = 2).copy(timestamp = 300)
        val equalTimestampLowerId = testMovieCollectEntity(id = 1).copy(timestamp = 200)

        dao.insertMovieCollect(oldest)
        dao.insertMovieCollect(equalTimestampHigherId)
        dao.insertMovieCollect(newest)
        dao.insertMovieCollect(equalTimestampLowerId)

        assertEquals(
            listOf(newest, equalTimestampLowerId, equalTimestampHigherId, oldest),
            dao.getAllMovies().first(),
        )
    }

    @Test
    fun deleteMovie_removes_it_from_getAllMovies() = runTest {
        val entity = testMovieCollectEntity()
        dao.insertMovieCollect(entity)

        dao.deleteMovie(entity)

        assertTrue(dao.getAllMovies().first().isEmpty())
    }

    @Test
    fun collectedMovieIds_reflects_inserted_ids() = runTest {
        dao.insertMovieCollect(testMovieCollectEntity(id = 1))
        dao.insertMovieCollect(testMovieCollectEntity(id = 2))

        assertEquals(listOf(1, 2), dao.collectedMovieIds().first())
    }

    @Test
    fun getMovieCollectEntityById_returns_matching_entity() = runTest {
        dao.insertMovieCollect(testMovieCollectEntity(id = 42))

        assertEquals(testMovieCollectEntity(id = 42), dao.getMovieCollectEntityById(42).first())
    }

    @Test
    fun getMovieCollectEntityById_returns_null_when_missing() = runTest {
        assertNull(dao.getMovieCollectEntityById(999).first())
    }
}
