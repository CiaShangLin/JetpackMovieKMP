package com.shang.jetpackmoviekmp.common

/**
 * Supplies the TMDB API language code used by network requests.
 */
interface LanguageProvider {
    /**
     * Returns a TMDB-compatible language code, for example `zh-TW`.
     */
    fun getLanguageCode(): String
}
