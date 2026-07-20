package com.shang.jetpackmoviekmp.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItemColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

/**
 * Jetpack Movie 導覽元件的預設色彩設定。
 */
object JMNavigationDefaults {
    /**
     * 導覽元件未選取時的內容顏色
     */
    @Composable
    fun navigationContentColor() = MaterialTheme.colorScheme.onSurfaceVariant

    /**
     * 導覽元件選取時的內容顏色
     */
    @Composable
    fun navigationSelectedItemColor() = MaterialTheme.colorScheme.onPrimaryContainer

    /**
     * 導覽元件指示器顏色
     * 建議設為 primary，確保與 container 對比明顯，符合 Material3 標準
     */
    @Composable
    fun navigationIndicatorColor() = MaterialTheme.colorScheme.primary

    /**
     * 導覽列/抽屜/軌道的背景顏色
     * 建議使用 primaryContainer 以提升可視性，並自動隨主題切換
     */
    @Composable
    fun navigationContainerColor() = MaterialTheme.colorScheme.primaryContainer

    /**
     * 導覽列/抽屜/軌道的內容顏色
     */
    @Composable
    fun navigationOnContainerColor() = MaterialTheme.colorScheme.onSurface
}

/**
 * 依照視窗尺寸自動切換 bottom bar、navigation rail 或 drawer 的導覽 scaffold。
 *
 * @param navigationSuiteItems 導覽項目定義。
 * @param modifier 套用在 scaffold 上的修飾符。
 * @param windowAdaptiveInfo 目前視窗尺寸資訊，預設由系統取得。
 * @param content scaffold 主要內容。
 */
@Composable
fun JMNavigationSuiteScaffold(
    navigationSuiteItems: JMNavigationSuiteScope.() -> Unit,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
    content: @Composable () -> Unit,
) {
    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(windowAdaptiveInfo)
    val navigationSuiteItemColors = NavigationSuiteItemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = JMNavigationDefaults.navigationSelectedItemColor(),
            unselectedIconColor = JMNavigationDefaults.navigationContentColor(),
            selectedTextColor = JMNavigationDefaults.navigationSelectedItemColor(),
            unselectedTextColor = JMNavigationDefaults.navigationContentColor(),
            indicatorColor = JMNavigationDefaults.navigationIndicatorColor(),
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = JMNavigationDefaults.navigationSelectedItemColor(),
            unselectedIconColor = JMNavigationDefaults.navigationContentColor(),
            selectedTextColor = JMNavigationDefaults.navigationSelectedItemColor(),
            unselectedTextColor = JMNavigationDefaults.navigationContentColor(),
            indicatorColor = JMNavigationDefaults.navigationIndicatorColor(),
        ),
        navigationDrawerItemColors = NavigationDrawerItemDefaults.colors(
            selectedIconColor = JMNavigationDefaults.navigationSelectedItemColor(),
            unselectedIconColor = JMNavigationDefaults.navigationContentColor(),
            selectedTextColor = JMNavigationDefaults.navigationSelectedItemColor(),
            unselectedTextColor = JMNavigationDefaults.navigationContentColor(),
        ),
    )

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            JMNavigationSuiteScope(
                navigationSuiteScope = this,
                navigationSuiteItemColors = navigationSuiteItemColors,
            ).run(navigationSuiteItems)
        },
        modifier = modifier,
        layoutType = layoutType,
        containerColor = Color.Transparent,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = JMNavigationDefaults.navigationContainerColor(),
            navigationBarContentColor = JMNavigationDefaults.navigationOnContainerColor(),
            navigationRailContainerColor = JMNavigationDefaults.navigationContainerColor(),
            navigationRailContentColor = JMNavigationDefaults.navigationOnContainerColor(),
            navigationDrawerContainerColor = JMNavigationDefaults.navigationContainerColor(),
            navigationDrawerContentColor = JMNavigationDefaults.navigationOnContainerColor(),
        ),
    ) {
        content()
    }
}

/**
 * 提供統一色彩設定的 NavigationSuite item 建構 scope。
 *
 * @constructor 建立包裝 Material3 [NavigationSuiteScope] 的 scope。
 */
class JMNavigationSuiteScope internal constructor(
    private val navigationSuiteScope: NavigationSuiteScope,
    private val navigationSuiteItemColors: NavigationSuiteItemColors,
) {
    /**
     * 新增一個導覽項目。
     *
     * @param selected 目前項目是否被選取。
     * @param onClick 使用者點擊項目時的回呼。
     * @param modifier 套用在導覽項目上的修飾符。
     * @param icon 未選取狀態的圖示內容。
     * @param selectedIcon 選取狀態的圖示內容，預設與 [icon] 相同。
     * @param label 導覽項目的文字標籤。
     */
    fun item(
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        icon: @Composable () -> Unit,
        selectedIcon: @Composable () -> Unit = icon,
        label: @Composable (() -> Unit)? = null,
    ) = navigationSuiteScope.item(
        selected = selected,
        onClick = onClick,
        icon = {
            if (selected) {
                selectedIcon()
            } else {
                icon()
            }
        },
        label = label,
        colors = navigationSuiteItemColors,
        modifier = modifier,
    )
}

/**
 * JMNavigationSuiteScaffold 預覽範例。
 * 展示兩個 navigation item，並以 Box 作為內容。
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PreviewJMNavigationSuiteScaffold() {
    JMNavigationSuiteScaffold(
        navigationSuiteItems = {
            item(
                selected = true,
                onClick = {},
                icon = { Icon(Icons.Filled.Home, contentDescription = "首頁") },
                label = { Text("首頁") },
            )
            item(
                selected = false,
                onClick = {},
                icon = { Icon(Icons.Filled.Person, contentDescription = "搜尋") },
                label = { Text("搜尋") },
            )
            item(
                selected = false,
                onClick = {},
                icon = { Icon(Icons.Filled.Settings, contentDescription = "設定") },
                label = { Text("設定") },
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("內容區塊", modifier = Modifier)
        }
    }
}
