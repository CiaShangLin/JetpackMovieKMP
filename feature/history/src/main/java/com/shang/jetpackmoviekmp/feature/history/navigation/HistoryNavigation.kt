package com.shang.jetpackmoviekmp.feature.history.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.shang.jetpackmoviekmp.feature.history.ui.HistoryScreen
import kotlinx.serialization.Serializable

/** 歷史頁在 Navigation3 導覽骨架中的目的地 key。 */
@Serializable
data object HistoryKey : NavKey

/**
 * 產生歷史頁對應的 [NavEntry]，供 `androidApp` 的 entryProvider 使用。
 *
 * @return [HistoryKey] 與其對應的 [NavEntry]。
 */
fun historyEntry(): Pair<NavKey, NavEntry<NavKey>> =
    HistoryKey to NavEntry(HistoryKey) {
        HistoryScreen()
    }
