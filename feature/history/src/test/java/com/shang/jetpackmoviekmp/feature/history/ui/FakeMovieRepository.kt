package com.shang.jetpackmoviekmp.feature.history.ui

import androidx.paging.PagingData
import com.shang.jetpackmoviekmp.common.AppResult
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.MovieCardResult
import com.shang.jetpackmoviekmp.model.MovieCastAndCrewBean
import com.shang.jetpackmoviekmp.model.MovieDetailBean
import com.shang.jetpackmoviekmp.model.MovieGenreBean
import com.shang.jetpackmoviekmp.model.MovieRecommendBean
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.util.concurrent.CountDownLatch

internal class FakeMovieRepository : MovieRepository {

    val historyMovies = MutableStateFlow<List<MovieCardResult>>(emptyList())
    val collectedMovieIds = MutableStateFlow<List<Int>>(emptyList())
    var insertedMovie: MovieCardResult? = null
        private set
    var deletedMovie: MovieCardResult? = null
        private set
    var clearHistoryCallCount = 0
        private set
    val insertMovieCollectLatch = CountDownLatch(1)
    val deleteMovieCollectLatch = CountDownLatch(1)
    val clearHistoryLatch = CountDownLatch(1)

    override fun getConfiguration(): Flow<Result<ConfigurationBean>> = flowOf(Result.success(ConfigurationBean()))

    override fun getMovieGenres(): Flow<AppResult<MovieGenreBean>> = flowOf(AppResult.Success(MovieGenreBean()))

    override fun getMovieListPager(withGenres: String): Flow<PagingData<MovieCardResult>> = flowOf(PagingData.empty())

    override fun getMovieSearchPager(query: String): Flow<PagingData<MovieCardResult>> = flowOf(PagingData.empty())

    override fun getMovieDetail(id: Int): Flow<Result<MovieDetailBean>> = flowOf(Result.success(MovieDetailBean()))

    override fun getMovieRecommendations(id: Int): Flow<Result<MovieRecommendBean>> = flowOf(Result.success(MovieRecommendBean()))

    override fun getMovieActor(id: Int): Flow<Result<MovieCastAndCrewBean>> = flowOf(Result.success(MovieCastAndCrewBean()))

    override suspend fun insertMovieCollect(movieResult: MovieCardResult) {
        insertedMovie = movieResult
        insertMovieCollectLatch.countDown()
    }

    override suspend fun deleteMovieCollect(movieResult: MovieCardResult) {
        deletedMovie = movieResult
        deleteMovieCollectLatch.countDown()
    }

    override fun getCollectedMovieIds(): Flow<List<Int>> = collectedMovieIds

    override fun getAllMovieCollect(): Flow<List<MovieCardResult>> = flowOf(emptyList())

    override fun getMovieCollectEntityById(id: Int): Flow<MovieCardResult?> = flowOf(null)

    override suspend fun insertMovieHistory(movieResult: MovieCardResult) = Unit

    override suspend fun deleteMovieHistory(movieResult: MovieCardResult) = Unit

    override fun getAllMovieHistory(): Flow<List<MovieCardResult>> = historyMovies

    override suspend fun deleteAllMovieHistory(): Boolean {
        clearHistoryCallCount++
        clearHistoryLatch.countDown()
        return true
    }
}
