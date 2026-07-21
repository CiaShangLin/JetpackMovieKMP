## Context

`androidApp` 剛重新引入舊版本 `MainActivity`／`MainViewModel`，三個問題讓專案目前無法建置或會在執行期 crash：

1. `MainActivity.onCreate()` 呼叫 `installSplashScreen()`，但 `androidx.core:core-splashscreen` 不在 `gradle/libs.versions.toml` 也不在 `androidApp/build.gradle.kts` 的依賴中，且 `AndroidManifest.xml` / theme 資源完全沒有 splash 相關設定。
2. `MainActivity` 內的 `SuccessScreen`／`MainScreen` 使用 `NavHostController`、`rememberNavController()`、`NavHost`（`androidx.navigation.compose` 的 API），但專案的 catalog 只有 `androidx-navigation3-runtime` / `androidx-navigation3-ui`（Navigation3），從未加入過 classic Navigation Compose 依賴，程式碼裡也沒有對應 import——現狀完全無法編譯。
3. `MainViewModel` 建構子需要 `GetConfigurationUseCase`、`UserDataRepository`，但 `MainActivity` 用 `private val viewModel: MainViewModel by viewModels()` 取得實例。`by viewModels()` 預設走 `SavedStateViewModelFactory`，只認得無參數建構子，執行期會丟出 `InstantiationException`。專案其餘模組（`shared/domain`、`shared/data` 等）皆已用 Koin 管理依賴，但目前沒有任何 Koin module 提供 `MainViewModel`，`androidApp/build.gradle.kts` 也還沒加入 `koin-compose-viewmodel`（catalog 已有 alias，只是未被引用）。

`MainNavItem` 目前所有列舉項目都被註解掉（等待 feature module），`NavHost` 內的畫面註冊也整段註解——本次變更刻意不處理這部分，只需讓骨架在「沒有任何實際頁面」的狀態下也能編譯、執行、顯示 Loading/Error/空白 Success 畫面。

## Goals / Non-Goals

**Goals:**
- `androidApp` 可重新編譯並在裝置/模擬器上啟動，不再因為缺依賴或錯誤 API 造成編譯失敗或執行期 crash。
- Splash screen 使用 Android 官方 `core-splashscreen`，並與既有 `MainUiState.Loading` 邏輯銜接（維持 `setKeepOnScreenCondition`）。
- 導覽骨架改用專案既定的 Navigation3，取代不存在的 classic Navigation Compose 依賴；`MainNavItem` 驅動的導覽列渲染邏輯與既有互動語意（selected/unselected icon、點擊切換）盡量維持不變。
- `MainViewModel` 改由 Koin 注入，遵循專案既有「各層自帶 `di/*Module.kt`，在對應入口 `loadKoinModules`」慣例（參考 `core/ui` 的 `uiModule()` 在 `JetpackMovieApplication` 載入的模式）。

**Non-Goals:**
- 不處理任何 feature module 的畫面接回（`homeScreen`／`collectScreen`／`searchScreen`／`historyScreen`／`settingsScreen`／`movieDetailScreen` 等註解區塊維持原樣，待後續 change 處理）。
- 不重新設計 `MainNavItem` 的圖示／文案／分頁結構本身，只調整它與 Navigation3 API 的整合方式。
- 不引入 feature module 專屬的 Koin module。

## Decisions

