package com.shang.jetpackmoviekmp.common

/**
 * 提供組合圖片路徑（poster／backdrop）所需的 TMDB 圖片 CDN base URL。
 */
interface BaseHostUrlProvider {
    /**
     * 回傳目前的圖片 CDN base URL，例如 `https://image.tmdb.org/t/p/`。
     *
     * 尚未持久化任何值時回傳空字串。
     */
    fun getBaseHostUrl(): String
}
