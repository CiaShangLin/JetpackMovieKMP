package com.shang.jetpackmoviekmp.ui

import androidx.navigation3.runtime.NavKey
import com.shang.jetpackmoviekmp.feature.collect.navigation.CollectKey
import com.shang.jetpackmoviekmp.feature.home.navigation.HomeKey
import com.shang.jetpackmoviekmp.feature.search.navigation.SearchKey
import kotlin.test.Test
import kotlin.test.assertEquals

class MainNavBackStackTest {

    @Test
    fun `依序切換 Home 到 Collect 到 Search 時 backStack 依序累積`() {
        // Arrange
        val backStack = mutableListOf<NavKey>(HomeKey)

        // Act
        switchTab(backStack, CollectKey)
        switchTab(backStack, SearchKey)

        // Assert
        assertEquals(listOf(HomeKey, CollectKey, SearchKey), backStack)
    }

    @Test
    fun `已存在的 Tab 被再次點擊時移除舊位置並移到尾端`() {
        // Arrange
        val backStack = mutableListOf<NavKey>(HomeKey, CollectKey)

        // Act
        switchTab(backStack, HomeKey)

        // Assert
        assertEquals(listOf(CollectKey, HomeKey), backStack)
    }

    @Test
    fun `點擊目前已在頂端的 Tab 不重複加入`() {
        // Arrange
        val backStack = mutableListOf<NavKey>(HomeKey, CollectKey)

        // Act
        switchTab(backStack, CollectKey)

        // Assert
        assertEquals(listOf(HomeKey, CollectKey), backStack)
    }
}
