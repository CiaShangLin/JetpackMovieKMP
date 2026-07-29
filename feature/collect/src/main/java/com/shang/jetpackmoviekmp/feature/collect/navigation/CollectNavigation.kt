package com.shang.jetpackmoviekmp.feature.collect.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.shang.jetpackmoviekmp.feature.collect.ui.CollectScreen
import com.shang.jetpackmoviekmp.model.MovieCardData
import kotlinx.serialization.Serializable

/** 收藏頁在 Navigation3 導覽骨架中的目的地 key。 */
@Serializable
data object CollectKey : NavKey

/**
 * 產生收藏頁對應的 [NavEntry]，供 `androidApp` 的 entryProvider 使用。
 *
 * @param onMovieClick 使用者點擊電影卡片時的回呼。
 * @return [CollectKey] 與其對應的 [NavEntry]。
 */
fun collectEntry(onMovieClick: (MovieCardData) -> Unit): Pair<NavKey, NavEntry<NavKey>> =
    CollectKey to NavEntry(CollectKey) {
        CollectScreen(onMovieClick = onMovieClick)
    }
