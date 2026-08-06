# After Report（Checkpoint 2：方案 B — Baseline Profile）

量測時間：2026-08-06

> **狀態：已結案**。SM-S9360 上的異常數據成因未明，但 SM-A715F 的同一 session A/B 對照未發現 Baseline Profile 造成回歸，且有初步正向訊號，決定保留方案 B，詳見下方「結論與決策」。

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

- [x] 在同一 session、相同條件下，暫時移除 `baseline-prof.txt`（`androidx.profileinstaller` 依賴保留不動），重跑 `StartupBenchmark` 作為對照組，跟有 baseline 的數字做 A/B 比較 — 見下方「同一 session A/B 測試（新裝置：SM-A715F）」；但因換了裝置，未直接解答上面 SM-S9360 那次異常的成因
- [ ] 依 A/B 結果補上結論段落，並回填 tasks.md Task 8.2／8.3

## 同一 session A/B 測試（新裝置：SM-A715F）

量測時間：2026-08-06（換裝置後，同一 session 內連續測試）

> 此裝置為換機後的新測試對象，跟上方 SM-S9360 的異常數據**不可直接比較絕對值**，但同一 session 內「無 baseline」與「有 baseline」兩組互相比較是有效的。

### 量測條件

| 項目 | 值 |
|---|---|
| 裝置 | Samsung SM-A715F（Galaxy A71）/ Android 13 |
| 連線方式 | ADB over USB |
| 動畫縮放 | 未特別設定（沿用裝置當時狀態） |
| 測試指令 | `./gradlew :benchmark:connectedCheck -Pandroid.testInstrumentationRunnerArguments.class=com.shang.benchmark.StartupBenchmark` |
| 「無 baseline」作法 | 暫時將 `androidApp/src/main/baseline-prof.txt` 移出（`androidx.profileinstaller` 依賴保留），跑 3 輪後還原檔案 |
| 「有 baseline」作法 | 還原 `baseline-prof.txt` 後（等同目前 committed 狀態）再跑 3 輪 |
| 執行順序 | 先無 baseline（3 輪），後有 baseline（3 輪）—未做順序對調，見下方注意事項 |

### 量測結果：timeToInitialDisplayMs

| 階段 | 輪次 | P50 (ms) | 5 次迭代 (ms) | SKIN 溫度／throttling status |
|---|---|---|---|---|
| 無 baseline | 1 | 1,923.19 | 1979.2 / 1923.2 / 1754.4 / 1791.9 / 2696.8 | 37.3°C / status 1 |
| 無 baseline | 2 | 2,569.47 | 2894.9 / 2569.5 / 2536.5 / 2586.6 / 2560.8 | 39.0°C / status 2 |
| 無 baseline | 3 | 1,787.79 | 2121.1 / 1758.3 / 1787.8 / 1666.7 / 1912.5 | 39.3°C / status 2 |
| 有 baseline | 1 | 2,450.36 | 2494.3 / 2485.5 / 2366.3 / 2450.4 / 2425.1 | 38.8°C / status 2 |
| 有 baseline | 2 | 1,673.44 | 1673.7 / 1646.7 / 1666.1 / 1675.1 / 1673.4 | 38.9°C / status 2 |
| 有 baseline | 3 | 1,666.73 | 1741.5 / 1666.7 / 1498.7 / 1705.8 / 1664.5 | 40.6°C / status 2 |

### 觀察

- **有 baseline 的第 2、3 輪高度收斂**（1,673ms vs 1,667ms，差 <0.5%），**無 baseline 的三輪彼此離散**（1,923 / 2,569 / 1,788ms，最大差距 ~44%），未收斂。
- 兩組**各自的第 1 輪都明顯偏高**（無 baseline 1,923ms、有 baseline 2,450ms）：這兩輪剛好都是「換版本後（移除／加回 baseline-prof.txt 觸發重新 package）首次執行」，較可能是換版後 ART 編譯／驗證的暖機開銷，非穩態冷啟動代表值，與 Task 6 已知的「跨版本切換首輪偏高」模式一致。
- 排除各自第 1 輪後：無 baseline 剩 2,569 / 1,788ms（仍大幅擺盪），有 baseline 剩 1,673 / 1,667ms（高度一致）。SKIN 溫度全程 38–41°C、throttling status 多維持在 2，單看溫度無法解釋無 baseline 組的大幅擺盪，跟 Task 6 的熱降頻假說對不上。
- 全部 15 個樣本 pooled 中位數：無 baseline ≈1,979ms，有 baseline ≈1,675ms，有 baseline 快約 15%。

