package com.shang.jetpackmoviekmp.core.ui.coil

import kotlin.test.Test
import kotlin.test.assertEquals

class HostInterceptorTest {

    @Test
    fun `帶尺寸提示時使用對應 TMDB path segment`() {
        assertEquals(
            "https://image.tmdb.org/t/p/w342/poster.jpg",
            buildTmdbImageUrl(
                baseUrl = "https://image.tmdb.org/t/p/",
                path = "/poster.jpg",
                sizeSegment = TmdbImageSize.LIST_THUMBNAIL,
            ),
        )
    }

    @Test
    fun `未帶尺寸提示時維持 original`() {
        assertEquals(
            "https://image.tmdb.org/t/p/original/backdrop.jpg",
            buildTmdbImageUrl(
                baseUrl = "https://image.tmdb.org/t/p/",
                path = "/backdrop.jpg",
                sizeSegment = TmdbImageSize.ORIGINAL,
            ),
        )
    }

    @Test
    fun `已是完整網址時原樣返回不重寫`() {
        val fullUrl = "https://fastly.picsum.photos/id/1020/400/300.jpg"
        assertEquals(
            fullUrl,
            buildTmdbImageUrl(
                baseUrl = "https://image.tmdb.org/t/p/",
                path = fullUrl,
                sizeSegment = TmdbImageSize.LIST_THUMBNAIL,
            ),
        )
    }

    @Test
    fun `尚未取得 base URL 時原樣返回`() {
        assertEquals(
            "/poster.jpg",
            buildTmdbImageUrl(
                baseUrl = "",
                path = "/poster.jpg",
                sizeSegment = TmdbImageSize.LIST_THUMBNAIL,
            ),
        )
    }
}
