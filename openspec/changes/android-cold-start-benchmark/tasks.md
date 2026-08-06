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

- [x] 3.1 選定一台實體 Android 裝置，在其上執行 `./gradlew :benchmark:connectedCheck`，確認 `StartupBenchmark` 可成功執行並產出報告（不修改任何啟動路徑程式碼）
- [x] 3.2 記錄量測裝置資訊（型號、Android 版本）與 `CompilationMode` 設定，後續 Checkpoint 1／2 MUST 沿用同一台裝置與相同設定
- [x] 3.3 將 Checkpoint 0 冷啟動量測結果（P50，樣本足夠時含 P90）整理進 `openspec/changes/android-cold-start-benchmark/baseline-report.md`

## 4. 實作方案 A：Koin module 延遲載入

> **狀態更新**：本方案已於 Task 6 複測後回退（效能差異在量測雜訊範圍內，未見改善），詳見 Task 6.3 與 `after-report-koin-lazy-load.md`。以下項目保留勾選作為實作歷史記錄，程式碼目前已回退為同步載入。

- [x] 4.1 於 `MainActivity.kt` 的 `mainEntry()` 中，為 `collectModule`、`historyModule`、`searchModule`、`detailModule`、`settingModule` 各自維護「已載入」旗標，在建立對應 `NavEntry` 之前同步呼叫 `loadKoinModules(listOf(xxxModule()))` 並標記為已載入，避免重複載入
- [x] 4.2 調整 `JetpackMovieApplication.onCreate()`：只同步載入首屏必要的 Koin module（`uiModule`、`mainModule`、`homeModule`），其餘改由 4.1 的機制延遲載入
- [x] 4.3 新增 `androidApp/src/test` 下的 Koin module 依賴圖解析測試（`FeatureModulesResolutionTest`），驗證 `mainModule`／`homeModule`／`collectModule`／`historyModule`／`searchModule`／`detailModule`／`settingModule` 加總後依賴圖可完整解析；`uiModule` 因需要真實 Android Context，純 JVM 測試無法涵蓋，改由 Task 5 手動驗證涵蓋
- [x] 4.4 執行 `./gradlew :androidApp:assembleDebug` 確認建置成功

## 5. 功能回歸驗證（方案 A，避免延遲載入破壞既有行為）

- [x] 5.1 手動驗證：依序點擊底部導覽列收藏、歷史、搜尋、設定分頁，確認各分頁正常渲染，且不出現 Koin 注入失敗（例如 `NoBeanDefFoundException`）
- [x] 5.2 手動驗證：Splash 畫面仍在 `MainViewModel.configuration` 進入 `Success`／`Error` 時正確收起，行為與既有 `android-app-entry` capability 一致
- [x] 5.3 手動驗證：從各分頁進入電影詳情（`MovieDetailKey`）與返回流程正常運作

## 6. 方案 A 複測（Checkpoint 1）

- [x] 6.1 使用與 Task 3 相同裝置與 `CompilationMode` 設定，重新執行 `./gradlew :benchmark:connectedCheck`（實際執行方式：人工試跑時觀察到「感覺變慢」，故先以受控 A/B 診斷測試排查——同一裝置、同一 session 內，eager 版與延遲載入版各連續跑 5 輪 `:benchmark:connectedCheck`，非嚴格複用 Checkpoint 0 的動畫關閉／USB 條件，詳見 `after-report-koin-lazy-load.md`）
- [x] 6.2 將 Checkpoint 1 冷啟動量測結果整理進 `openspec/changes/android-cold-start-benchmark/after-report-koin-lazy-load.md`（因量測條件與 Checkpoint 0 不同，未做絕對值比較，改採同一 session 內 eager vs 延遲載入版的相對比較）
- [x] 6.3 診斷結果：依裝置熱狀態分桶後，延遲載入與同步載入的效能差異僅 ~1-2%，落在量測雜訊範圍內，效能未見改善，依此決定回退方案 A——`JetpackMovieApplication.kt` 恢復同步載入全部 8 個 module，`MainActivity.kt` 移除延遲載入機制；`FeatureModulesResolutionTest`（Task 4.3）保留

