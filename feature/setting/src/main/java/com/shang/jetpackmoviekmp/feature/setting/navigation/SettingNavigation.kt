package com.shang.jetpackmoviekmp.feature.setting.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.shang.jetpackmoviekmp.feature.setting.ui.SettingScreen
import kotlinx.serialization.Serializable

/** 設定頁在 Navigation3 導覽骨架中的目的地 key。 */
@Serializable
data object SettingKey : NavKey

/**
 * 產生設定頁對應的 [NavEntry]，供 `androidApp` 的 entryProvider 使用。
 *
 * @return [SettingKey] 與其對應的 [NavEntry]。
 */
fun settingEntry(): Pair<NavKey, NavEntry<NavKey>> =
    SettingKey to NavEntry(SettingKey) {
        SettingScreen()
    }
