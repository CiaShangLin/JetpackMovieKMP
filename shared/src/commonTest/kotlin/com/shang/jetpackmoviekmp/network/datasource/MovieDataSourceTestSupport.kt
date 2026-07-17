package com.shang.jetpackmoviekmp.network.datasource

import com.shang.jetpackmoviekmp.network.di.configureMovieClient
import com.shang.jetpackmoviekmp.network.provider.LanguageProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine

internal class FakeLanguageProvider(var currentLanguageCode: String = "en") : LanguageProvider {
    override fun getLanguageCode(): String = currentLanguageCode
}

internal fun buildDataSource(
    engine: MockEngine,
    languageProvider: LanguageProvider = FakeLanguageProvider(),
): MovieDataSourceImpl {
    val client = HttpClient(engine) {
        configureMovieClient(languageProvider, isDebug = true)
    }
    return MovieDataSourceImpl(client)
}
