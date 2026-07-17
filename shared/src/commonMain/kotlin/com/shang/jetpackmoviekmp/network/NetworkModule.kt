package com.shang.jetpackmoviekmp.network

import com.shang.jetpackmoviekmp.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val httpClient = HttpClient {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
            },
        )
    }
    install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.INFO
    }
    defaultRequest {
        url("https://api.themoviedb.org/3/")
        url.parameters.append("api_key", BuildConfig.TMDB_API_KEY)
    }
//    parameters {
//        "language" to
//    }
}
