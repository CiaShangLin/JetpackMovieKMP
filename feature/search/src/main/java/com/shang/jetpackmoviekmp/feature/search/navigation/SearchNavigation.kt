package com.shang.jetpackmoviekmp.feature.search.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.shang.jetpackmoviekmp.feature.search.ui.SearchScreen
import com.shang.jetpackmoviekmp.model.MovieCardData
import kotlinx.serialization.Serializable

/** 搜尋頁在 Navigation3 導覽骨架中的目的地 key。 */
@Serializable
data object SearchKey : NavKey

/**
 * 產生搜尋頁對應的 [NavEntry]，供 `androidApp` 的 entryProvider 使用。
 *
 * @param onMovieClick 使用者點擊電影卡片時的回呼。
 * @return [SearchKey] 與其對應的 [NavEntry]。
 */
fun searchEntry(onMovieClick: (MovieCardData) -> Unit): Pair<NavKey, NavEntry<NavKey>> =
    SearchKey to NavEntry(SearchKey) {
        SearchScreen(onMovieClick = onMovieClick)
    }
