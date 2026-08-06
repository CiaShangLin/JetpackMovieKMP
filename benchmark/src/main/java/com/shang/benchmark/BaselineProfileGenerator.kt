package com.shang.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 產生 androidApp 的 Baseline Profile，供 android-cold-start-benchmark
 * 方案 B（Baseline Profile）使用；產生結果需手動複製到
 * `androidApp/src/main/baseline-prof.txt`（見 tasks.md Task 7 的說明）。
 *
 * 除了冷啟動本身，也依序切換底部導覽列的其餘頁籤，讓 profile 額外涵蓋
 * collect／search／history／setting 各 feature module 的初次載入路徑，
 * 不只侷限於 HomeKey。導覽項目透過 `nav_<key>`（見 MainActivity.kt 的
 * `Modifier.testTag`）以 resource-id 定位，不受裝置語言影響。
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = "com.shang.jetpackmoviekmp",
    ) {
        pressHome()
        startActivityAndWait()

        // 這幾個 resource-id 對應 MainActivity.kt 的 Modifier.testTag("nav_${item.name.lowercase()}")，
        // 兩邊需同步修改：MainNavItem 改名或新增分頁時，這裡也要跟著更新。
        listOf("nav_collect", "nav_search", "nav_history", "nav_setting").forEach { tag ->
            val found = device.wait(Until.hasObject(By.res(packageName, tag)), 5_000)
            check(found) { "找不到導覽項目 resource-id=$tag，BaselineProfileGenerator 的導覽 journey 可能已失效" }
            device.findObject(By.res(packageName, tag)).click()
            device.waitForIdle()
        }
    }
}