### 1. Splash Screen：採用 `androidx.core:core-splashscreen` + `Theme.SplashScreen`
- 在 `gradle/libs.versions.toml` 新增 `androidx-core-splashscreen` alias，`androidApp/build.gradle.kts` 加入 `implementation(libs.androidx.core.splashscreen)`。
- `androidApp/src/main/res/values/themes.xml` 已補上（討論過程中使用者已先行加入）：
  ```xml
  <style name="Theme.JetpackMovieCompose" parent="Theme.Material3.Light.NoActionBar">
      <item name="android:windowLightStatusBar">true</item>
  </style>

  <style name="Theme.App.Starting" parent="Theme.SplashScreen">
      <item name="windowSplashScreenBackground">@color/white</item>
      <item name="windowSplashScreenAnimatedIcon">@mipmap/ic_launcher_round</item>
      <item name="windowSplashScreenAnimationDuration">500</item>
      <item name="postSplashScreenTheme">@style/Theme.JetpackMovieCompose</item>
  </style>
  ```
  但目前仍有兩個缺口會讓建置失敗或 splash 設定不生效，需一併修復：
  1. **`@color/white` 未定義**：`res/` 底下沒有 `colors.xml`，`windowSplashScreenBackground="@color/white"` 會造成 resource linking 錯誤 → 需新增 `androidApp/src/main/res/values/colors.xml` 定義 `white`（`#FFFFFFFF`）。
  2. **`AndroidManifest.xml` 的 `<activity>` 尚未套用該 theme**：目前 `<activity android:name=".ui.MainActivity">` 沒有 `android:theme` 屬性，導致 `Theme.App.Starting` 定義了也不會實際套用到 `MainActivity` → 需在該 `<activity>` 加上 `android:theme="@style/Theme.App.Starting"`（`<application>` 維持現有 `@android:style/Theme.Material.Light.NoActionBar`，本次不動；`postSplashScreenTheme` 會在 splash 結束後由 compat library 自動切換為 `Theme.JetpackMovieCompose`，不需額外處理 `<application>` 層級）。
- launcher icon（`mipmap-*`／`drawable(-v24)/ic_launcher_*`）使用者已換上 Android Studio 標準範本的 adaptive icon，自包含無額外 color 依賴，本次不需調整。
- `MainActivity.onCreate()` 現有的 `installSplashScreen()` + `setKeepOnScreenCondition { configuration.value is MainUiState.Loading }` 邏輯不變，只補齊依賴、`colors.xml` 與 manifest theme 屬性。
- **替代方案考慮**：手刻一個 Compose splash 畫面（不依賴 `core-splashscreen`）。放棄原因：偏離 Android 官方建議做法，且與系統冷啟動 splash 銜接較差，會有雙重 splash 閃爍問題。

### 2. Navigation：改用 Navigation3（`NavBackStack` + `NavDisplay` + entry provider）
- 移除 `MainActivity` 中對 `NavHostController`／`rememberNavController()`／`NavHost`／`currentBackStackEntryAsState()`／`popUpTo` 等 classic Navigation Compose API 的使用（這些 API 目前也無從 import，因為對應依賴不存在）。
- 改用 `androidx.navigation3:navigation3-runtime` 的 `rememberNavBackStack()` 建立 `NavBackStack`，並用 `androidx.navigation3:navigation3-ui` 的 `NavDisplay` 取代 `NavHost`；點擊導覽列項目時直接操作 backstack（例如 `backStack.add(key)` / 對齊既有 key 時 `backStack.removeLastOrNull()` 再 `add`），取代 `navController.navigate(route) { popUpTo... }` 的字串路由語意。
- `MainNavItem` 目前 `route: String` 欄位改為每個分頁對應一個實作 Navigation3 `NavKey` 的物件（沿用現有 icon/文案 metadata 結構），讓 backstack 元素型別安全；由於所有分頁目前都被註解掉，暫時只保留列舉殼與型別定義，實際 `NavKey` 物件在 feature module 導入時逐一補上。
- 由於目前沒有任何可用的畫面，`NavDisplay` 的 `entryProvider` 暫時只註冊一個佔位用的空白 Composable（例如沿用既有 `LoadingScreen` 或新增一個極簡 placeholder），作為 `rememberNavBackStack()` 的 start key，直到 feature module 導入前都維持這個最小可執行狀態；行為明確標註為暫時性，不視為正式 UX。
- **替代方案考慮**：先加回 classic `androidx.navigation:navigation-compose` 依賴讓現有程式碼直接編譯過。放棄原因：與專案既定技術選型（Navigation3，見 `openspec/specs` 及 `libs.versions.toml` 既有 alias）衝突，等於引入一個之後還要再拔掉的依賴，屬於重複工。

