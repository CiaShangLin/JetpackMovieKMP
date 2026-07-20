package com.shang.jetpackmoviekmp.network.datasource

import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.MovieCastAndCrewBean
import com.shang.jetpackmoviekmp.model.MovieDetailBean
import com.shang.jetpackmoviekmp.model.MovieGenreBean
import com.shang.jetpackmoviekmp.model.MovieListBean
import com.shang.jetpackmoviekmp.model.MovieRecommendBean
import com.shang.jetpackmoviekmp.model.MovieSearchBean
import com.shang.jetpackmoviekmp.network.extension.mapData
import com.shang.jetpackmoviekmp.network.extension.safeApiCall
import com.shang.jetpackmoviekmp.network.model.ConfigurationResponse
import com.shang.jetpackmoviekmp.network.model.DiscoverMovieResponse
import com.shang.jetpackmoviekmp.network.model.MovieCastAndCrewResponse
import com.shang.jetpackmoviekmp.network.model.MovieDetailResponse
import com.shang.jetpackmoviekmp.network.model.MovieGenreResponse
import com.shang.jetpackmoviekmp.network.model.MovieRecommendResponse
import com.shang.jetpackmoviekmp.network.model.NetworkResponse
import com.shang.jetpackmoviekmp.network.model.SearchMovieResponse
import com.shang.jetpackmoviekmp.network.model.asExternalModel
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Ktor-backed implementation of [MovieDataSource].
 *
 * The injected [httpClient] owns base URL, API key, language, logging, and JSON
 * configuration. This class is responsible only for endpoint paths and mapping
 * DTO responses into shared external models.
 */
class MovieDataSourceImpl(private val httpClient: HttpClient) : MovieDataSource {

    override suspend fun getConfiguration(): NetworkResponse<ConfigurationBean> {
        return safeApiCall<ConfigurationResponse> {
            httpClient.get("configuration")
        }.mapData { it.asExternalModel() }
    }

    override suspend fun getMovieGenres(): NetworkResponse<MovieGenreBean> {
        return safeApiCall<MovieGenreResponse> {
            httpClient.get("genre/movie/list")
        }.mapData { it.asExternalModel() }
    }

    override suspend fun getDiscoverMovie(withGenres: String, page: Int): NetworkResponse<MovieListBean> {
        return safeApiCall<DiscoverMovieResponse> {
            httpClient.get("discover/movie") {
                parameter("with_genres", withGenres)
                parameter("page", page)
            }
        }.mapData { it.asExternalModel() }
    }

    override suspend fun getMovieSearch(query: String, page: Int): NetworkResponse<MovieSearchBean> {
        return safeApiCall<SearchMovieResponse> {
            httpClient.get("search/movie") {
                parameter("query", query)
                parameter("page", page)
            }
        }.mapData { it.asExternalModel() }
    }

    override suspend fun getMovieDetail(id: Int): NetworkResponse<MovieDetailBean> {
        return safeApiCall<MovieDetailResponse> {
            httpClient.get("movie/$id")
        }.mapData { it.asExternalModel() }
    }

    override suspend fun getMovieRecommendations(id: Int): NetworkResponse<MovieRecommendBean> {
        return safeApiCall<MovieRecommendResponse> {
            httpClient.get("movie/$id/recommendations")
        }.mapData { it.asExternalModel() }
    }

    override suspend fun getMovieActor(id: Int): NetworkResponse<MovieCastAndCrewBean> {
        return safeApiCall<MovieCastAndCrewResponse> {
            httpClient.get("movie/$id/credits")
        }.mapData { it.asExternalModel() }
    }
}
