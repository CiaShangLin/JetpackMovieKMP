package com.shang.jetpackmoviekmp.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class MainNavItemTest {

    @Test
    fun `搜尋是第三個底部導覽項目`() {
        assertEquals(
            listOf(
                MainNavItem.HOME,
                MainNavItem.COLLECT,
                MainNavItem.SEARCH,
                MainNavItem.HISTORY,
                MainNavItem.SETTING,
            ),
            MainNavItem.entries.toList(),
        )
    }
}