### 3. MainViewModel 注入：新增 androidApp 層 Koin module，改用 `koinViewModel()`
- 依循既有模式（`core/ui` 有 `uiModule()`，在 `JetpackMovieApplication.onCreate()` 用 `loadKoinModules(uiModule())` 載入），在 `androidApp` 新增對應 `di/MainModule.kt`：
  ```kotlin
  fun mainModule() = module {
      viewModel { MainViewModel(getConfigurationUseCase = get(), userDataRepository = get()) }
  }
  ```
- `JetpackMovieApplication.onCreate()` 改為 `loadKoinModules(uiModule(), mainModule())`。
- `androidApp/build.gradle.kts` 新增 `implementation(libs.koin.compose.viewmodel)`（catalog 已有 alias，僅未被引用）。
- `MainActivity` 內 `private val viewModel: MainViewModel by viewModels()` 移除，改在 `setContent` 內用 `koinViewModel<MainViewModel>()` 取得實例（Compose 進入點注入，符合 Koin Compose 慣例，也避免 `ComponentActivity` 層需要额外 Factory 設定）。
- **替代方案考慮**：改用 `koin-android` 的 `by viewModel()`（Activity 層級擴充函式）取代 `by viewModels()`。兩者皆可行；選擇 `koinViewModel()` 是因為它是 Compose-first API，與專案其餘 Compose 化程度一致，且 `androidApp/build.gradle.kts` 已預期會用到 `koin-compose`／`koin-compose-viewmodel`（目前只有 `koin-compose` 被引用但未使用）。

## Risks / Trade-offs

- **[Risk]** Navigation3（`1.1.4`）目前是相對年輕的函式庫，`NavKey`／`NavDisplay`／entry provider 的確切 API 簽章需要在實作時對照官方文件確認，設計文件中的呼叫方式可能與實際 API 有出入。
  → **Mitigation**：實作 tasks 中安排一個「查閱 Navigation3 官方文件核對 API」步驟（可用 context7 MCP 查詢），確認後再動手改寫，避免憑記憶寫出不存在的 API。
- **[Risk]** `MainNavItem` 目前所有項目皆被註解，`NavDisplay` 暫時只有佔位畫面，觀感上「導覽列是空的」，可能讓人誤以為導覽功能沒做完。
  → **Mitigation**：在程式碼與 tasks 中明確註記這是刻意的暫時狀態（feature module 導入前的骨架），並在 proposal Impact 段落已說明範圍排除。
- **[Risk]** Splash theme 新增後，若 `AndroidManifest.xml` 的 `<application android:theme>` 與新的 `<activity android:theme>` 交互設定不當，可能造成 Android 12 以下裝置沒有 splash、或 Android 12+ 出現「兩層 splash」的觀感問題。
  → **Mitigation**：依 Android 官方 `SplashScreen` migration 指南設定（`postSplashScreenTheme` 或維持單一 activity theme 切換），並在實體裝置/模擬器上手動驗證冷啟動畫面。
- **[Trade-off]** `MainViewModel` 注入方式與導覽骨架的改動都不影響對外可觀察行為（使用者看到的仍是 Loading/Error/空白 Success），純粹是內部實作對齊架構，符合 proposal 中標記的 BREAKING（內部契約，非使用者行為）說明。

## Migration Plan

1. 依序修正三個問題（各自獨立，可分開驗證）：先補 splash screen 依賴與 theme → 再改寫導覽骨架 → 最後接上 Koin 注入。
2. 每個步驟完成後執行 `./gradlew :androidApp:assembleDebug` 確認可編譯，並手動在模擬器啟動確認冷啟動 splash、畫面渲染、無 crash。
3. 無資料庫 schema 變更，不涉及 Room migration；無需 rollback 策略之外的特殊處理——若某一步驟出狀況，可個別 revert 對應檔案而不影響其他兩項修正。

## Open Questions

- Navigation3 `1.1.4` 的 `NavKey`／`NavDisplay`／`entryProvider` 確切 API 簽章（是否需要額外的 `SceneStrategy`、`rememberNavBackStack` 的參數形式等）留待實作時查證官方文件後定案。
- Splash icon／品牌顏色等視覺細節（`windowSplashScreenAnimatedIcon` 等）本次不特別設計，先用預設或沿用既有 app icon，待有明確設計稿再調整。