### 待驗證的干擾因素（結論保守的原因）

本次測試**未對調執行順序**（固定先無 baseline、後有 baseline），且無 baseline 組本身雜訊很大（單輪變動可達 44%），樣本數也少（每組僅 3 輪 ×5 次）。因此「有 baseline 較快 15%」這個訊號**方向上與預期一致（正向）**，但目前證據力不足以視為確認結論，可能的干擾包含：

1. 順序效應：無 baseline 先跑，裝置／系統狀態可能因為先跑過一輪而在「有 baseline」階段變得更穩定，而非 baseline profile 本身的效果
2. 換版本後首輪的 ART 暖機開銷疊加在雜訊中，稀釋了訊號
3. 樣本數過少，無法排除單純運氣

若要提高信心，建議之後補測：把順序對調（先有 baseline、後無 baseline）重跑一次，或增加每組輪數，看「有 baseline 較穩定且較快」的模式是否還原。

## 結論與決策

- SM-S9360 上觀察到的異常（P50 遠高於 Checkpoint 0 基準）成因仍未查明，但 SM-A715F 的同一 session A/B 對照顯示**沒有證據指出 Baseline Profile 造成回歸**：有 baseline 的量測結果不比無 baseline 差，且後兩輪明顯更穩定收斂。
- 「有 baseline 較快 15%」的正向訊號因測試順序未對調、樣本數少，還不足以視為嚴謹確認的效能改善，但已足夠支撐**保留 Baseline Profile（不回退方案 B）**的決策：沒有回歸風險，且有初步正向跡象。
- 決定：**維持現行程式碼狀態**（`baseline-prof.txt`、`androidx.profileinstaller` 依賴保留），不回退方案 B。順序對調的補測列為可選的後續強化項目，不阻塞後續 Task 9 最終驗證。

## 附錄：擴充 BaselineProfileGenerator 涵蓋範圍的嘗試（結案，維持現狀）

在上述 A/B 對照之後，另外嘗試擴充 `BaselineProfileGenerator` 的涵蓋範圍，讓它在冷啟動後依序點擊底部導覽列（collect／search／history／setting），期望讓 profile 額外收錄這四個 feature module 的初次載入路徑（原本只涵蓋 Home）。為此對正式程式碼做了兩處修改：

- `MainActivity.kt`：`MainScreen` 根節點加上 `.semantics { testTagsAsResourceId = true }`，並為底部導覽項目加上 `Modifier.testTag("nav_<key>")`，讓 UiAutomator 能用不受裝置語言影響的 `resource-id` 定位並點擊
- `BaselineProfileGenerator.kt`：冷啟動後依序 `device.wait` + `click` 上述四個 `nav_*` resource-id

**驗證點擊機制本身確實有效**：手動安裝 benchmark APK、用 `adb shell input tap` 點擊 `nav_collect` 後 dump UI，畫面文字正確變成「目前沒有收藏」（`CollectScreen` 的空狀態文案），證實導覽有真的切換、畫面有真的渲染。

**但比對新舊 `baseline-prof.txt` 的差異（以行內容做集合比對，排除排序影響）後發現：`feature/collect`／`feature/search`／`feature/history`／`feature/setting` 這四個 package 完全沒有新增或移除的行**。919 行新增、650 行移除全部集中在：

1. `androidx.compose.ui.semantics`、`androidx.core.view.accessibility` 等——來自新加的 `testTagsAsResourceId` 本身（這個修改對**所有** build type 生效，不只 benchmark）
2. `coil3.network`、`ktor.client`、`okio` 等——本次量測時網路／圖片快取狀態不同造成的雜訊
3. 部分 `SPL`／`HSPL` flag 標記差異——ART profile 抽樣本身的 run-to-run 隨機性

