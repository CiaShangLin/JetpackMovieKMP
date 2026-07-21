package com.shang.jetpackmoviekmp.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Home
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.shang.jetpackmoviekmp.R
import com.shang.jetpackmoviekmp.feature.home.navigation.HomeKey

/**
 * 底部導覽列項目。`key` 對應 Navigation3 的 [NavKey]，取代舊版 `androidx.navigation` 的字串路由。
 *
 * 除 [HOME] 外的其餘項目皆待對應 feature module 導入後才會補上。
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

//    COLLECT(
//        selectedIcon = Icons.Rounded.Favorite,
//        unselectedIcon = Icons.Outlined.Favorite,
//        iconTextId = R.string.nav_favor,
//        titleTextId = R.string.nav_favor,
//        route = COLLECT_ROUTE,
//    ),
//
//    SEARCH(
//        selectedIcon = Icons.Rounded.Search,
//        unselectedIcon = Icons.Outlined.Search,
//        iconTextId = R.string.nav_search,
//        titleTextId = R.string.nav_search,
//        route = SEARCH_ROUTE,
//    ),
//
//    HISTORY(
//        selectedIcon = Icons.Rounded.History,
//        unselectedIcon = Icons.Outlined.History,
//        iconTextId = R.string.nav_history,
//        titleTextId = R.string.nav_history,
//        route = HISTORY_ROUTE,
//    ),
//
//    SETTING(
//        selectedIcon = Icons.Rounded.Settings,
//        unselectedIcon = Icons.Outlined.Settings,
//        iconTextId = R.string.nav_setting,
//        titleTextId = R.string.nav_setting,
//        route = SETTINGS_ROUTE,
//    ),
}
