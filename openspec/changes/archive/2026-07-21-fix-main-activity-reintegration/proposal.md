## Why

`androidApp` 剛重新引入舊版本的 `MainActivity`／`MainViewModel` 程式碼，但尚未對齊目前架構（Navigation3、Koin DI），導致專案目前無法建置／執行：`installSplashScreen()` 缺少對應依賴、`MainActivity` 仍使用舊版 `androidx.navigation`（`NavHostController`／`NavHost`）API 但完全沒有對應 import 也未加入依賴、`MainViewModel` 帶有建構子參數卻用 `by viewModels()` 建立（沒有可用的 Factory，執行期會直接 crash）。需要先把這三個問題修好，讓 app 能回到可建置、可執行的狀態，之後才能繼續導入 feature module。

## What Changes

- 新增 `androidx.core:core-splashscreen` 依賴到 `gradle/libs.versions.toml` 與 `androidApp/build.gradle.kts`，並補上 `Theme.SplashScreen` 相關的 `themes.xml` 設定與 `AndroidManifest.xml` theme 屬性，讓 `installSplashScreen()` 可正確編譯與運作。
- 將 `MainActivity` 內的導覽程式碼從舊版 `androidx.navigation`（`NavHostController`／`rememberNavController`／`NavHost`）改寫為專案既有的 Navigation3（`androidx.navigation3:navigation3-runtime`／`navigation3-ui`）API（`NavBackStack`／`rememberNavBackStack`／`NavDisplay`／entry provider），維持 `MainNavItem` 驅動的導覽列行為。
- 新增 androidApp 層的 Koin module（提供 `MainViewModel`），並在既有 Koin 初始化流程（`JetpackMovieApplication` / `InitKoinAndroid`）中載入；`androidApp/build.gradle.kts` 新增 `koin-compose-viewmodel` 依賴，`MainActivity` 改用 Koin 的 `koinViewModel()` 取得 `MainViewModel`，取代目前會 crash 的 `by viewModels()`。
- **BREAKING**：`MainActivity` 取得 `MainViewModel` 的方式（`by viewModels()` → Koin 注入）與導覽實作（classic Navigation Compose → Navigation3）皆屬不相容的內部改寫，但不影響對外行為（UI／導覽流程維持一致）。

## Capabilities

### New Capabilities
- `android-app-entry`：定義 `androidApp` 模組的入口點（`MainActivity`、`MainViewModel`）如何啟動 splash screen、如何用 Navigation3 建立主要導覽骨架、以及如何透過 Koin 取得 ViewModel，作為 feature module 尚未導入前的最小可執行骨架。

### Modified Capabilities
（無：既有 specs 未涵盖 `androidApp` 入口點行為，不涉及既有 capability 的需求變更）

## Impact

- **受影響模組**：`androidApp`（`MainActivity.kt`、`MainViewModel.kt`、`JetpackMovieApplication.kt`、新增的 androidApp Koin module、`AndroidManifest.xml`、`res/values/themes.xml`）、`shared/app`（視需要調整 `InitKoinAndroid.kt` 載入新 module 的位置）。
- **新增依賴**：需修改 `gradle/libs.versions.toml` 新增 `androidx-core-splashscreen` alias（catalog 目前已有 `koin-compose-viewmodel` alias 但未被 androidApp 引用，需在 `androidApp/build.gradle.kts` 加上 `implementation(libs.koin.compose.viewmodel)` 與 `implementation(libs.androidx.core.splashscreen)`）。
- **不影響**：feature module 導入（`MainNavItem` 目前所有項目皆註解、`NavDisplay` 暫時沒有實際畫面內容）刻意排除在本次範圍外，後續 feature module 導入時再串接。
