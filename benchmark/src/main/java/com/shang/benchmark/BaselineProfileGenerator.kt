package com.shang.benchmark

import android.os.SystemClock
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 產生 androidApp 的 Baseline Profile，供 android-cold-start-benchmark
 * 方案 B（Baseline Profile）使用；產生結果需手動複製到
 * `androidApp/src/main/baseline-prof.txt`（見 tasks.md Task 7 的說明）。
 *
 * 冷啟動後會捲動首頁片單清單：官方文件明確指出單純啟動＋等待並不足夠，
 * ART 只會記錄實際被反覆執行的 method，需要真的做出捲動／導覽等互動，
 * 才能讓 Compose LazyColumn 清單填充、Coil 圖片載入、Paging 分頁載入
 * 等路徑被記錄進 profile（見
 * https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile ）。
 *
 * 捲動改用座標式 `UiDevice.swipe`，不透過 resource-id／`Until.hasObject`
 * 定位元件：已證實 UiAutomator 的 accessibility 查詢在這個收集階段
 * （interpreted、self-instrumenting）下無法可靠偵測 Compose 節點，曾
 * 造成 generate() 不穩定失敗，詳見
 * openspec/changes/archive/2026-08-06-android-cold-start-benchmark/after-report-baseline-profile.md
 * 附錄。底部導覽列四個頁籤的點擊 journey 已證實對 profile 內容零貢獻
 * （R8 inline 進 `mainEntry()`），故不重新加入。
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

        // 讓設定資料的網路請求、Room 初始化跑完，首頁清單才有內容可捲動。
        SystemClock.sleep(10_000)

        val centerX = device.displayWidth / 2
        val topY = device.displayHeight / 4
        val bottomY = device.displayHeight * 3 / 4
        repeat(3) {
            device.swipe(centerX, bottomY, centerX, topY, 15)
            SystemClock.sleep(1_000)
        }
        device.swipe(centerX, topY, centerX, bottomY, 15)
        SystemClock.sleep(2_000)
    }
}
