package com.shang.jetpackmoviekmp.network.datasource

import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.MovieCastAndCrewBean
import com.shang.jetpackmoviekmp.model.MovieDetailBean
import com.shang.jetpackmoviekmp.model.MovieGenreBean
import com.shang.jetpackmoviekmp.model.MovieListBean
import com.shang.jetpackmoviekmp.model.MovieRecommendBean
import com.shang.jetpackmoviekmp.model.MovieSearchBean
import com.shang.jetpackmoviekmp.network.model.NetworkResponse

/**
 * Defines TMDB movie network operations used by the shared data layer.
 *
 * Each call returns [NetworkResponse] so callers can handle success and
 * network failures without depending on Ktor-specific exceptions.
 */
interface MovieDataSource {

    /**
     * Fetches TMDB image configuration and available size metadata.
     *
     * @return Configuration metadata mapped to [ConfigurationBean].
     */
    suspend fun getConfiguration(): NetworkResponse<ConfigurationBean>

    /**
     * Fetches the available movie genres from TMDB.
     *
     * @return Movie genre metadata mapped to [MovieGenreBean].
     */
    suspend fun getMovieGenres(): NetworkResponse<MovieGenreBean>

    /**
     * Fetches discover results for a genre filter.
     *
     * @param withGenres TMDB genre id filter, formatted for the `with_genres` query parameter.
     * @param page Result page to request.
     * @return Discover results mapped to [MovieListBean].
     */
    suspend fun getDiscoverMovie(withGenres: String, page: Int): NetworkResponse<MovieListBean>

    /**
     * Searches movies by text query.
     *
     * @param query Search keyword passed to TMDB.
     * @param page Result page to request.
     * @return Search results mapped to [MovieSearchBean].
     */
    suspend fun getMovieSearch(query: String, page: Int): NetworkResponse<MovieSearchBean>

    /**
     * Fetches a movie detail record.
     *
     * @param id TMDB movie id.
     * @return Movie detail mapped to [MovieDetailBean].
     */
    suspend fun getMovieDetail(id: Int): NetworkResponse<MovieDetailBean>

    /**
     * Fetches TMDB recommendations for a movie.
     *
     * @param id TMDB movie id.
     * @return Recommendation results mapped to [MovieRecommendBean].
     */
    suspend fun getMovieRecommendations(id: Int): NetworkResponse<MovieRecommendBean>

    /**
     * Fetches cast and crew credits for a movie.
     *
     * @param id TMDB movie id.
     * @return Cast and crew credits mapped to [MovieCastAndCrewBean].
     */
    suspend fun getMovieActor(id: Int): NetworkResponse<MovieCastAndCrewBean>
}
