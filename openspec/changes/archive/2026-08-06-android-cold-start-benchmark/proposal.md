## Why

`androidApp` 目前沒有任何自動化的啟動效能量測機制：`JetpackMovieApplication.onCreate()` 一次性同步載入全部 8 個 Koin feature module（`uiModule`、`mainModule`、`homeModule`、`collectModule`、`historyModule`、`searchModule`、`detailModule`、`settingModule`），也沒有 Baseline Profile。冷啟動優化若沒有先建立可重複執行的量測環境，就無法客觀判斷「優化前後」的差異，也無法在未來的變更中防止效能回退。因此需要先建立 Macrobenchmark 測試環境並記錄基準數據，再進行冷啟動優化，最後用同一套測試驗證改善幅度。

## What Changes

- 新增獨立的 `:benchmark` Gradle module，使用 AGP `com.android.test` plugin 與 Jetpack Macrobenchmark library，撰寫 `StartupBenchmark` 量測 `androidApp` 冷啟動（`StartupMode.COLD`）耗時。
- 在正式進行任何啟動路徑程式碼變更前，先執行一次 `:benchmark` 測試並記錄「優化前」基準報告（存放於本 change 目錄下，例如 `baseline-report.md`），作為後續比較基準。
- 針對冷啟動路徑進行優化，候選手段（實際取捨列於 design.md）：
  - 導入 Baseline Profile（`androidx.profileinstaller` + Baseline Profile Gradle Plugin），由 `:benchmark` module 產生並整合進 `androidApp`。
  - 調整 `JetpackMovieApplication.onCreate()` 的 Koin module 載入策略（例如依畫面延遲載入非首頁必要的 feature module）。
  - 視需要調整 Splash 顯示邏輯與其他啟動期同步工作。
- 優化完成後，重新執行相同的 `:benchmark` 測試，產出「優化後」報告，並在 change 文件中列出優化前後的量化比較（例如 P50/P90 冷啟動時間）。
- **BREAKING**：無。本次不變更任何對外可見的功能行為或 public API，僅新增量測基礎設施與調整啟動期初始化順序。

## Capabilities

### New Capabilities
- `android-startup-benchmark`：定義 `:benchmark` module 的 Macrobenchmark 測試環境、冷啟動量測方式，以及優化前後基準報告的驗收標準。

### Modified Capabilities
（無現有 capability 的對外行為需求變更；`android-app-entry` 的既有 Requirement 不受影響，`JetpackMovieApplication.onCreate()` 的內部初始化順序調整屬於實作細節而非 spec 行為變更。）

## Impact

- 新增 `:benchmark`（Android module，`com.android.test` plugin，僅限 Android 平台，`androidTest` source set）：新增 `settings.gradle.kts` include、`benchmark/build.gradle.kts`。
- `androidApp`（Android 平台）：
  - `build.gradle.kts` 可能新增 `androidx.profileinstaller` 依賴與 Baseline Profile consumer plugin。
  - `JetpackMovieApplication.kt` 的 Koin module 載入邏輯可能調整（延遲載入策略）。
  - 新增 `benchmark-type` build type 或等效設定，供 Macrobenchmark 測試使用（依 Macrobenchmark 官方建議）。
- `gradle/libs.versions.toml`：新增 Macrobenchmark 相關 version/library/plugin alias（例如 `androidx-benchmark-macro-junit4`、`androidx-profileinstaller`、Baseline Profile Gradle Plugin），並列出使用該 alias 的模組（`:benchmark`、`androidApp`）。
- 不影響 `shared/*`、`core/*`、`feature/*` 的對外行為與 public API；`shared/app` 的 `InitKoin.kt`／`InitKoinAndroid.kt` 本次不調整（僅 `androidApp` 呼叫端的載入時機可能改變）。
- 不影響 iOS 平台（`iosApp`）。
