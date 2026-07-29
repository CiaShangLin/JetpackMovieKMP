package com.shang.jetpackmoviekmp.feature.search.ui

import androidx.paging.PagingData
import com.shang.jetpackmoviekmp.common.AppResult
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.MovieCardResult
import com.shang.jetpackmoviekmp.model.MovieCastAndCrewBean
import com.shang.jetpackmoviekmp.model.MovieDetailBean
import com.shang.jetpackmoviekmp.model.MovieGenreBean
import com.shang.jetpackmoviekmp.model.MovieRecommendBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初始空白 query 不搜尋`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository()
        val viewModel = SearchViewModel(repository)
        val job = viewModel.movieSearchPager.launchIn(this)

        // Act
        runCurrent()

        // Assert
        assertEquals("", viewModel.searchQuery.value)
        assertTrue(repository.searchQueries.isEmpty())
        job.cancel()
    }

    @Test
    fun `提交非空 query 後搜尋電影`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository()
        val viewModel = SearchViewModel(repository)
        val job = viewModel.movieSearchPager.launchIn(this)

        // Act
        viewModel.startSearch("Dune")
        advanceTimeBy(300)
        runCurrent()

        // Assert
        assertEquals("Dune", viewModel.searchQuery.value)
        assertEquals(listOf("Dune"), repository.searchQueries)
        job.cancel()
    }

    @Test
    fun `提交新 query 時改為搜尋新關鍵字`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository()
        val viewModel = SearchViewModel(repository)
        val job = viewModel.movieSearchPager.launchIn(this)
        viewModel.startSearch("Dune")
        advanceTimeBy(300)
        runCurrent()

        // Act
        viewModel.startSearch("Avatar")
        advanceTimeBy(300)
        runCurrent()

        // Assert
        assertEquals(listOf("Dune", "Avatar"), repository.searchQueries)
        job.cancel()
    }

    @Test
    fun `retry 重新搜尋目前 query`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeMovieRepository()
        val viewModel = SearchViewModel(repository)
        val job = viewModel.movieSearchPager.launchIn(this)
        viewModel.startSearch("Dune")
        advanceTimeBy(300)
        runCurrent()

        // Act
        viewModel.retrySearch()
        runCurrent()

        // Assert
        assertEquals(listOf("Dune", "Dune"), repository.searchQueries)
        job.cancel()
    }
}

private class FakeMovieRepository : MovieRepository {

    val searchQueries = mutableListOf<String>()

    override fun getConfiguration(): Flow<Result<ConfigurationBean>> = flowOf(Result.success(ConfigurationBean()))

    override fun getMovieGenres(): Flow<AppResult<MovieGenreBean>> = flowOf(AppResult.Success(MovieGenreBean()))

    override fun getMovieListPager(withGenres: String): Flow<PagingData<MovieCardResult>> = flowOf(PagingData.empty())

    override fun getMovieSearchPager(query: String): Flow<PagingData<MovieCardResult>> {
        searchQueries += query
        return flowOf(PagingData.empty())
    }

    override fun getMovieDetail(id: Int): Flow<Result<MovieDetailBean>> = flowOf(Result.success(MovieDetailBean()))

    override fun getMovieRecommendations(id: Int): Flow<Result<MovieRecommendBean>> =
        flowOf(Result.success(MovieRecommendBean()))

    override fun getMovieActor(id: Int): Flow<Result<MovieCastAndCrewBean>> =
        flowOf(Result.success(MovieCastAndCrewBean()))

    override suspend fun insertMovieCollect(movieResult: MovieCardResult) = Unit

    override suspend fun deleteMovieCollect(movieResult: MovieCardResult) = Unit

    override fun getCollectedMovieIds(): Flow<List<Int>> = flowOf(emptyList())

    override fun getAllMovieCollect(): Flow<List<MovieCardResult>> = flowOf(emptyList())

    override fun getMovieCollectEntityById(id: Int): Flow<MovieCardResult?> = flowOf(null)

    override suspend fun insertMovieHistory(movieResult: MovieCardResult) = Unit

    override suspend fun deleteMovieHistory(movieResult: MovieCardResult) = Unit

    override fun getAllMovieHistory(): Flow<List<MovieCardResult>> = flowOf(emptyList())

    override suspend fun deleteAllMovieHistory(): Boolean = false
}
