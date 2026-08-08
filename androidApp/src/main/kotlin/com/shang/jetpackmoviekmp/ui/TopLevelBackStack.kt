package com.shang.jetpackmoviekmp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * 官方 Navigation 3 Common UI recipe 版本的頂層導覽 back stack：
 * 每個頂層 Tab（[topLevelStacks] 的 key）各自維護獨立的 sub back stack，
 * [backStack] 為所有 Tab 的 sub back stack 攤平後的 flat list，供 `NavDisplay` 消費。
 */
class TopLevelBackStack<T : Any>(startKey: T) {

    private var topLevelStacks: LinkedHashMap<T, SnapshotStateList<T>> = linkedMapOf(
        startKey to mutableStateListOf(startKey),
    )

    /** 目前所在的頂層 Tab key，只在呼叫 [addTopLevel] 或 [removeLast] 切換 Tab 時改變。 */
    var topLevelKey by mutableStateOf(startKey)
        private set

    /** 所有 Tab 的 sub back stack 攤平後的 flat list，直接交給 `NavDisplay` 渲染。 */
    val backStack = mutableStateListOf(startKey)

    private fun updateBackStack() {
        backStack.clear()
        backStack.addAll(topLevelStacks.flatMap { it.value })
    }

    /**
     * 切換到頂層 Tab [key]。
     *
     * 若該 Tab 尚未存在，建立一個只含自己的新 sub back stack；
     * 若已存在，保留其原有 sub back stack 內容，僅將其移到 [topLevelStacks] 尾端。
     *
     * @param key 要切換到的頂層 Tab key
     */
    fun addTopLevel(key: T) {
        if (topLevelStacks[key] == null) {
            topLevelStacks[key] = mutableStateListOf(key)
        } else {
            topLevelStacks.apply {
                remove(key)?.let {
                    put(key, it)
                }
            }
        }

        topLevelKey = key

        updateBackStack()
    }

    /**
     * 將 [key] 加入目前所在 Tab（[topLevelKey]）的 sub back stack 尾端。
     *
     * @param key 要加入的畫面 key
     */
    fun add(key: T) {
        topLevelStacks[topLevelKey]?.add(key)
        updateBackStack()
    }

    /**
     * 移除目前所在 Tab（[topLevelKey]）sub back stack 的最後一筆。
     *
     * 若移除後該 Tab 的 sub back stack 已空（原本只剩 Tab 根畫面），
     * 會將該 Tab 從 [topLevelStacks] 中移除，並切換到其餘 Tab 中最後被加入的一個。
     */
    fun removeLast() {
        val removedKey = topLevelStacks[topLevelKey]?.removeLastOrNull()
        if (removedKey != null && topLevelStacks[topLevelKey]?.isEmpty() == true) {
            topLevelStacks.remove(removedKey)
            topLevelKey = topLevelStacks.keys.last()
        }
        updateBackStack()
    }
}
