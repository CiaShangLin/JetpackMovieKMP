package com.shang.jetpackmoviekmp.network

import com.shang.jetpackmoviekmp.network.model.ConfigurationResponse

interface MovieDataSource {

    suspend fun getConfiguration(): ConfigurationResponse
}
