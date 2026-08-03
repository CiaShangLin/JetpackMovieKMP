package com.shang.jetpackmoviekmp.presenter

import androidx.paging.PagingData
import com.shang.jetpackmoviekmp.common.AppResult
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.domain.usecase.GetSearchMovieListUseCase
import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.MovieCardResult
import com.shang.jetpackmoviekmp.model.MovieCastAndCrewBean
import com.shang.jetpackmoviekmp.model.MovieDetailBean
import com.shang.jetpackmoviekmp.model.MovieGenreBean
import com.shang.jetpackmoviekmp.model.MovieRecommendBean
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SearchMovieListPresenterTest {

    @Test
    fun init_withTrimmedQuery_passesTrimmedQueryToUseCase() = runTest {
        // Arrange
        val repository = SearchMovieRepository()
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val useCase = GetSearchMovieListUseCase(
            movieRepository = repository,
            ioDispatcher = dispatcher,
        )

        // Act
        val presenter = SearchMovieListPresenter(
            getSearchMovieListUseCase = useCase,
            query = "  Dune  ",
            ioDispatcher = dispatcher,
        )
        advanceUntilIdle()

        // Assert
        assertEquals("Dune", repository.lastSearchQuery)
        presenter.clear()
    }
}

private class SearchMovieRepository : MovieRepository {
    var lastSearchQuery: String? = null

    private val collectedMovieIds = MutableStateFlow<List<Int>>(emptyList())

    override fun getConfiguration(): Flow<Result<ConfigurationBean>> = flowOf(Result.success(ConfigurationBean()))

    override fun getMovieGenres(): Flow<AppResult<MovieGenreBean>> = flowOf(AppResult.Success(MovieGenreBean()))

    override fun getMovieListPager(withGenres: String): Flow<PagingData<MovieCardResult>> = flowOf(PagingData.empty())

    override fun getMovieSearchPager(query: String): Flow<PagingData<MovieCardResult>> {
        lastSearchQuery = query
        return flowOf(PagingData.empty())
    }

    override fun getMovieDetail(id: Int): Flow<Result<MovieDetailBean>> = flowOf(Result.success(MovieDetailBean()))

    override fun getMovieRecommendations(id: Int): Flow<Result<MovieRecommendBean>> =
        flowOf(Result.success(MovieRecommendBean()))

    override fun getMovieActor(id: Int): Flow<AppResult<MovieCastAndCrewBean>> =
        flowOf(AppResult.Success(MovieCastAndCrewBean()))

    override suspend fun insertMovieCollect(movieResult: MovieCardResult) = Unit

    override suspend fun deleteMovieCollect(movieResult: MovieCardResult) = Unit

    override fun getCollectedMovieIds(): Flow<List<Int>> = collectedMovieIds

    override fun getAllMovieCollect(): Flow<List<MovieCardResult>> = flowOf(emptyList())

    override fun getMovieCollectEntityById(id: Int): Flow<MovieCardResult?> = flowOf(null)

    override suspend fun insertMovieHistory(movieResult: MovieCardResult) = Unit

    override suspend fun deleteMovieHistory(movieResult: MovieCardResult) = Unit

    override fun getAllMovieHistory(): Flow<List<MovieCardResult>> = flowOf(emptyList())

    override suspend fun deleteAllMovieHistory(): Boolean = false
}
