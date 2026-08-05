## 1. 依賴版本目錄與模組註冊

- [x] 1.1 於 `gradle/libs.versions.toml` 新增 Macrobenchmark 相關 version/library/plugin alias（`androidx-benchmark-macro-junit4`、`androidx-uiautomator`、`android-test` plugin 已加入；`androidx-profileinstaller`、`androidx-baselineprofile` plugin 留待 Task 7 方案 B 時再加）
- [x] 1.2 於 `androidApp/build.gradle.kts` 新增 `testImplementation(libs.koin.test)`（`koin-test` alias 已存在於版本目錄）
- [x] 1.3 於 `settings.gradle.kts` 新增 `include(":benchmark")`

## 2. `:benchmark` module 建立

- [x] 2.1 建立 `benchmark/build.gradle.kts`，套用 `com.android.test` plugin，設定 `targetProjectPath = ":androidApp"` 與 `experimentalProperties["android.experimental.self-instrumenting"] = true`
- [x] 2.2 新增 `benchmark/src/main/AndroidManifest.xml`（`com.android.test` module 所需的最小 manifest）
- [x] 2.3 撰寫 `StartupBenchmark.kt`（`androidx.benchmark.macro.junit4.MacrobenchmarkRule`），量測 `androidApp` 冷啟動（`StartupMode.COLD`）
- [x] 2.4 於 `androidApp/build.gradle.kts` 新增 `benchmark` build type（繼承 `release`、`debuggable = false`、啟用 `profileable`）
- [x] 2.5 執行 `./gradlew :benchmark:assembleBenchmark` 確認 `:benchmark` module 可成功編譯

## 3. Smoke test 與「優化前」基準量測（Checkpoint 0）

- [ ] 3.1 選定一台實體 Android 裝置，在其上執行 `./gradlew :benchmark:connectedCheck`，確認 `StartupBenchmark` 可成功執行並產出報告（不修改任何啟動路徑程式碼）
- [ ] 3.2 記錄量測裝置資訊（型號、Android 版本）與 `CompilationMode` 設定，後續 Checkpoint 1／2 MUST 沿用同一台裝置與相同設定
- [ ] 3.3 將 Checkpoint 0 冷啟動量測結果（P50，樣本足夠時含 P90）整理進 `openspec/changes/android-cold-start-benchmark/baseline-report.md`

## 4. 實作方案 A：Koin module 延遲載入

- [ ] 4.1 於 `MainActivity.kt` 的 `mainEntry()` 中，為 `collectModule`、`historyModule`、`searchModule`、`detailModule`、`settingModule` 各自維護「已載入」旗標，在建立對應 `NavEntry` 之前同步呼叫 `loadKoinModules(listOf(xxxModule()))` 並標記為已載入，避免重複載入
- [ ] 4.2 調整 `JetpackMovieApplication.onCreate()`：只同步載入首屏必要的 Koin module（`uiModule`、`mainModule`、`homeModule`），其餘改由 4.1 的機制延遲載入
- [ ] 4.3 新增 `androidApp/src/test` 下的 Koin `checkModules()` 單元測試，驗證全部 8 個 module 加總後依賴圖可完整解析
- [ ] 4.4 執行 `./gradlew :androidApp:assembleDebug` 確認建置成功

## 5. 功能回歸驗證（方案 A，避免延遲載入破壞既有行為）

- [ ] 5.1 手動驗證：依序點擊底部導覽列收藏、歷史、搜尋、設定分頁，確認各分頁正常渲染，且不出現 Koin 注入失敗（例如 `NoBeanDefFoundException`）
- [ ] 5.2 手動驗證：Splash 畫面仍在 `MainViewModel.configuration` 進入 `Success`／`Error` 時正確收起，行為與既有 `android-app-entry` capability 一致
- [ ] 5.3 手動驗證：從各分頁進入電影詳情（`MovieDetailKey`）與返回流程正常運作

## 6. 方案 A 複測（Checkpoint 1）

- [ ] 6.1 使用與 Task 3 相同裝置與 `CompilationMode` 設定，重新執行 `./gradlew :benchmark:connectedCheck`
- [ ] 6.2 將 Checkpoint 1 冷啟動量測結果整理進 `openspec/changes/android-cold-start-benchmark/after-report-koin-lazy-load.md`，並列出與 Checkpoint 0 的量化比較（絕對值與百分比差異）
- [ ] 6.3 若延遲載入造成功能回歸或效能未見改善，於報告中記錄原因並視需要回退對應 feature module 的延遲載入

## 7. 實作方案 B：Baseline Profile

- [ ] 7.1 新增 `androidx.profileinstaller` 依賴至 `androidApp/build.gradle.kts`
- [ ] 7.2 在 `:benchmark` module 新增 `BaselineProfileGenerator`，產生 `androidApp` 的 Baseline Profile
- [ ] 7.3 執行 `./gradlew :androidApp:assembleDebug` 確認建置成功

## 8. 方案 B 複測（Checkpoint 2）與最終比較報告

- [ ] 8.1 使用與 Task 3 相同裝置與 `CompilationMode` 設定，重新執行 `./gradlew :benchmark:connectedCheck`
- [ ] 8.2 將 Checkpoint 2 冷啟動量測結果整理進 `openspec/changes/android-cold-start-benchmark/after-report-baseline-profile.md`，並彙整 Checkpoint 0／1／2 三者的量化比較
- [ ] 8.3 若 Baseline Profile 造成任一功能回歸或效能未見改善，於報告中記錄原因

## 9. 最終驗證

- [ ] 9.1 執行 `./gradlew ktlintCheck`
- [ ] 9.2 執行 `./gradlew :androidApp:assembleDebug` 確認正式 debug 建置不受影響
- [ ] 9.3 執行 `openspec validate android-cold-start-benchmark --type change --strict --no-interactive`
