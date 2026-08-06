# After Report（Checkpoint 1：方案 A — Koin 延遲載入）

量測時間：2026-08-06

## 背景

方案 A（Koin 依 nav destination 延遲載入 feature module，commit `0947f3f`）實作完成後，人工試跑時觀察到冷啟動「感覺變慢」，因此在正式重跑 Checkpoint 1 之前，先以受控 A/B 測試排查：這個「變慢」是程式碼造成的，還是量測環境雜訊。

## 量測條件（與 baseline-report.md 的差異）

本次為**受控 A/B 診斷測試**，非嚴格複用 Checkpoint 0 條件的正式 Checkpoint 1：

| 項目 | 值 | 與 Checkpoint 0 的差異 |
|---|---|---|
| 裝置 | Samsung SM-S9360 / Android 16 | 相同 |
| 連線方式 | ADB over WiFi | Checkpoint 0 為 USB |
| 動畫縮放 | **未關閉**（維持裝置預設） | Checkpoint 0 已關閉 |
| 迭代次數 | 每輪 5 次，共跑 5 輪（合計 25 個樣本） | Checkpoint 0 僅 1 輪 5 次 |
| 測試指令 | `./gradlew :benchmark:connectedCheck`（同一 session 內，eager／延遲載入版各連續跑 5 輪） | 同 |

因為動畫未關閉、走 WiFi，本次數字**不可直接跟 baseline-report.md 的 1,118.6ms 比較絕對值**；但由於 eager 版與延遲載入版都在同一 session、同樣條件下測，兩者互相比較是有效的。

## 測試方法

1. 先跑「加回去」版本（`JetpackMovieApplication.onCreate()` 同步載入全部 8 個模組，等同回退 `0947f3f`）連續 5 輪。
2. 切回延遲載入版本（`0947f3f` 原始程式碼），裝置降溫至 SKIN 溫度 ≤33°C 後，同樣連續跑 5 輪。
3. 兩組各 25 個 `timeToInitialDisplayMs` 樣本用於比較。

## 量測結果：timeToInitialDisplayMs

### 加回去版（Eager，5 輪 P50）

| 輪次 | P50 (ms) | 變異係數 |
|---|---|---|
| 1 | 857.7 | 9.83% |
| 2 | 928.9 | 5.46% |
| 3 | 1,527.0 | 8.76% |
| 4 | 1,549.0 | 2.98% |
| 5 | 1,507.6 | 19.91% |

### 延遲載入版（Lazy，5 輪 P50）

| 輪次 | P50 (ms) | 變異係數 |
|---|---|---|
| 1 | 948.8 | 9.87% |
| 2 | 921.5 | 34.45% |
| 3 | 1,500.6 | 3.95% |
| 4 | 947.0 | 9.03% |
| 5 | 865.3 | 3.29% |

### 關鍵發現：裝置熱降頻是主要干擾變因

兩組數字都出現明顯的「低段（~850–1050ms）／高段（~1480–1830ms）」雙峰分佈，且高段大致對應連續跑多輪後裝置溫度上升、觸發 CPU/GPU 熱降頻的區間，並非隨機雜訊或程式碼差異。若直接比較「不分桶」的整體中位數（Eager 1,504.4ms vs Lazy 947.0ms），會因為兩組樣本落在「熱／冷」區間的比例不同（Eager 25 個樣本中 14 個落在熱段，Lazy 僅 7 個），而得出「延遲載入大幅領先」的錯誤結論。

依裝置熱狀態分桶後（以 1,100ms 為分界，對應觀察到的跳點）：

| 熱狀態 | Eager median | Lazy median | 差異 |
|---|---|---|---|
| 冷（<1,100ms，n=11／18） | 903.1ms | 893.6ms | ~1.1% |
| 熱（≥1,100ms，n=14／7） | 1,534.5ms | 1,558.6ms | ~1.6% |

同一熱狀態下，兩版本差異落在 2% 以內，遠小於量測本身的變異係數（部分輪次 CV 高達 20%~34%）。統計上無法判定 Koin 延遲載入對 `timeToInitialDisplayMs` 有實質影響。

## 結論與決策

方案 A（Koin 延遲載入）對冷啟動時間**沒有量得到的改善**，此前「感覺變慢」的觀察來自裝置熱降頻造成的量測干擾，並非程式碼本身導致效能退化。

依 tasks.md Task 6.3 的既定判斷準則（效能未見改善 → 視需要回退），決定**回退方案 A**：

- `JetpackMovieApplication.kt` 恢復同步載入全部 8 個 Koin module（`uiModule`、`mainModule`、`homeModule`、`collectModule`、`historyModule`、`searchModule`、`detailModule`、`settingModule`）。
- `MainActivity.kt` 移除 `loadFeatureModuleIfNeeded` 延遲載入機制。
- 保留 `FeatureModulesResolutionTest`：此測試驗證 Koin 依賴圖完整解析，與模組載入時機無關，回退後仍具備防止 DI 綁定缺漏的價值。

方案 B（Baseline Profile，Task 7）不受本次結論影響，仍按原計畫進行，並改以 Checkpoint 0（Task 3）的原始基準（1,118.6ms）作為比較對象。

## Trace 檔案

原始 Perfetto trace（每輪覆蓋前一輪）未逐輪保留，僅保留各輪次 `timeToInitialDisplayMs` 彙整 JSON 於本機 scratchpad，供本次診斷分析使用。
