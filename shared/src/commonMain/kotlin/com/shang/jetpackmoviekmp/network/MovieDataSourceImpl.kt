package com.shang.jetpackmoviekmp.network

import com.shang.jetpackmoviekmp.network.model.ConfigurationResponse
import io.ktor.client.call.body
import io.ktor.client.request.get

class MovieDataSourceImpl : MovieDataSource {
    override suspend fun getConfiguration(): ConfigurationResponse {
        return httpClient.get("configuration").body()
    }
}
