## Context

`androidApp` 目前沒有任何啟動效能量測基礎設施。`JetpackMovieApplication.onCreate()`（`androidApp/src/main/kotlin/com/shang/jetpackmoviekmp/JetpackMovieApplication.kt`）在啟動時同步呼叫 `initKoinAndroid()` 並一次以 `loadKoinModules()` 載入全部 8 個 Koin module（`uiModule`、`mainModule`、`homeModule`、`collectModule`、`historyModule`、`searchModule`、`detailModule`、`settingModule`），而 `MainActivity` 已使用 `androidx.core:core-splashscreen` 顯示啟動畫面直到 `MainViewModel.configuration` 進入 `Success`／`Error`（見 `android-app-entry` spec）。`gradle/libs.versions.toml` 目前沒有 Macrobenchmark、Baseline Profile 或 profileinstaller 相關 alias，`settings.gradle.kts` 也沒有任何 `com.android.test` module。本次需要從零建立量測環境，再據此做冷啟動優化，因此屬於跨模組（新增 module + androidApp + 依賴版本目錄）的變更，適合先定案技術方案再實作。

## Goals / Non-Goals

**Goals:**
- 建立可重複執行、可產出量化數據（P50/P90 冷啟動時間）的 Macrobenchmark 測試環境。
- 在任何啟動路徑優化之前，先產出一份「優化前」基準報告，作為比較基準。
- 針對 `JetpackMovieApplication.onCreate()` 的啟動期初始化做出可驗證的優化，並用同一套測試量化改善幅度。
- 優化後的 App 行為（畫面、導覽、Koin 注入結果）必須與優化前一致，不改變任何對外可見功能。

**Non-Goals:**
- 不涉及 iOS 平台的啟動效能（iOS 已有獨立的 `ios-splash-screen` capability，不在本次範圍）。
- 不重構 `shared/app` 的 `InitKoin.kt`／`InitKoinAndroid.kt` 對外簽章；本次只調整 `androidApp` 呼叫端載入 module 的時機與順序。
- 不追求導入完整的 Startup Profile／R8 全模式優化等大型建置管線調整；若基準報告顯示 Koin 載入順序調整已達足夠改善，優先採用風險較低的方案。
- 不涵蓋「熱啟動」「溫啟動」（warm/hot start）量測，僅聚焦 `StartupMode.COLD`。

## Decisions

### 1. 新增獨立 `:benchmark` module，使用官方 Macrobenchmark 範本

採用 AGP 官方建議的 `com.android.test` plugin 建立獨立 `:benchmark` module（非 `androidApp` 內部的 `androidTest`），原因：
- Macrobenchmark 官方文件明確要求以獨立 test-only module 執行，避免與一般 instrumentation test 混用造成的 process/權限問題。
- 與現有 module 依賴圖（`shared/*` → `feature/*` → `androidApp`）不衝突：`:benchmark` 只依賴 `androidApp` 作為 `targetProjectPath`，不會被其他模組依賴，符合「新增模組不擴大既有依賴方向」的原則。

`:benchmark/build.gradle.kts` 需新增：
- plugin：`com.android.test`、Kotlin Android plugin、（若採用 Baseline Profile 生成）`androidx.baselineprofile`。
- 依賴：`androidx.test.ext:junit`、`androidx.test.espresso:espresso-core`、`androidx.benchmark:benchmark-macro-junit4`（透過 `gradle/libs.versions.toml` 新增對應 alias）。
- `targetProjectPath = ":androidApp"`，並設定 `experimentalProperties["android.experimental.self-instrumenting"] = true`（Macrobenchmark 官方建議設定，避免部分裝置量測失準）。

備選方案（不採用）：把 benchmark 測試直接放進 `androidApp/src/androidTest`。放棄原因：無法使用 Macrobenchmark 所需的獨立 process 隔離，且會讓 `androidApp` 的一般 instrumentation test 與效能測試混雜，不利於未來持續維護。

### 2. 量測流程採「三段式」，逐一歸因兩種優化手段

若只比較「優化前」與「兩種手段都做完後」的單一結果，會無法判斷改善幅度是 Koin 延遲載入帶來的、還是 Baseline Profile 帶來的，也不符合使用者「看得出差異」的訴求。因此量測流程改為三個檢查點（對應 tasks.md 的驗收順序，皆使用同一台實體裝置與相同 `CompilationMode`）：

1. **建立測試環境**：新增 `:benchmark` module 與 `StartupBenchmark`（`StartupMode.COLD`），確認可透過 `./gradlew :benchmark:connectedCheck` 於實體 Android 裝置上執行並產出報告。
2. **量測基準（Checkpoint 0）**：在**不修改任何啟動路徑程式碼**的前提下執行測試，結果整理進 `baseline-report.md`。
3. **只完成方案 A 後複測（Checkpoint 1）**：實作 Koin 延遲載入（不含 Baseline Profile）後執行一次測試，結果整理進 `after-report-koin-lazy-load.md`，可獨立看出方案 A 的貢獻。
4. **疊加方案 B 後複測（Checkpoint 2）**：在 Checkpoint 1 的基礎上加入 Baseline Profile 後再執行一次測試，結果整理進 `after-report-baseline-profile.md`，可獨立看出方案 B 疊加後的額外貢獻。

