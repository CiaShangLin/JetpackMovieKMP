package com.shang.jetpackmoviekmp.network.datasource

import com.shang.jetpackmoviekmp.datastore.InMemoryPreferencesDataStore
import com.shang.jetpackmoviekmp.datastore.UserPreferenceDataSource
import com.shang.jetpackmoviekmp.datastore.provider.DatastoreLanguageProvider
import com.shang.jetpackmoviekmp.model.LanguageMode
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val CONFIGURATION_JSON = """{
    "change_keys": [],
    "images": {
        "backdrop_sizes": [],
        "base_url": "",
        "logo_sizes": [],
        "poster_sizes": [],
        "profile_sizes": [],
        "secure_base_url": "",
        "still_sizes": []
    }
}"""

class DatastoreBackedLanguageRequestTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun requests_use_language_from_datastore_backed_provider_and_update_without_recreating_datasource() = runTest {
        val userPreferenceDataSource = UserPreferenceDataSource(InMemoryPreferencesDataStore())
        val languageProvider = DatastoreLanguageProvider(
            userPreferenceDataSource,
            CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        )
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

        userPreferenceDataSource.setLanguageMode(LanguageMode.ENGLISH)
        dataSource.getConfiguration()

        userPreferenceDataSource.setLanguageMode(LanguageMode.TRADITIONAL_CHINESE)
        dataSource.getConfiguration()

        assertEquals(listOf("en-US", "zh-TW"), requestedLanguages)
    }
}

private fun buildDataSource(
    engine: MockEngine,
    languageProvider: DatastoreLanguageProvider,
): MovieDataSourceImpl {
    val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json()
        }
        defaultRequest {
            url("https://api.themoviedb.org/3/")
            url.parameters.append("api_key", "")
            url.parameters.append("language", languageProvider.getLanguageCode())
        }
    }
    return MovieDataSourceImpl(client)
}
