# Baseline Report（Checkpoint 0）

量測時間：2026-08-05

本報告為「優化前」基準量測，執行時**未修改任何啟動路徑程式碼**，僅驗證 `:benchmark` module 可成功量測並記錄基準數值，供 Checkpoint 1（方案 A：Koin 延遲載入）、Checkpoint 2（方案 B：Baseline Profile）比對。

## 量測條件

Checkpoint 1／2 MUST 沿用以下同一套條件：

| 項目 | 值 |
|---|---|
| 裝置型號 | Samsung SM-S9360（機身代號 `pa2q`） |
| Android 版本 | Android 16（SDK 36，build `BP4A.251205.006`） |
| CompilationMode | `run-from-apk`（未指定 `CompilationMode`，AndroidX Benchmark 於此裝置上實際採用的模式，即未經 AOT/Baseline Profile 預編譯） |
| StartupMode | `COLD` |
| 迭代次數 | 5 |
| 測試指令 | `./gradlew :benchmark:connectedCheck` |
| 測試類別 | `com.shang.benchmark.StartupBenchmark#startup` |

> 裝置端須確保「視窗動畫縮放」「轉場動畫縮放」「Animator 時長縮放」皆已關閉（`window_animation_scale` / `transition_animation_scale` / `animator_duration_scale` = 0），否則 Macrobenchmark 可能無法正確判斷 Activity 啟動完成。

## 量測結果：timeToInitialDisplayMs

| 統計量 | 數值 (ms) |
|---|---|
| 最小值 | 1,052.3 |
| **P50（中位數）** | **1,118.6** |
| 最大值 | 1,167.8 |
| 變異係數 | 4.02% |

各次迭代原始數值（ms）：1146.0、1167.8、1096.2、1052.3、1118.6

> **P90 說明**：本次僅 5 次迭代，樣本數不足以計算穩定的 P90，故本報告僅採 P50 作為主要比較基準；若後續 Checkpoint 需要更穩定的尾端延遲數據，可考慮提高 `iterations`（例如 10 次以上）後再重新量測全部三個 Checkpoint。

## Trace 檔案

原始 Perfetto trace 保留於：
`benchmark/build/outputs/connected_android_test_additional_output/benchmark/connected/SM-S9360 - 16/`

## 結論

`:benchmark` module 與 `StartupBenchmark` 已驗證可在實體裝置上成功執行並產出可比較的量測數據。此份基準數據將作為 Checkpoint 1（方案 A）、Checkpoint 2（方案 B）的比較基準。