多跑兩輪測試的成本很低（同一套 `StartupBenchmark`，只是多次執行 `connectedCheck`），換來的是能明確回答「哪個優化手段有效、有效多少」，值得投入。

風險：Macrobenchmark 對裝置狀態（背景程式、電量模式）敏感，三個檢查點 MUST 使用同一台實體裝置、同一 `CompilationMode`、關閉不必要背景程式，此限制會寫入各報告的量測條件章節。

### 3. 冷啟動優化手段：優先延遲非首屏必要的 Koin module 載入，Baseline Profile 視基準數據決定是否導入

候選方案與取捨：

| 方案 | 說明 | 取捨 |
|---|---|---|
| **A. Koin module 延遲載入**（優先採用） | `JetpackMovieApplication.onCreate()` 只同步載入 `uiModule()`、`mainModule()`、`homeModule()`（首屏必要），其餘（`collectModule`、`historyModule`、`searchModule`、`detailModule`、`settingModule`）改為在對應分頁首次被導覽到時才載入 | 風險低、不需新增第三方依賴；但需確認 Koin 的 `get()`／`inject()` 呼叫時機不會早於對應 module 載入（例如底部導覽 icon 的 badge 邏輯若跨 feature 注入，需個別檢查） |
| **B. Baseline Profile** | 用 `:benchmark` module 的 `BaselineProfileGenerator` 產生 `androidApp` 的 baseline profile，透過 `androidx.profileinstaller` 在安裝時預先 AOT 編譯關鍵路徑 class | 對「已安裝一段時間」的裝置（ART 已完成完整編譯）效益有限，主要改善的是應用「剛安裝／更新後」的前幾次啟動；需要在 CI 或發版流程中額外執行 profile 生成步驟，增加維護成本 |
| **C. Splash 邏輯調整** | 檢視是否有可延後到 Splash 消失後才執行的初始化工作 | 影響範圍涉及 `android-app-entry` capability 既有 Splash 行為 Requirement，若調整會回頭需要對該 spec 提 MODIFIED delta；本次優先不變更，除非 baseline 數據顯示 Splash 停留時間是主要瓶頸 |

決策：**第一輪優化採方案 A**（Koin 延遲載入），因為風險最低、不引入新依賴、且直接針對已知的「一次性同步載入 8 個 module」瓶頸。方案 B（Baseline Profile）作為**同一 change 內的加分項**一併導入（`:benchmark` module 已具備產生能力，成本低），但方案 C 不在本次範圍，除非基準報告顯示有必要。兩種手段的效果分別在 Decision 2 的 Checkpoint 1／Checkpoint 2 獨立量測、不混在一起比較。

#### 3.1 方案 A 的具體實作機制

`loadKoinModules()` 是同步、非 suspend 的呼叫（單純註冊 module definitions，不涉及 I/O），因此可以直接在建立畫面內容之前呼叫，不會造成畫面延遲或競態：

- 在 `MainActivity.kt` 的 `mainEntry()`（entry provider）中，為每個延遲載入的 feature module 維護一個「已載入」旗標（例如 `mutableSetOf<KClass<*>>()` 或個別 `Boolean`，可放在 `MainActivity` 層級的 remember 或簡單的 top-level 物件），在對應 `NavKey` 分支被解析、**建立該分頁的 `NavEntry` 之前**，同步檢查旗標並視需要呼叫 `loadKoinModules(listOf(xxxModule()))`，再標記為已載入。
- 此旗標須確保「重複導覽到同一分頁」不會重複呼叫 `loadKoinModules()`（避免 Koin 對同一 module 產生 override 警告或非預期行為）。
- 因為檢查與載入都在該分頁的 Composable 開始渲染、任何 `koinViewModel()`／`inject()` 呼叫發生之前完成，DI 圖在畫面需要它時已經就緒，不會有「畫面先渲染、依賴才注入」的競態。

#### 3.2 方案 A 的自動化驗證：Koin `checkModules()`

除了 tasks.md Task 5 的手動點擊驗證外，另於 `androidApp` 新增一個純 JVM 單元測試（`androidApp/src/test`，此 source set 已存在），使用 `koin-test`（`gradle/libs.versions.toml` 已有 `koin-test` alias，僅需在 `androidApp/build.gradle.kts` 加上 `testImplementation(libs.koin.test)`）的 `checkModules()` API，一次驗證全部 8 個 Koin module（`uiModule`、`mainModule`、`homeModule`、`collectModule`、`historyModule`、`searchModule`、`detailModule`、`settingModule`）加總在一起時依賴圖可完整解析，不需要 instrumentation 或裝置即可快速抓出遺漏或錯置的 bean 定義。此測試驗證的是「延遲載入不會改變最終的依賴圖完整性」，手動點擊驗證的是「延遲載入的時機正確、UI 層不會在載入完成前嘗試取得依賴」，兩者互補。

