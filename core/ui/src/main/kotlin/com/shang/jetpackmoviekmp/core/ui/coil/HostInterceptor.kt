package com.shang.jetpackmoviekmp.core.ui.coil

import coil3.intercept.Interceptor
import coil3.request.ImageResult
import com.shang.jetpackmoviekmp.common.BaseHostUrlProvider

/**
 * Coil request interceptor，將 TMDB 相對圖片路徑補上目前設定的 host。
 *
 * @property baseHostUrlProvider 提供圖片 CDN base URL 的來源。
 */
class HostInterceptor(
    private val baseHostUrlProvider: BaseHostUrlProvider,
) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val baseUrl = baseHostUrlProvider.getBaseHostUrl()
        val request = chain.request
        val originalUrl = request.data.toString()
        val newUrl = when {
            baseUrl.isEmpty() -> originalUrl
            originalUrl.startsWith("http://") || originalUrl.startsWith("https://") -> originalUrl
            else -> "${baseUrl}original$originalUrl"
        }

        return if (newUrl != originalUrl) {
            chain.withRequest(
                request.newBuilder()
                    .data(newUrl)
                    .build(),
            ).proceed()
        } else {
            chain.proceed()
        }
    }
}
