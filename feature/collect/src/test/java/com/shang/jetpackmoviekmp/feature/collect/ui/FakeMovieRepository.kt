package com.shang.jetpackmoviekmp.feature.collect.ui

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

    val collectedMovies = MutableStateFlow<List<MovieCardResult>>(emptyList())
    var deleteMovieCollectCallCount = 0
        private set
    var deletedMovie: MovieCardResult? = null
        private set
    val deleteMovieCollectLatch = CountDownLatch(1)

    override fun getConfiguration(): Flow<Result<ConfigurationBean>> = flowOf(Result.success(ConfigurationBean()))

    override fun getMovieGenres(): Flow<AppResult<MovieGenreBean>> = flowOf(AppResult.Success(MovieGenreBean()))

    override fun getMovieListPager(withGenres: String): Flow<PagingData<MovieCardResult>> =
        flowOf(PagingData.empty())

    override fun getMovieSearchPager(query: String): Flow<PagingData<MovieCardResult>> =
        flowOf(PagingData.empty())

    override fun getMovieDetail(id: Int): Flow<Result<MovieDetailBean>> = flowOf(Result.success(MovieDetailBean()))

    override fun getMovieRecommendations(id: Int): Flow<Result<MovieRecommendBean>> =
        flowOf(Result.success(MovieRecommendBean()))

    override fun getMovieActor(id: Int): Flow<AppResult<MovieCastAndCrewBean>> =
        flowOf(AppResult.Success(MovieCastAndCrewBean()))

    override suspend fun insertMovieCollect(movieResult: MovieCardResult) = Unit

    override suspend fun deleteMovieCollect(movieResult: MovieCardResult) {
        deleteMovieCollectCallCount++
        deletedMovie = movieResult
        deleteMovieCollectLatch.countDown()
    }

    override fun getCollectedMovieIds(): Flow<List<Int>> = flowOf(emptyList())

    override fun getAllMovieCollect(): Flow<List<MovieCardResult>> = collectedMovies

    override fun getMovieCollectEntityById(id: Int): Flow<MovieCardResult?> = flowOf(null)

    override suspend fun insertMovieHistory(movieResult: MovieCardResult) = Unit

    override suspend fun deleteMovieHistory(movieResult: MovieCardResult) = Unit

    override fun getAllMovieHistory(): Flow<List<MovieCardResult>> = flowOf(emptyList())

    override suspend fun deleteAllMovieHistory(): Boolean = false
}