## 7. 實作方案 B：Baseline Profile

> 方案 A 已回退（見 Task 6.3），本方案基於同步載入的程式碼狀態（即 Checkpoint 0 基準）進行，不疊加方案 A。

> **實作方式變更**：原計畫透過 `androidx.baselineprofile` Gradle plugin 全自動產生／整合 Baseline Profile，但該 plugin 目前最新版本（穩定版 1.4.1／測試版 1.5.0-beta01）都尚未支援 AGP 9.3.0 預設的 new DSL 架構，連續在兩個階段撞牆：(1) 套用階段直接丟 `Module :androidApp is not a supported android module.`，需以 `android.newDsl=false` 繞過；(2) 繞過後在 variant 解析階段又出現 `No matching variant of project :benchmark was found`（producer 端未能正確標記 `release` build type 的 baseline profile 輸出 variant）。因為每次繞過都需要對整個專案設定動手術（`android.newDsl` 是專案層級開關），且問題並非一次性、後續可能還有更多隱藏的不相容之處，因此改為**不套用 plugin，手動產生並複製 Baseline Profile**：`BaselineProfileGenerator` 測試本身可正常執行並產出完整、有效的 profile（已驗證，27,949 行規則），只是失去 `generateBaselineProfile` 這個一鍵整合 task，日後啟動路徑有重大變更時需手動重跑下方三個步驟更新檔案。

- [x] 7.1 新增 `androidx.profileinstaller` 依賴至 `androidApp/build.gradle.kts`；`gradle/libs.versions.toml` 新增對應 library alias（版號 1.4.1）
- [x] 7.2 在 `:benchmark` module 新增 `BaselineProfileGenerator`，執行 `./gradlew :benchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shang.benchmark.BaselineProfileGenerator` 產生 profile，並手動複製輸出檔（`benchmark/build/outputs/connected_android_test_additional_output/benchmark/connected/<裝置>/BaselineProfileGenerator_generate-baseline-prof.txt`）至 `androidApp/src/main/baseline-prof.txt`
- [x] 7.3 執行 `./gradlew :androidApp:assembleDebug` 確認建置成功（含 `ktlintCheck`、`:benchmark:assembleBenchmark`）

## 8. 方案 B 複測（Checkpoint 2）與最終比較報告

- [x] 8.1 使用與 Task 3 相同裝置與 `CompilationMode` 設定，重新執行 `./gradlew :benchmark:connectedCheck`；因 `:benchmark` module 現同時含 `StartupBenchmark`／`BaselineProfileGenerator` 兩個測試類別、執行順序未受控，另外用 `-Pandroid.testInstrumentationRunnerArguments.class=com.shang.benchmark.StartupBenchmark` 單獨重跑一次排除污染
- [ ] 8.2 將 Checkpoint 2 冷啟動量測結果整理進 `openspec/changes/android-cold-start-benchmark/after-report-baseline-profile.md`（**進行中**：已記錄兩次原始量測數據，P50 皆遠高於 Checkpoint 0 基準且不符合 Task 6 已知的熱降頻模式，判定為異常，結論待補——需先在同一 session 內做「有／無 Baseline Profile」A/B 對照排查後再回填）
- [ ] 8.3 若 Baseline Profile 造成任一功能回歸或效能未見改善，於報告中記錄原因（待 8.2 排查結果確定後處理）

## 9. 最終驗證

- [ ] 9.1 執行 `./gradlew ktlintCheck`
- [ ] 9.2 執行 `./gradlew :androidApp:assembleDebug` 確認正式 debug 建置不受影響
- [ ] 9.3 執行 `openspec validate android-cold-start-benchmark --type change --strict --no-interactive`
