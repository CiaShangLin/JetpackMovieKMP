package com.shang.jetpackmoviekmp.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.shang.jetpackmoviekmp.common.NetworkException
import com.shang.jetpackmoviekmp.data.model.asCollectEntity
import com.shang.jetpackmoviekmp.data.model.asHistoryEntity
import com.shang.jetpackmoviekmp.data.paging.MovieGenrePagingSource
import com.shang.jetpackmoviekmp.data.paging.MovieSearchPagingSource
import com.shang.jetpackmoviekmp.database.dao.MovieCollectDao
import com.shang.jetpackmoviekmp.database.dao.MovieHistoryDao
import com.shang.jetpackmoviekmp.database.entity.asExtendedModel
import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.MovieCardResult
import com.shang.jetpackmoviekmp.model.MovieCastAndCrewBean
import com.shang.jetpackmoviekmp.model.MovieDetailBean
import com.shang.jetpackmoviekmp.model.MovieGenreBean
import com.shang.jetpackmoviekmp.model.MovieRecommendBean
import com.shang.jetpackmoviekmp.network.datasource.MovieDataSource
import com.shang.jetpackmoviekmp.network.model.NetworkResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * [MovieRepository] 的預設實作，整合 [MovieDataSource]（network）與
 * [MovieCollectDao]／[MovieHistoryDao]（本地資料庫）。
 *
 * @param ioDispatcher 執行網路／資料庫操作用的 dispatcher；正式環境由
 *   `dataModule()` 透過 `commonModule()` 提供的 `CommonDispatcher.IO` qualifier 注入，
 *   測試可直接建構並傳入 test dispatcher，不需要透過 Koin。
 */
internal class MovieRepositoryImpl(
    private val movieDataSource: MovieDataSource,
    private val movieCollectDao: MovieCollectDao,
    private val movieHistoryDao: MovieHistoryDao,
    private val ioDispatcher: CoroutineDispatcher,
) : MovieRepository {

    override fun getConfiguration(): Flow<Result<ConfigurationBean>> {
        return flow {
            val response = movieDataSource.getConfiguration()
            if (response.isSuccess) {
                emit(Result.success(response.data!!))
            } else {
                emit(Result.failure(response.error ?: unexpectedNetworkFailure()))
            }
        }.flowOn(ioDispatcher)
    }

    override fun getMovieGenres(): Flow<Result<MovieGenreBean>> {
        return flow {
            val response = movieDataSource.getMovieGenres()
            if (response.isSuccess) {
                emit(Result.success(response.data!!))
            } else {
                emit(Result.failure(response.error ?: unexpectedNetworkFailure()))
            }
        }.flowOn(ioDispatcher)
    }

    override fun getMovieListPager(withGenres: String): Flow<PagingData<MovieCardResult>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 20,
                prefetchDistance = 2,
            ),
            pagingSourceFactory = {
                MovieGenrePagingSource(movieDataSource, withGenres)
            },
        ).flow
            .flowOn(ioDispatcher)
    }

    override fun getMovieSearchPager(query: String): Flow<PagingData<MovieCardResult>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 20,
                prefetchDistance = 2,
            ),
            pagingSourceFactory = {
                MovieSearchPagingSource(movieDataSource, query)
            },
        ).flow
            .flowOn(ioDispatcher)
    }

    override fun getMovieDetail(id: Int): Flow<Result<MovieDetailBean>> {
        return flow {
            val response = movieDataSource.getMovieDetail(id)
            if (response.isSuccess) {
                emit(Result.success(response.data!!))
            } else {
                emit(Result.failure(response.error ?: unexpectedNetworkFailure()))
            }
        }.flowOn(ioDispatcher)
    }

    override fun getMovieRecommendations(id: Int): Flow<Result<MovieRecommendBean>> {
        return flow {
            val response = movieDataSource.getMovieRecommendations(id)
            if (response.isSuccess) {
                emit(Result.success(response.data!!))
            } else {
                emit(Result.failure(response.error ?: unexpectedNetworkFailure()))
            }
        }.flowOn(ioDispatcher)
    }

    override fun getMovieActor(id: Int): Flow<Result<MovieCastAndCrewBean>> {
        return flow {
            val response = movieDataSource.getMovieActor(id)
            if (response.isSuccess) {
                emit(Result.success(response.data!!))
            } else {
                emit(Result.failure(response.error ?: unexpectedNetworkFailure()))
            }
        }.flowOn(ioDispatcher)
    }

    override fun getCollectedMovieIds(): Flow<List<Int>> {
        return movieCollectDao.collectedMovieIds().flowOn(ioDispatcher)
    }

    override fun getAllMovieCollect(): Flow<List<MovieCardResult>> {
        return movieCollectDao.getAllMovies()
            .map {
                it.map { entity ->
                    entity.asExtendedModel()
                }
            }
            .flowOn(ioDispatcher)
    }

    override fun getMovieCollectEntityById(id: Int): Flow<MovieCardResult?> {
        return movieCollectDao.getMovieCollectEntityById(id)
            .map { entity ->
                entity?.asExtendedModel()
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun insertMovieCollect(movieResult: MovieCardResult) {
        movieCollectDao.insertMovieCollect(movieResult.asCollectEntity())
    }

    override suspend fun deleteMovieCollect(movieResult: MovieCardResult) {
        movieCollectDao.deleteMovie(movieResult.asCollectEntity())
    }

    override suspend fun insertMovieHistory(movieResult: MovieCardResult) {
        movieHistoryDao.insertMovie(movieResult.asHistoryEntity())
    }

    override suspend fun deleteMovieHistory(movieResult: MovieCardResult) {
        movieHistoryDao.deleteMovie(movieResult.asHistoryEntity())
    }

    override fun getAllMovieHistory(): Flow<List<MovieCardResult>> {
        return movieHistoryDao.getAllMovies()
            .map { entities ->
                entities.map {
                    it.asExtendedModel()
                }
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun deleteAllMovieHistory(): Boolean {
        return movieHistoryDao.deleteAllMovies() > 0
    }

    /**
     * [NetworkResponse.isSuccess] 為 `false` 卻沒有帶 [NetworkResponse.error] 時的保底例外。
     *
     * 目前 `safeApiCall` 的實作在所有失敗路徑都會填入 `error`，理論上不會走到這裡；
     * 保留這個 fallback 是為了不讓型別系統沒鎖住的邊界情況（例如未來新增回傳
     * 2xx 但 body 為空的 endpoint）直接讓 `!!` NPE 崩潰。
     */
    private fun unexpectedNetworkFailure(): NetworkException =
        NetworkException.UnknownError(IllegalStateException("Response failed without an error"))
}
