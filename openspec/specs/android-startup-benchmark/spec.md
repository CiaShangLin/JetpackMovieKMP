# android-startup-benchmark Specification

## Purpose
定義 `androidApp` 冷啟動效能量測基礎設施的驗收標準：獨立的 `:benchmark` Macrobenchmark 測試模組、供量測使用的 `benchmark` build type、以三個檢查點（優化前／Koin 延遲載入／疊加 Baseline Profile）量化歸因冷啟動優化手段的流程，以及確保優化過程不破壞既有可觀察功能行為。

## Requirements

### Requirement: 專案 MUST 具備獨立的 Macrobenchmark 測試模組

專案 MUST 提供一個獨立的 `:benchmark` Gradle module（使用 `com.android.test` plugin，`targetProjectPath` 指向 `:androidApp`），其中 MUST 包含至少一個以 Jetpack Macrobenchmark library（`androidx.benchmark:benchmark-macro-junit4`）撰寫、量測 `androidApp` 冷啟動（`StartupMode.COLD`）耗時的測試類別。

#### Scenario: `:benchmark` module 可獨立編譯

- **WHEN** 執行 `./gradlew :benchmark:assembleBenchmark`（或對應 build type 的 assemble task）
- **THEN** 建置 MUST 成功完成，不得因缺少依賴或設定錯誤而中止

#### Scenario: 冷啟動測試可在裝置或模擬器上執行並產出報告

- **WHEN** 在已連接的實體裝置或模擬器上執行 `./gradlew :benchmark:connectedCheck`
- **THEN** 測試 MUST 成功啟動 `androidApp` 至少一次完整冷啟動流程
- **AND** MUST 產出包含各次迭代耗時（可推導出 P50/P90）的量測報告

### Requirement: `androidApp` MUST 提供供效能量測使用的 `benchmark` build type

`androidApp` 的 `build.gradle.kts` MUST 定義一個繼承自 `release` 的 `benchmark` build type，MUST 設定 `debuggable = false` 且 MUST 啟用 `profileable`，以符合 Macrobenchmark 官方建議的量測前提條件，避免直接對 `debug`（含偵錯開銷）或未開啟 profileable 的 `release` build 進行量測而導致數據失真。

#### Scenario: `benchmark` build type 存在且設定正確

- **WHEN** 檢查 `androidApp/build.gradle.kts` 的 `buildTypes` 設定
- **THEN** MUST 存在名為 `benchmark` 的 build type
- **AND** 該 build type MUST 設定 `debuggable = false`
- **AND** 該 build type MUST 啟用 `profileable`（例如 `isProfileable = true` 或等效設定）

### Requirement: 冷啟動優化 MUST 以三個檢查點的量化報告分別歸因兩種優化手段

本次冷啟動優化變更 MUST 在任何啟動路徑程式碼調整之前，先以 `:benchmark` module 的測試產出 Checkpoint 0（優化前）基準報告；MUST 在只完成 Koin module 延遲載入後，重新執行相同測試產出 Checkpoint 1 報告；MUST 在疊加 Baseline Profile 後，再次執行相同測試產出 Checkpoint 2 報告。三份報告 MUST 使用同一台實體裝置、記錄裝置資訊（型號、Android 版本）與 `CompilationMode` 設定，並列出量化的冷啟動時間比較（至少含 P50，樣本數足夠時應含 P90），使優化前後的差異可分別歸因於各優化手段。

#### Scenario: 基準報告先於優化程式碼變更產出

- **WHEN** 檢視本次 change 的 commit 歷史或報告檔案時間軸
- **THEN** Checkpoint 0 基準報告 MUST 在任何調整 `JetpackMovieApplication.onCreate()` 啟動路徑或新增 Baseline Profile 的程式碼變更之前完成

#### Scenario: Checkpoint 1 只反映 Koin 延遲載入的貢獻

- **WHEN** 只完成 Koin module 延遲載入（尚未加入 Baseline Profile）後重新執行 `:benchmark` 測試
- **THEN** 產出的 Checkpoint 1 報告 MUST 使用與 Checkpoint 0 相同的裝置與 `CompilationMode` 設定
- **AND** MUST 能明確列出 Checkpoint 0 與 Checkpoint 1 之間的冷啟動時間差異（絕對值與百分比）

#### Scenario: Checkpoint 2 反映疊加 Baseline Profile 後的額外貢獻

- **WHEN** 在 Koin 延遲載入的基礎上完成 Baseline Profile 後重新執行 `:benchmark` 測試
- **THEN** 產出的 Checkpoint 2 報告 MUST 使用與 Checkpoint 0／1 相同的裝置與 `CompilationMode` 設定
- **AND** MUST 能明確列出 Checkpoint 1 與 Checkpoint 2 之間的冷啟動時間差異（絕對值與百分比），與 Checkpoint 0 到 Checkpoint 2 的總改善幅度

### Requirement: 冷啟動優化 MUST NOT 改變既有可觀察功能行為

針對 `JetpackMovieApplication.onCreate()` 啟動路徑（例如 Koin module 載入時機）所做的任何調整，MUST NOT 改變使用者可觀察到的既有功能行為：底部導覽列各分頁（首頁、收藏、歷史、搜尋、設定）MUST 仍可正常進入並正確渲染內容，Splash 畫面收起時機 MUST 維持 `android-app-entry` capability 既有 Requirement 定義的行為。

#### Scenario: 延遲載入 Koin module 後各分頁仍可正常運作

- **WHEN** 使用者在完成冷啟動優化後的 App 中依序點擊底部導覽列的收藏、歷史、搜尋、設定分頁
- **THEN** 各分頁 MUST 成功渲染對應畫面，MUST NOT 因對應 Koin module 尚未載入而發生 `NoBeanDefFoundException` 或等效的注入失敗例外

#### Scenario: Splash 收起時機不受影響

- **WHEN** 完成冷啟動優化後啟動 App
- **THEN** Splash 畫面 MUST 仍在 `MainViewModel.configuration` 進入 `Success`／`Error` 狀態時收起，行為與 `android-app-entry` capability 既有 Requirement 一致
