package com.shang.jetpackmoviekmp.ui

import androidx.navigation3.runtime.NavKey
import com.shang.jetpackmoviekmp.feature.collect.navigation.CollectKey
import com.shang.jetpackmoviekmp.feature.detail.navigation.MovieDetailKey
import com.shang.jetpackmoviekmp.feature.home.navigation.HomeKey
import com.shang.jetpackmoviekmp.feature.search.navigation.SearchKey
import kotlin.test.Test
import kotlin.test.assertEquals

class TopLevelBackStackTest {

    @Test
    fun `addTopLevel 切換 Tab 後保留該 Tab 原有 sub back stack`() {
        // Arrange
        val topLevelBackStack = TopLevelBackStack<NavKey>(HomeKey)
        topLevelBackStack.addTopLevel(CollectKey)
        topLevelBackStack.add(MovieDetailKey(1))
        topLevelBackStack.addTopLevel(HomeKey)

        // Act
        topLevelBackStack.addTopLevel(CollectKey)

        // Assert
        assertEquals(CollectKey, topLevelBackStack.topLevelKey)
        assertEquals(listOf(HomeKey, CollectKey, MovieDetailKey(1)), topLevelBackStack.backStack)
    }

    @Test
    fun `不同 Tab 的 sub back stack 互不影響`() {
        // Arrange
        val topLevelBackStack = TopLevelBackStack<NavKey>(HomeKey)

        // Act
        topLevelBackStack.addTopLevel(CollectKey)
        topLevelBackStack.addTopLevel(SearchKey)

        // Assert
        assertEquals(listOf(HomeKey, CollectKey, SearchKey), topLevelBackStack.backStack)
    }

    @Test
    fun `add 與 removeLast 只影響目前 Tab 的 sub back stack`() {
        // Arrange
        val topLevelBackStack = TopLevelBackStack<NavKey>(HomeKey)
        topLevelBackStack.addTopLevel(CollectKey)
        topLevelBackStack.add(MovieDetailKey(5))

        // Act
        topLevelBackStack.removeLast()

        // Assert
        assertEquals(CollectKey, topLevelBackStack.topLevelKey)
        assertEquals(listOf(HomeKey, CollectKey), topLevelBackStack.backStack)
    }

    @Test
    fun `Tab 根畫面按返回鍵切到其餘 Tab 中最後加入的一個`() {
        // Arrange
        val topLevelBackStack = TopLevelBackStack<NavKey>(HomeKey)
        topLevelBackStack.addTopLevel(CollectKey)
        topLevelBackStack.addTopLevel(SearchKey)

        // Act
        topLevelBackStack.removeLast()

        // Assert
        assertEquals(CollectKey, topLevelBackStack.topLevelKey)
        assertEquals(listOf(HomeKey, CollectKey), topLevelBackStack.backStack)
    }

    @Test
    fun `只剩最後一個 Tab 時 backStack 只剩 1 筆`() {
        // Arrange
        val topLevelBackStack = TopLevelBackStack<NavKey>(HomeKey)
        topLevelBackStack.addTopLevel(CollectKey)

        // Act
        topLevelBackStack.removeLast()

        // Assert
        assertEquals(HomeKey, topLevelBackStack.topLevelKey)
        assertEquals(listOf<NavKey>(HomeKey), topLevelBackStack.backStack)
    }
}
