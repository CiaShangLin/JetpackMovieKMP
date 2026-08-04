package com.shang.jetpackmoviekmp.data.repository

import com.shang.jetpackmoviekmp.common.AppResult
import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.MovieCardResult
import com.shang.jetpackmoviekmp.model.MovieCastAndCrewBean
import com.shang.jetpackmoviekmp.model.MovieDetailBean
import com.shang.jetpackmoviekmp.model.MovieGenreBean
import com.shang.jetpackmoviekmp.network.model.NetworkResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MovieRepositoryImplTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun repository(
        dataSource: FakeMovieDataSource = FakeMovieDataSource(),
        collectDao: FakeMovieCollectDao = FakeMovieCollectDao(),
        historyDao: FakeMovieHistoryDao = FakeMovieHistoryDao(),
        currentTimeMillis: () -> Long = { 2L },
    ) = MovieRepositoryImpl(
        movieDataSource = dataSource,
        movieCollectDao = collectDao,
        movieHistoryDao = historyDao,
        ioDispatcher = UnconfinedTestDispatcher(),
        currentTimeMillis = currentTimeMillis,
    )

    private val movie = MovieCardResult(id = 1, title = "A", timestamp = 1L)

    @Test
    fun getConfiguration_emits_success_when_response_succeeds() = runTest {
        val configuration = ConfigurationBean(changeKeys = listOf("images"))
        val dataSource = FakeMovieDataSource().apply {
            configurationResponse = NetworkResponse(code = 200, data = configuration)
        }

        val result = repository(dataSource).getConfiguration().first()

        assertTrue(result.isSuccess)
        assertEquals(configuration, result.getOrNull())
    }

    @Test
    fun getConfiguration_emits_failure_when_response_fails() = runTest {
        val dataSource = FakeMovieDataSource().apply {
            configurationResponse = NetworkResponse(code = 500, error = failureResponse())
        }

        val result = repository(dataSource).getConfiguration().first()

        assertTrue(result.isFailure)
    }

    @Test
    fun getConfiguration_emits_failure_without_crashing_when_response_has_no_error() = runTest {
        // isSuccess 只保證 2xx && data != null；理論上失敗回應仍可能沒有帶 error
        // （例如未來新增回傳 2xx 但空 body 的 endpoint）。過去直接用 `error!!` 會在這裡 NPE。
        val dataSource = FakeMovieDataSource().apply {
            configurationResponse = NetworkResponse(code = 500, error = null)
        }

        val result = repository(dataSource).getConfiguration().first()

        assertTrue(result.isFailure)
    }

    @Test
    fun getMovieGenres_emits_success_when_response_succeeds() = runTest {
        val genres = MovieGenreBean(genres = listOf(MovieGenreBean.MovieGenre(id = 28, name = "Action")))
        val dataSource = FakeMovieDataSource().apply {
            movieGenresResponse = NetworkResponse(code = 200, data = genres)
        }

        val result = repository(dataSource).getMovieGenres().first()

        assertEquals(AppResult.Success(genres), result)
    }

    @Test
    fun getMovieGenres_emits_failure_when_response_fails() = runTest {
        val dataSource = FakeMovieDataSource().apply {
            movieGenresResponse = NetworkResponse(code = 404, error = failureResponse())
        }

        val result = repository(dataSource).getMovieGenres().first()

        assertIs<AppResult.Failure>(result)
    }

    @Test
    fun getMovieDetail_emits_success_when_response_succeeds() = runTest {
        val detail = MovieDetailBean(id = 7, title = "Detail")
        val dataSource = FakeMovieDataSource().apply {
            movieDetailResponse = NetworkResponse(code = 200, data = detail)
        }

        val result = repository(dataSource).getMovieDetail(7).first()

        assertEquals(detail, result.getOrNull())
    }

    @Test
    fun getMovieActor_emits_app_result_success_when_response_succeeds() = runTest {
        val castAndCrew = MovieCastAndCrewBean()
        val dataSource = FakeMovieDataSource().apply {
            movieActorResponse = NetworkResponse(code = 200, data = castAndCrew)
        }

        val result = repository(dataSource).getMovieActor(7).first()

        assertEquals(AppResult.Success(castAndCrew), result)
    }

    @Test
    fun getMovieActor_emits_app_result_failure_when_response_fails() = runTest {
        val dataSource = FakeMovieDataSource().apply {
            movieActorResponse = NetworkResponse(code = 500, error = failureResponse())
        }

        val result = repository(dataSource).getMovieActor(7).first()

        assertIs<AppResult.Failure>(result)
    }

    @Test
    fun getMovieListPager_is_collectible_without_throwing() = runTest {
        val pagingData = repository().getMovieListPager(withGenres = "28").first()

        assertNotNull(pagingData)
    }

    @Test
    fun getMovieSearchPager_is_collectible_without_throwing() = runTest {
        val pagingData = repository().getMovieSearchPager(query = "matrix").first()

        assertNotNull(pagingData)
    }

    @Test
    fun insertMovieCollect_then_getAllMovieCollect_contains_entry_marked_as_collected() = runTest {
        val repository = repository()

        repository.insertMovieCollect(movie)

        val collected = repository.getAllMovieCollect().first()
        assertEquals(1, collected.size)
        assertTrue(collected.first().isCollect)
        assertEquals(movie.id, collected.first().id)
    }

    @Test
    fun insertMovieCollect_uses_shared_collection_time_instead_of_caller_timestamp() = runTest {
        val collectDao = FakeMovieCollectDao()
        val repository = repository(collectDao = collectDao)

        repository.insertMovieCollect(movie.copy(timestamp = 1L))

        assertEquals(2L, collectDao.getAllMovies().first().single().timestamp)
    }

    @Test
    fun getAllMovieCollect_preserves_dao_newest_first_order() = runTest {
        val collectionTimes = listOf(100L, 300L).iterator()
        val repository = repository(currentTimeMillis = { collectionTimes.next() })

        repository.insertMovieCollect(movie.copy(id = 1, timestamp = 0L))
        repository.insertMovieCollect(movie.copy(id = 2, timestamp = 0L))

        assertEquals(listOf(2, 1), repository.getAllMovieCollect().first().map { it.id })
    }

    @Test
    fun deleteMovieCollect_removes_entry_from_getAllMovieCollect_and_getCollectedMovieIds() = runTest {
        val repository = repository()
        repository.insertMovieCollect(movie)

        repository.deleteMovieCollect(movie)

        assertTrue(repository.getAllMovieCollect().first().isEmpty())
        assertTrue(repository.getCollectedMovieIds().first().isEmpty())
    }

    @Test
    fun getMovieCollectEntityById_returns_entry_when_present_and_null_when_absent() = runTest {
        val repository = repository()
        repository.insertMovieCollect(movie)

        assertEquals(movie.id, repository.getMovieCollectEntityById(movie.id).first()?.id)
        assertEquals(null, repository.getMovieCollectEntityById(999).first())
    }

    @Test
    fun insertMovieHistory_then_getAllMovieHistory_contains_entry() = runTest {
        val repository = repository()

        repository.insertMovieHistory(movie)

        val history = repository.getAllMovieHistory().first()
        assertEquals(1, history.size)
        assertEquals(movie.id, history.first().id)
    }

    @Test
    fun deleteMovieHistory_removes_entry_from_getAllMovieHistory() = runTest {
        val repository = repository()
        repository.insertMovieHistory(movie)

        repository.deleteMovieHistory(movie)

        assertTrue(repository.getAllMovieHistory().first().isEmpty())
    }

    @Test
    fun deleteAllMovieHistory_returns_true_when_entries_were_deleted() = runTest {
        val repository = repository()
        repository.insertMovieHistory(movie)

        val deleted = repository.deleteAllMovieHistory()

        assertTrue(deleted)
        assertTrue(repository.getAllMovieHistory().first().isEmpty())
    }

    @Test
    fun deleteAllMovieHistory_returns_false_when_nothing_to_delete() = runTest {
        val deleted = repository().deleteAllMovieHistory()

        assertFalse(deleted)
    }
}
