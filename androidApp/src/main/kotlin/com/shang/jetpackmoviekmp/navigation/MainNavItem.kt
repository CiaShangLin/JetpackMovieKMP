package com.shang.jetpackmoviekmp.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.shang.jetpackmoviekmp.R
import com.shang.jetpackmoviekmp.feature.collect.navigation.CollectKey
import com.shang.jetpackmoviekmp.feature.history.navigation.HistoryKey
import com.shang.jetpackmoviekmp.feature.home.navigation.HomeKey
import com.shang.jetpackmoviekmp.feature.search.navigation.SearchKey
import com.shang.jetpackmoviekmp.feature.setting.navigation.SettingKey

/**
 * 底部導覽列項目。`key` 對應 Navigation3 的 [NavKey]，取代舊版 `androidx.navigation` 的字串路由。
 */
enum class MainNavItem(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val iconTextId: Int,
    @StringRes val titleTextId: Int,
    val key: NavKey,
) {

    HOME(
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home,
        iconTextId = R.string.nav_home,
        titleTextId = R.string.nav_home,
        key = HomeKey,
    ),

    COLLECT(
        selectedIcon = Icons.Rounded.Favorite,
        unselectedIcon = Icons.Outlined.Favorite,
        iconTextId = R.string.nav_favor,
        titleTextId = R.string.nav_favor,
        key = CollectKey,
    ),
    SEARCH(
        selectedIcon = Icons.Rounded.Search,
        unselectedIcon = Icons.Outlined.Search,
        iconTextId = R.string.nav_search,
        titleTextId = R.string.nav_search,
        key = SearchKey,
    ),
    HISTORY(
        selectedIcon = Icons.Rounded.History,
        unselectedIcon = Icons.Outlined.History,
        iconTextId = R.string.nav_history,
        titleTextId = R.string.nav_history,
        key = HistoryKey,
    ),
    SETTING(
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        iconTextId = R.string.nav_setting,
        titleTextId = R.string.nav_setting,
        key = SettingKey,
    ),
}
