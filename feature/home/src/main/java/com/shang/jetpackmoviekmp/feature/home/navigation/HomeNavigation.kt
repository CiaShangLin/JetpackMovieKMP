package com.shang.jetpackmoviekmp.feature.home.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.shang.jetpackmoviekmp.feature.home.ui.HomeScreen
import kotlinx.serialization.Serializable

/**
 * `feature/home` 首頁在 Navigation3 導覽骨架中的進入點，取代 classic Navigation Compose
 * 的字串路由 `HOME_ROUTE`。
 */
@Serializable
data object HomeKey : NavKey

/**
 * 產生首頁對應的 [NavEntry]，供呼叫端（`androidApp`）的 `entryProvider` 使用。
 *
 * @param onMovieClick 使用者點擊電影卡片時的回呼。
 * @return [HomeKey] 與其對應 [NavEntry] 的組合。
 */
fun homeEntry(onMovieClick: (Int) -> Unit): Pair<NavKey, NavEntry<NavKey>> =
    HomeKey to NavEntry(HomeKey) {
        HomeScreen(
            onMovieClick = onMovieClick,
        )
    }
