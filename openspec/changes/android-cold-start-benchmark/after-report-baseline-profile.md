# After Report（Checkpoint 2：方案 B — Baseline Profile）

量測時間：2026-08-06

> **狀態：調查中，數據異常，結論待補**。本報告先記錄目前量到的原始數據與觀察到的異常，待完成同一 session 內的 A/B 排查（有／無 Baseline Profile 對照）後再補上結論段落。

## 量測條件

| 項目 | 值 | 與 Checkpoint 0 的差異 |
|---|---|---|
| 裝置 | Samsung SM-S9360 / Android 16 | 相同 |
| 連線方式 | ADB over WiFi | Checkpoint 0 為 USB |
| 動畫縮放 | 已關閉（`window_animation_scale`／`transition_animation_scale`／`animator_duration_scale` = 0） | 相同 |
| CompilationMode | 未指定（沿用 `StartupBenchmark` 預設） | 相同 |
| 測試指令 | 見下方「量測結果」各筆記錄 | — |

程式碼狀態：Task 7 完成後的狀態（`androidApp/src/main/baseline-prof.txt` 已加入、`androidx.profileinstaller` 依賴已加入，`:benchmark` module 同時含 `StartupBenchmark` 與 `BaselineProfileGenerator` 兩個測試類別）。

## 量測結果：timeToInitialDisplayMs

### 第一次：`./gradlew :benchmark:connectedCheck`（同時跑了 `StartupBenchmark` 與 `BaselineProfileGenerator` 兩個測試）

- P50：**1,522.955ms**
- 各次迭代：1480.073、1537.258、1648.897、1443.441、1522.955
- 變異係數：5.09%
- 執行前裝置 SKIN 溫度：33.5°C（接近冷啟動狀態）

此次 `connectedCheck` 因 `:benchmark` module 同時含兩個測試類別，執行順序未受控，`BaselineProfileGenerator` 很可能先於 `StartupBenchmark` 執行並讓裝置先做過幾次冷啟動，是潛在污染源，故另外單獨重跑排除此因素。

### 第二次：單獨執行 `StartupBenchmark`（`-Pandroid.testInstrumentationRunnerArguments.class=com.shang.benchmark.StartupBenchmark`）

- P50：**1,505.715ms**
- 各次迭代：1619.130、1599.905、1505.715、1480.960、1482.159
- 變異係數：4.34%
- 執行前裝置 SKIN 溫度：34.2°C（接近冷啟動狀態）

## 觀察到的異常

兩次量測結果都遠高於 Checkpoint 0 基準（1,118.6ms），差距約 35~36%，方向跟預期相反（Baseline Profile 應該持平或改善，不應該讓冷啟動變慢）。

排除「裝置熱降頻」這個 Task 6 已知的干擾變因：第二次單獨量測中，**第一次迭代（裝置最涼、SKIN 34.2°C）反而是最慢的一次（1619ms）**，且 5 次迭代的數值是隨迭代次數遞減（1619 → 1600 → 1506 → 1481 → 1482），跟 Task 6 觀察到的「先涼後熱、熱了才變慢」模式相反，比較像是「前幾次迭代較慢、後面才穩定變快」的快取／狀態暖機模式。這代表這次的變慢**不太像單純裝置熱狀態造成**，較可能與以下因素有關（尚待驗證）：

1. `androidx.profileinstaller` 新增的執行期初始化開銷（每次冷啟動都會檢查 profile 安裝狀態）
2. 這個 session（WiFi 連線）跟 Checkpoint 0 那次 session（USB、不同天）之間存在其他未知的系統性差異（呼應 Task 6 已發現的「跨 session 比較不可靠」教訓）

## 待辦

- [ ] 在同一 session、相同條件下，暫時移除 Task 7 的改動（`baseline-prof.txt`、`androidx.profileinstaller` 依賴），重跑 `StartupBenchmark` 作為對照組，跟本報告的數字做 A/B 比較，排查是否為 Baseline Profile／profileinstaller 本身造成的迴歸
- [ ] 依 A/B 結果補上結論段落，並回填 tasks.md Task 8.2／8.3
