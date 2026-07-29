package com.shang.jetpackmoviekmp.feature.history.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.shang.jetpackmoviekmp.feature.history.ui.HistoryScreen
import com.shang.jetpackmoviekmp.model.MovieCardData
import kotlinx.serialization.Serializable

/** 歷史頁在 Navigation3 導覽骨架中的目的地 key。 */
@Serializable
data object HistoryKey : NavKey

/**
 * 產生歷史頁對應的 [NavEntry]，供 `androidApp` 的 entryProvider 使用。
 *
 * @param onMovieClick 使用者點擊電影卡片時的回呼。
 * @return [HistoryKey] 與其對應的 [NavEntry]。
 */
fun historyEntry(onMovieClick: (MovieCardData) -> Unit): Pair<NavKey, NavEntry<NavKey>> =
    HistoryKey to NavEntry(HistoryKey) {
        HistoryScreen(onMovieClick = onMovieClick)
    }
