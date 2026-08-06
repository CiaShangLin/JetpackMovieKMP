package com.shang.jetpackmoviekmp.ui

import androidx.navigation3.runtime.NavKey

/**
 * 以 MRU（Most-Recently-Used）方式切換底部導覽 Tab：
 * 若 [target] 已存在於 [backStack] 中，先移除舊位置的項目，再加入尾端；
 * 否則直接加入尾端。
 */
internal fun switchTab(backStack: MutableList<NavKey>, target: NavKey) {
    backStack.remove(target)
    backStack.add(target)
}