### 3.3 方案 A 驗收結果：已測試並回退

Checkpoint 1 診斷（詳見 `after-report-koin-lazy-load.md`）發現：依裝置熱狀態分桶比較後，Koin 延遲載入與同步載入版本的 `timeToInitialDisplayMs` 差異僅 ~1–2%，落在量測本身的雜訊範圍內（部分輪次變異係數達 20%~34%），無法判定有實質改善。依 Decision 2 的三段式量測設計初衷（能歸因才值得保留），決定回退方案 A：`JetpackMovieApplication.onCreate()` 恢復同步載入全部 8 個 Koin module，`MainActivity.kt` 移除延遲載入機制；3.2 的 `checkModules()` 驗證測試（`FeatureModulesResolutionTest`）保留，因其驗證的是依賴圖完整性，與載入時機無關。方案 B（Baseline Profile）改以 Checkpoint 0 基準（`baseline-report.md`，P50 1,118.6ms）作為比較對象。

### 4. `benchmark` build type 設定

依 Macrobenchmark 官方建議，在 `androidApp/build.gradle.kts` 新增 `benchmark` build type（繼承 `release`，關閉 `debuggable`、開啟 `profileable`），避免直接對 `debug`／`release` build 量測造成失真（`debug` 有額外偵錯開銷、`release` 預設不可用 profiler attach）。此設定只影響建置設定，不影響現有 `debug`／`release` 行為與簽章設定。

## Risks / Trade-offs

- **[風險] Macrobenchmark 數據雜訊大，三個檢查點之間裝置狀態不一致會讓比較失去意義** → 三個檢查點 MUST 在同一台實體裝置上執行，並在報告中記錄裝置資訊；不承諾建立 CI 自動化門檻，僅作為人工驗收依據。
- **[風險] Koin 延遲載入可能造成「原本同步可用的依賴」在切換頁面瞬間注入失敗** → 以 Decision 3.1 的具體機制（進入分頁前同步呼叫 `loadKoinModules()`）避免時機競態；並以 Decision 3.2 的 `checkModules()` 單元測試驗證依賴圖完整性，加上 tasks.md Task 5 的手動點擊驗證時機正確性，兩者互補降低風險。
- **[風險] 新增 `:benchmark` module 若設定錯誤（例如 `targetProjectPath` 或 `testInstrumentationRunner` 設定不當）可能導致 `connectedCheck` 無法執行** → 依 AGP 官方 Macrobenchmark 範本設定，任務中包含「可成功執行一次 smoke test」的驗收步驟。
- **[Trade-off] Baseline Profile 對已完整 AOT 編譯的裝置效益有限** → 已在 Decision 2 的三段式量測中拆分：Checkpoint 1（只做方案 A）與 Checkpoint 2（疊加方案 B）分別產出報告，避免把兩者效果混為一談。

## Migration Plan

1. 新增 `:benchmark` module 與 `benchmark` build type（不影響現有建置產物）。
2. 執行 smoke test 確認 `:benchmark:connectedCheck` 可成功量測並產出報告。
3. 記錄「優化前」基準報告（Checkpoint 0：`baseline-report.md`）。
4. 實作 Koin 延遲載入（方案 A），新增 `checkModules()` 單元測試並完成手動點擊驗證。
5. 重新執行測試，記錄 Checkpoint 1 報告（`after-report-koin-lazy-load.md`）；診斷結果顯示效能差異在雜訊範圍內，回退方案 A（見 Decision 3.3）。
6. 疊加實作 Baseline Profile 生成（方案 B，基於 Checkpoint 0 基準狀態進行，因方案 A 已回退）。
7. 再次執行測試，記錄 Checkpoint 2 報告（`after-report-baseline-profile.md`），並彙整三個檢查點的量化比較。
8. 若延遲載入導致任何既有功能（分頁切換、注入時機）出現回歸，優先回退該 feature module 的延遲載入，改回同步載入，不影響其餘已驗證的模組。

不涉及正式環境資料或使用者可見版本發布，回滾方式為 `git revert` 對應的實作 commit（`:benchmark` module 可直接移除而不影響 `androidApp` 執行）。

## Open Questions（已於討論中確認）

- **量測裝置**：使用實體 Android 裝置（而非模擬器），由開發者自行選定一台實體機並記錄其型號與 Android 版本於 `baseline-report.md`／`after-report.md`；基準報告與複測報告 MUST 使用同一台裝置。
- **優化目標**：本次不設定明確的數值目標（例如特定 P50 降低百分比），驗收標準為「優化後報告相較基準報告有可量化的改善」，具體目標數值留待後續迭代視實際基準數據再訂定。
