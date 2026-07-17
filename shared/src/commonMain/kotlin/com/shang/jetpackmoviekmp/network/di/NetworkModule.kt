package com.shang.jetpackmoviekmp.network.di

import com.shang.jetpackmoviekmp.BuildConfig
import com.shang.jetpackmoviekmp.network.datasource.MovieDataSource
import com.shang.jetpackmoviekmp.network.datasource.MovieDataSourceImpl
import com.shang.jetpackmoviekmp.network.provider.DefaultLanguageProvider
import com.shang.jetpackmoviekmp.network.provider.LanguageProvider
import com.shang.jetpackmoviekmp.sharedJson
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import org.koin.dsl.module

/**
 * 提供 shared 模組的 network 層依賴。
 *
 * @param isDebug 為 `true` 時啟用 request logging。
 * @param provideDefaultLanguageProvider 是否綁定 [DefaultLanguageProvider] 作為 [LanguageProvider]。
 * production DI 必須設為 `false`，改為提供 datastore-backed 的 [LanguageProvider]
 * （見 `datastoreModule`），避免兩者對同一型別重複綁定；預設為 `true`，
 * 讓本 module 在未搭配 datastore module 時（例如測試）仍可獨立使用。
 */
fun networkModule(isDebug: Boolean, provideDefaultLanguageProvider: Boolean = true) = module {
    if (provideDefaultLanguageProvider) {
        single<LanguageProvider> {
            DefaultLanguageProvider()
        }
    }
    single<HttpClient> {
        createHttpClient(languageProvider = get(), isDebug = isDebug)
    }
    single<MovieDataSource> {
        MovieDataSourceImpl(get())
    }
}

private fun createHttpClient(languageProvider: LanguageProvider, isDebug: Boolean): HttpClient {
    return HttpClient {
        configureMovieClient(languageProvider, isDebug)
    }
}

/**
 * Applies the common TMDB client configuration.
 *
 * This function is internal so common tests can configure `HttpClient(MockEngine)`
 * with the same base URL, query parameters, JSON parser, logging, and
 * `expectSuccess` behavior as production clients.
 *
 * @param languageProvider Supplies the request language for the `language` query parameter.
 * @param isDebug Enables Ktor logging when true.
 */
internal fun HttpClientConfig<*>.configureMovieClient(languageProvider: LanguageProvider, isDebug: Boolean) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(sharedJson)
    }
    install(Logging) {
        logger = Logger.SIMPLE
        level = if (isDebug) LogLevel.INFO else LogLevel.NONE
    }
    defaultRequest {
        url("https://api.themoviedb.org/3/")
        url.parameters.append("api_key", BuildConfig.TMDB_API_KEY)
        url.parameters.append("language", languageProvider.getLanguageCode())
    }
}