推測最可能原因：`benchmark` build type 沿用 release 的 R8 最小化設定，`CollectScreen`／`HistoryScreen` 等只有單一呼叫點的小型 Composable 很可能被 R8 直接 inline 進呼叫端（`MainActivity.kt` 的 `mainEntry()`），導致沒有獨立 class 可以被 profile 記錄——程式碼確實執行了，但歸屬被合併、在這個粒度下看不出來。

**決策（與使用者確認）**：維持現狀，不回退。理由：

- `testTag`／`testTagsAsResourceId` 是 Google 官方建議的常規做法，風險低，對未來其他 UI 自動化測試有幫助
- 新產生的 `baseline-prof.txt` 本身沒有錯誤或回歸，只是沒有達到「擴充涵蓋範圍」的預期目標，維持現狀不會造成傷害
- 若要深入排查 R8 inline 的確切成因（比對 mapping.txt）或改用其他方式驗證涵蓋範圍，效益不確定且需要更多時間，暫不投入

此次嘗試對 Task 8 的結論（保留方案 B、未見回歸）沒有影響，僅供後續有人想繼續擴充 profile 涵蓋範圍時參考，避免重複踩同樣的坑。

### 交叉驗證：新舊 baseline-prof.txt 的 StartupBenchmark 對照（同裝置 SM-A715F）

量測時間：2026-08-06（同一 session，延續上面附錄的驗證）

用新產生的 `baseline-prof.txt`（含 `testTagsAsResourceId` 相關 startup 類別，但 `feature/collect|search|history|setting` 內容跟舊版完全相同）重跑 3 輪 `StartupBenchmark`，跟前面「同一 session A/B 測試」章節的舊 baseline 3 輪對照，驗證這次的程式碼變更是否影響冷啟動。

| 版本 | 輪次 | P50 (ms) | 5 次迭代 (ms) | SKIN 溫度 |
|---|---|---|---|---|
| 舊 baseline（前次） | 1 | 2,450.36 | 2494.3 / 2485.5 / 2366.3 / 2450.4 / 2425.1 | 38.8°C |
| 舊 baseline（前次） | 2 | 1,673.44 | 1673.7 / 1646.7 / 1666.1 / 1675.1 / 1673.4 | 38.9°C |
| 舊 baseline（前次） | 3 | 1,666.73 | 1741.5 / 1666.7 / 1498.7 / 1705.8 / 1664.5 | 40.6°C |
| 新 baseline（本次） | 1 | 2,349.63 | 2439.7 / 2306.7 / 2349.6 / 2342.7 / 2395.7 | 37.7°C |
| 新 baseline（本次） | 2 | 1,540.98 | 1965.9 / 1564.7 / 1541.0 / 1516.6 / 1400.7 | 38.5°C |
| 新 baseline（本次） | 3 | 1,617.07 | 1884.4 / 1617.1 / 1593.6 / 1598.0 / 2394.8 | 38.5°C |

**觀察**：

- 兩版都是**第 1 輪明顯偏高**，延續本報告已知的「換版本後首輪偏高」暖機模式（這次是覆蓋 `baseline-prof.txt` 觸發重新 package）
- 排除各自第 1 輪：舊版剩 1,673.44 / 1,666.73ms，新版剩 1,540.98 / 1,617.07ms，新版略快約 5–7%，但新版自身兩輪也有 5% 擺盪，差距落在雜訊範圍內，不視為有意義的改善
- 全部 15 個樣本 pooled 中位數：舊版 ≈1,675ms，新版 ≈1,884ms，pooled 反而偏慢，但主要是被新版第 1 輪暖機與第 2 輪一次離群值（1,965.9ms）拉高，非穩態差異

**結論**：新舊 `baseline-prof.txt` 的 `StartupBenchmark` 表現沒有觀察到有意義的差異，跟「兩者在 feature module 內容上完全相同、差異只在 startup 相關的 accessibility/semantics 類別與量測雜訊」的技術分析互相印證，屬於預期內的交叉驗證結果，未發現新的問題或回歸。
