package com.shang.jetpackmoviekmp.network.datasource

import com.shang.jetpackmoviekmp.BuildConfig
import com.shang.jetpackmoviekmp.common.NetworkException
import com.shang.jetpackmoviekmp.network.di.configureMovieClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val CONFIGURATION_JSON = """{
    "change_keys": ["a"],
    "images": {
        "backdrop_sizes": ["w300"],
        "base_url": "http://image.tmdb.org/t/p/",
        "logo_sizes": [],
        "poster_sizes": [],
        "profile_sizes": [],
        "secure_base_url": "https://image.tmdb.org/t/p/",
        "still_sizes": []
    }
}"""

class MovieDataSourceImplTest {

    @Test
    fun getConfiguration_sends_request_to_full_tmdb_url_with_3_prefix() = runTest {
        var requestedUrl = ""
        val engine = MockEngine { request ->
            requestedUrl = request.url.toString()
            respond(
                content = CONFIGURATION_JSON,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        buildDataSource(engine).getConfiguration()

        assertTrue(
            requestedUrl.startsWith("https://api.themoviedb.org/3/configuration"),
            "expected URL to keep the /3/ prefix but was: $requestedUrl",
        )
    }

    @Test
    fun leading_slash_path_drops_the_3_base_path_prefix_regression_guard() = runTest {
        var requestedPath = ""
        val engine = MockEngine { request ->
            requestedPath = request.url.encodedPath
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            configureMovieClient(FakeLanguageProvider(), isDebug = true)
        }

        // 刻意示範 design.md 記錄的坑：路徑開頭多打一個 "/" 會讓 defaultRequest 的 "/3/" base path 整段被覆蓋掉。
        // MovieDataSourceImpl 目前所有端點都不帶開頭斜線，這個測試是反面示範，避免未來有人抄錯寫法。
        client.get("/configuration")

        assertEquals("/configuration", requestedPath)
    }

    @Test
    fun getConfiguration_request_includes_api_key_query_parameter() = runTest {
        var apiKeyParam: String? = null
        val engine = MockEngine { request ->
            apiKeyParam = request.url.parameters["api_key"]
            respond(
                content = CONFIGURATION_JSON,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        buildDataSource(engine).getConfiguration()

        assertEquals(BuildConfig.TMDB_API_KEY, apiKeyParam)
    }

    @Test
    fun language_parameter_reflects_current_value_across_requests() = runTest {
        val languageProvider = FakeLanguageProvider("en")
        val requestedLanguages = mutableListOf<String>()
        val engine = MockEngine { request ->
            requestedLanguages += request.url.parameters["language"].orEmpty()
            respond(
                content = CONFIGURATION_JSON,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val dataSource = buildDataSource(engine, languageProvider)

        dataSource.getConfiguration()
        languageProvider.currentLanguageCode = "zh-TW"
        dataSource.getConfiguration()

        assertEquals(listOf("en", "zh-TW"), requestedLanguages)
    }

    @Test
    fun getConfiguration_maps_successful_response_to_external_model() = runTest {
        val engine = MockEngine {
            respond(
                content = CONFIGURATION_JSON,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = buildDataSource(engine).getConfiguration()

        assertTrue(result.isSuccess)
        assertEquals(listOf("a"), result.data?.changeKeys)
        assertEquals("http://image.tmdb.org/t/p/", result.data?.images?.baseUrl)
    }

    @Test
    fun getConfiguration_maps_http_error_response_to_HttpError() = runTest {
        val engine = MockEngine {
            respond(content = "not found", status = HttpStatusCode.NotFound)
        }

        val result = buildDataSource(engine).getConfiguration()

        assertFalse(result.isSuccess)
        val error = assertIs<NetworkException.HttpError>(result.error)
        assertEquals(404, error.httpCode)
    }

    @Test
    fun getConfiguration_maps_malformed_json_body_to_ParseError() = runTest {
        val engine = MockEngine {
            respond(
                content = "not-json",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = buildDataSource(engine).getConfiguration()

        assertFalse(result.isSuccess)
        assertIs<NetworkException.ParseError>(result.error)
    }

    @Test
    fun getConfiguration_maps_timeout_to_TimeoutError() = runTest {
        val engine = MockEngine {
            throw SocketTimeoutException("timed out")
        }

        val result = buildDataSource(engine).getConfiguration()

        assertFalse(result.isSuccess)
        assertIs<NetworkException.TimeoutError>(result.error)
    }

    @Test
    fun getConfiguration_maps_connection_failure_to_ConnectionError() = runTest {
        val engine = MockEngine {
            throw IOException("no route to host")
        }

        val result = buildDataSource(engine).getConfiguration()

        assertFalse(result.isSuccess)
        assertIs<NetworkException.ConnectionError>(result.error)
    }

    @Test
    fun getConfiguration_maps_unrecognized_exception_to_UnknownError() = runTest {
        val engine = MockEngine {
            throw IllegalStateException("something unexpected")
        }

        val result = buildDataSource(engine).getConfiguration()

        assertFalse(result.isSuccess)
        assertIs<NetworkException.UnknownError>(result.error)
    }
}
