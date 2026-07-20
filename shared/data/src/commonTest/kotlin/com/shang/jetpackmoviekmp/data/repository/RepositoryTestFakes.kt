package com.shang.jetpackmoviekmp.data.repository

import com.shang.jetpackmoviekmp.common.NetworkException
import com.shang.jetpackmoviekmp.database.dao.MovieCollectDao
import com.shang.jetpackmoviekmp.database.dao.MovieHistoryDao
import com.shang.jetpackmoviekmp.database.entity.MovieCollectEntity
import com.shang.jetpackmoviekmp.database.entity.MovieHistoryEntity
import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.MovieCastAndCrewBean
import com.shang.jetpackmoviekmp.model.MovieDetailBean
import com.shang.jetpackmoviekmp.model.MovieGenreBean
import com.shang.jetpackmoviekmp.model.MovieListBean
import com.shang.jetpackmoviekmp.model.MovieRecommendBean
import com.shang.jetpackmoviekmp.model.MovieSearchBean
import com.shang.jetpackmoviekmp.network.datasource.MovieDataSource
import com.shang.jetpackmoviekmp.network.model.NetworkResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * [MovieDataSource] 測試替身，每個方法皆可獨立指定回傳值，預設為成功回應。
 *
 * 開放繼承（`open`），供需要覆寫單一方法拋出例外（例如 [kotlinx.coroutines.CancellationException]）
 * 的測試情境使用，見 `MovieGenrePagingSourceTest`／`MovieSearchPagingSourceTest`。
 */
internal open class FakeMovieDataSource : MovieDataSource {
    var configurationResponse: NetworkResponse<ConfigurationBean> =
        NetworkResponse(code = 200, data = ConfigurationBean())
    var movieGenresResponse: NetworkResponse<MovieGenreBean> =
        NetworkResponse(code = 200, data = MovieGenreBean())
    var discoverMovieResponse: NetworkResponse<MovieListBean> =
        NetworkResponse(code = 200, data = MovieListBean())
    var movieSearchResponse: NetworkResponse<MovieSearchBean> =
        NetworkResponse(code = 200, data = MovieSearchBean())
    var movieDetailResponse: NetworkResponse<MovieDetailBean> =
        NetworkResponse(code = 200, data = MovieDetailBean())
    var movieRecommendationsResponse: NetworkResponse<MovieRecommendBean> =
        NetworkResponse(code = 200, data = MovieRecommendBean())
    var movieActorResponse: NetworkResponse<MovieCastAndCrewBean> =
        NetworkResponse(code = 200, data = MovieCastAndCrewBean())

    override suspend fun getConfiguration(): NetworkResponse<ConfigurationBean> = configurationResponse

    override suspend fun getMovieGenres(): NetworkResponse<MovieGenreBean> = movieGenresResponse

    override suspend fun getDiscoverMovie(withGenres: String, page: Int): NetworkResponse<MovieListBean> =
        discoverMovieResponse

    override suspend fun getMovieSearch(query: String, page: Int): NetworkResponse<MovieSearchBean> =
        movieSearchResponse

    override suspend fun getMovieDetail(id: Int): NetworkResponse<MovieDetailBean> = movieDetailResponse

    override suspend fun getMovieRecommendations(id: Int): NetworkResponse<MovieRecommendBean> =
        movieRecommendationsResponse

    override suspend fun getMovieActor(id: Int): NetworkResponse<MovieCastAndCrewBean> = movieActorResponse
}

internal fun failureResponse(): NetworkException = NetworkException.UnknownError(Exception("boom"))

/**
 * [MovieCollectDao] 記憶體內測試替身，以 [MutableStateFlow] 模擬 Room 的 reactive query。
 */
internal class FakeMovieCollectDao : MovieCollectDao {
    private val state = MutableStateFlow<List<MovieCollectEntity>>(emptyList())

    override fun getAllMovies() = state.map { it }

    override suspend fun insertMovieCollect(entity: MovieCollectEntity) {
        state.value = state.value.filterNot { it.id == entity.id } + entity
    }

    override suspend fun deleteMovie(entity: MovieCollectEntity) {
        state.value = state.value.filterNot { it.id == entity.id }
    }

    override fun collectedMovieIds() = state.map { list -> list.map { it.id } }

    override fun getMovieCollectEntityById(id: Int) = state.map { list -> list.find { it.id == id } }
}

/**
 * [MovieHistoryDao] 記憶體內測試替身，以 [MutableStateFlow] 模擬 Room 的 reactive query。
 */
internal class FakeMovieHistoryDao : MovieHistoryDao {
    private val state = MutableStateFlow<List<MovieHistoryEntity>>(emptyList())

    override fun getAllMovies() = state.map { it }

    override suspend fun insertMovie(movie: MovieHistoryEntity) {
        state.value = state.value.filterNot { it.id == movie.id } + movie
    }

    override suspend fun deleteMovie(movie: MovieHistoryEntity) {
        state.value = state.value.filterNot { it.id == movie.id }
    }

    override suspend fun deleteAllMovies(): Int {
        val count = state.value.size
        state.value = emptyList()
        return count
    }
}
