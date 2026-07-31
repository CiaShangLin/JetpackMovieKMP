package com.shang.jetpackmoviekmp.feature.detail.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.shang.jetpackmoviekmp.feature.detail.ui.MovieDetailScreen
import com.shang.jetpackmoviekmp.model.MovieCardData
import kotlinx.serialization.Serializable

/** 導向電影詳情頁所需的 Navigation 3 路由資料。 */
@Serializable
data class MovieDetailKey(val movieId: Int) : NavKey

/**
 * 建立電影詳情頁的 Navigation 3 entry。
 *
 * @param key 電影詳情路由資料。
 * @param onBackClick 使用者返回時的回呼。
 * @param onMovieClick 使用者選取推薦電影時的回呼。
 * @return 供 `NavDisplay` 使用的路由鍵與 entry 配對。
 */
fun movieDetailEntry(
    key: MovieDetailKey,
    onBackClick: () -> Unit,
    onMovieClick: (MovieCardData) -> Unit,
): Pair<NavKey, NavEntry<NavKey>> =
    key to NavEntry(key) {
        MovieDetailScreen(
            movieId = key.movieId,
            onBackClick = onBackClick,
            onMovieClick = onMovieClick,
        )
    }
