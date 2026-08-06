package com.shang.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 產生 androidApp 的 Baseline Profile，供 android-cold-start-benchmark
 * 方案 B（Baseline Profile）使用；產生結果經 `androidx.baselineprofile`
 * plugin 自動寫入 `androidApp/src/main/baseline-prof.txt`。
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
    }
}
