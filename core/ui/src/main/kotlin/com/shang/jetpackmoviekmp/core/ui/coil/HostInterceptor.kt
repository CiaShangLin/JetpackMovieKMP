package com.shang.jetpackmoviekmp.core.ui.coil

import coil3.Extras
import coil3.getExtra
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import com.shang.jetpackmoviekmp.common.BaseHostUrlProvider

/**
 * TMDB 圖片尺寸提示。呼叫端透過 Coil [Extras] 夾帶在 `ImageRequest` 上，
 * 讓 [HostInterceptor] 依顯示情境（清單縮圖 vs 詳情頁大圖）組出對應尺寸的圖片 URL；
 * 未夾帶時維持 [ORIGINAL] 行為，向後相容既有呼叫端。
 */
object TmdbImageSize {
    /** 夾帶尺寸提示的 [Extras] key；未設定時預設為 `null`，維持 [ORIGINAL] 行為。 */
    val key = Extras.Key<String?>(default = null)

    /** 詳情頁 backdrop 等大圖情境使用的預設尺寸（維持既有行為）。 */
    const val ORIGINAL = "original"

    /** 清單卡片縮圖使用的尺寸，略大於卡片實際顯示寬度以避免高密度螢幕模糊。 */
    const val LIST_THUMBNAIL = "w342"
}

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
        val sizeSegment = request.getExtra(TmdbImageSize.key) ?: TmdbImageSize.ORIGINAL
        val newUrl = buildTmdbImageUrl(baseUrl, originalUrl, sizeSegment)

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

/**
 * 依 [sizeSegment] 組出完整的 TMDB 圖片 URL。
 * 已是完整網址或尚未取得 base URL 時原樣返回，不重寫。
 */
internal fun buildTmdbImageUrl(baseUrl: String, path: String, sizeSegment: String): String =
    when {
        baseUrl.isEmpty() -> path
        path.startsWith("http://") || path.startsWith("https://") -> path
        else -> "$baseUrl$sizeSegment$path"
    }
