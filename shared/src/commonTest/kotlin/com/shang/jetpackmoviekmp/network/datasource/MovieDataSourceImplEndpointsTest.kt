package com.shang.jetpackmoviekmp.network.datasource

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun jsonEngine(url: (String) -> Unit, content: String): MockEngine =
    MockEngine { request ->
        url(request.url.toString())
        respond(
            content = content,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }

class MovieDataSourceImplEndpointsTest {

    @Test
    fun getMovieGenres_sends_request_to_genre_movie_list_and_maps_result() = runTest {
        var requestedUrl = ""
        val engine = jsonEngine(
            url = { requestedUrl = it },
            content = """{"genres":[{"id":28,"name":"Action"}]}""",
        )

        val result = buildDataSource(engine).getMovieGenres()

        assertTrue(requestedUrl.startsWith("https://api.themoviedb.org/3/genre/movie/list"))
        assertTrue(result.isSuccess)
        assertEquals(listOf(28), result.data?.genres?.map { it.id })
        assertEquals(listOf("Action"), result.data?.genres?.map { it.name })
    }

    @Test
    fun getDiscoverMovie_sends_with_genres_and_page_query_params_and_maps_result() = runTest {
        var requestedUrl = ""
        val engine = jsonEngine(
            url = { requestedUrl = it },
            content = """{"page":1,"results":[{"id":1,"title":"A"}],"total_pages":2,"total_results":3}""",
        )

        val result = buildDataSource(engine).getDiscoverMovie(withGenres = "28,12", page = 1)

        assertTrue(requestedUrl.startsWith("https://api.themoviedb.org/3/discover/movie"))
        assertTrue(requestedUrl.contains("with_genres=28%2C12") || requestedUrl.contains("with_genres=28,12"))
        assertTrue(requestedUrl.contains("page=1"))
        assertTrue(result.isSuccess)
        assertEquals(1, result.data?.page)
        assertEquals(listOf("A"), result.data?.results?.map { it.title })
    }

    @Test
    fun getMovieSearch_sends_query_and_page_query_params_and_maps_result() = runTest {
        var requestedUrl = ""
        val engine = jsonEngine(
            url = { requestedUrl = it },
            content = """{"page":1,"results":[{"id":2,"title":"B"}],"total_pages":1,"total_results":1}""",
        )

        val result = buildDataSource(engine).getMovieSearch(query = "batman", page = 1)

        assertTrue(requestedUrl.startsWith("https://api.themoviedb.org/3/search/movie"))
        assertTrue(requestedUrl.contains("query=batman"))
        assertTrue(result.isSuccess)
        assertEquals(listOf("B"), result.data?.results?.map { it.title })
    }

    @Test
    fun getMovieDetail_sends_request_to_movie_id_and_maps_result() = runTest {
        var requestedUrl = ""
        val engine = jsonEngine(
            url = { requestedUrl = it },
            content = """
                {
                    "id": 42,
                    "title": "The Matrix",
                    "runtime": 136,
                    "belongs_to_collection": {"id": 1, "name": "The Matrix Collection"},
                    "genres": [{"id": 28, "name": "Action"}],
                    "production_companies": [{"id": 79, "name": "Village Roadshow", "origin_country": "US"}],
                    "production_countries": [{"iso_3166_1": "US", "name": "United States"}],
                    "spoken_languages": [{"english_name": "English", "iso_639_1": "en", "name": "English"}]
                }
            """,
        )

        val result = buildDataSource(engine).getMovieDetail(id = 42)

        assertTrue(requestedUrl.startsWith("https://api.themoviedb.org/3/movie/42"))
        assertTrue(result.isSuccess)
        assertEquals(42, result.data?.id)
        assertEquals("The Matrix", result.data?.title)
        assertEquals(136, result.data?.runtime)
        assertEquals("The Matrix Collection", result.data?.belongsToCollection?.name)
        assertEquals(listOf("Action"), result.data?.genres?.map { it.name })
        assertEquals(listOf("Village Roadshow"), result.data?.productionCompanies?.map { it.name })
        assertEquals(listOf("United States"), result.data?.productionCountries?.map { it.name })
        assertEquals(listOf("English"), result.data?.spokenLanguages?.map { it.name })
    }

    @Test
    fun getMovieRecommendations_sends_request_to_movie_id_recommendations_and_maps_result() = runTest {
        var requestedUrl = ""
        val engine = jsonEngine(
            url = { requestedUrl = it },
            content = """{"page":1,"results":[{"id":3,"title":"C"}],"total_pages":1,"total_results":1}""",
        )

        val result = buildDataSource(engine).getMovieRecommendations(id = 42)

        assertTrue(requestedUrl.startsWith("https://api.themoviedb.org/3/movie/42/recommendations"))
        assertTrue(result.isSuccess)
        assertEquals(listOf("C"), result.data?.results?.map { it.title })
    }

    @Test
    fun getMovieActor_sends_request_to_movie_id_credits_and_maps_result() = runTest {
        var requestedUrl = ""
        val engine = jsonEngine(
            url = { requestedUrl = it },
            content = """{"id":42,"cast":[{"id":1,"name":"Keanu"}],"crew":[{"id":2,"name":"Wachowski","job":"Director"}]}""",
        )

        val result = buildDataSource(engine).getMovieActor(id = 42)

        assertTrue(requestedUrl.startsWith("https://api.themoviedb.org/3/movie/42/credits"))
        assertTrue(result.isSuccess)
        assertEquals(listOf("Keanu"), result.data?.cast?.map { it.name })
        assertEquals(listOf("Wachowski"), result.data?.crew?.map { it.name })
    }
}
