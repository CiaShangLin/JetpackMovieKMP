package com.shang.jetpackmoviekmp.database.dao

import com.shang.jetpackmoviekmp.database.AppDatabase
import com.shang.jetpackmoviekmp.database.entity.MovieHistoryEntity
import com.shang.jetpackmoviekmp.database.getRoomDatabase
import com.shang.jetpackmoviekmp.database.getTestDatabaseBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MovieHistoryDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: MovieHistoryDao

    @BeforeTest
    fun setUp() {
        database = getRoomDatabase(getTestDatabaseBuilder())
        dao = database.createMovieHistoryDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun historyEntity(id: Int = 1) = MovieHistoryEntity(
        id = id,
        title = "The Matrix",
        posterPath = "/poster.jpg",
        voteAverage = 8.7,
        releaseDate = "1999-03-31",
        timestamp = 2_000L,
    )

    @Test
    fun insertMovie_appears_in_getAllMovies() = runTest {
        dao.insertMovie(historyEntity())

        assertEquals(listOf(historyEntity()), dao.getAllMovies().first())
    }

    @Test
    fun deleteMovie_removes_it_from_getAllMovies() = runTest {
        val entity = historyEntity()
        dao.insertMovie(entity)

        dao.deleteMovie(entity)

        assertTrue(dao.getAllMovies().first().isEmpty())
    }

    @Test
    fun deleteAllMovies_clears_getAllMovies() = runTest {
        dao.insertMovie(historyEntity(id = 1))
        dao.insertMovie(historyEntity(id = 2))

        dao.deleteAllMovies()

        assertTrue(dao.getAllMovies().first().isEmpty())
    }
}
