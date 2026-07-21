## 1. 依賴 Catalog（gradle/libs.versions.toml）

- [x] 1.1 新增 `androidx-core-splashscreen` 版本與 alias（`androidx.core:core-splashscreen`）
- [x] 1.2 確認既有 `koin-compose-viewmodel` alias 版本與 `koin` 其他 alias 一致，無需新增（僅需在 `androidApp/build.gradle.kts` 引用）
- [x] 1.3（新增）為 Navigation3 `rememberNavBackStack` 需要的 `@Serializable` 支援，新增 `kotlinx-serialization-core` alias，並在 `androidApp/build.gradle.kts` 套用既有 `kotlin-serialization` plugin alias

## 2. Splash Screen（androidApp）

- [x] 2.1 `androidApp/build.gradle.kts` 新增 `implementation(libs.androidx.core.splashscreen)`
- [x] 2.2 ~~新增 `androidApp/src/main/res/values/themes.xml`，定義繼承 `Theme.SplashScreen` 的 `Theme.App.Starting`~~（已由使用者先行加入，含 `postSplashScreenTheme` 指向 `Theme.JetpackMovieCompose`）
- [x] 2.3 新增 `androidApp/src/main/res/values/colors.xml`，定義 `Theme.App.Starting` 引用的 `white`（修復目前 `@color/white` 找不到 resource 的建置錯誤）
- [x] 2.4 `AndroidManifest.xml` 的 `MainActivity` `<activity>` 元素加上 `android:theme="@style/Theme.App.Starting"`（目前完全沒有 `android:theme` 屬性，導致 themes.xml 定義了也不會生效）
- [x] 2.5 確認 `MainActivity.onCreate()` 的 `installSplashScreen()` 與 `setKeepOnScreenCondition { configuration.value is MainUiState.Loading }` 可正確編譯（依現有邏輯，不需改寫行為）
- [x] 2.5.1（新增，建置時發現）`Theme.JetpackMovieCompose` 原本 `parent="Theme.Material3.Light.NoActionBar"` 找不到對應 resource（androidApp 未依賴 Material Components View 函式庫，AAPT resource linking 失敗），改為 `parent="android:Theme.Material.Light.NoActionBar"`（與 `<application>` 既有 theme 一致的框架資源）
- [x] 2.6 手動在實機驗證冷啟動：`adb install` + 啟動後 app 穩定運行（logcat 無 FATAL/Exception），畫面正確切換為 Success 狀態的淺色主題畫面（status bar／nav bar 顏色符合 `ThemeProvider` 亮色設定），未卡在 splash
- [x] 2.7 launcher icon 資源（`mipmap-*`／`drawable(-v24)/ic_launcher_*`）使用者已換上 Android Studio 標準範本 adaptive icon；建置時發現 `mipmap-anydpi-v26/ic_launcher.xml`／`ic_launcher_round.xml` 誤引用不存在的 `@color/ic_launcher_background`（實際資源是 `drawable/ic_launcher_background.xml` 向量圖），已修正為 `@drawable/ic_launcher_background`

## 3. Navigation3 遷移（androidApp）

- [x] 3.1 查閱 Navigation3（`1.1.4`）官方文件（可用 context7 MCP），核對 `NavKey`／`rememberNavBackStack`／`NavDisplay`／entry provider 的確切 API 簽章（`NavKey`／`NavEntry`／`NavBackStack`／`rememberNavBackStack` 皆在 `androidx.navigation3.runtime`；`NavDisplay` 在 `androidx.navigation3.ui`；`rememberNavBackStack` 的 key 需實作 `NavKey` 並標註 `@Serializable`）
- [x] 3.2 移除 `MainActivity.kt` 中對 `NavHostController`／`rememberNavController()`／`NavHost`／`currentBackStackEntryAsState()`／`popUpTo` 等 classic Navigation Compose API 的使用
- [x] 3.3 調整 `MainNavItem`：將 `route: String` 改為對應 Navigation3 `NavKey` 的型別定義（保留 icon／文案 metadata，列舉項目暫時維持全部註解狀態）
- [x] 3.4 在 `MainActivity`／`SuccessScreen` 內以 `rememberNavBackStack()` 建立 backstack，並用 `NavDisplay` 取代 `NavHost` 渲染
- [x] 3.5 導覽列點擊事件改為操作 backstack（新增／回退），取代原本 `navController.navigate(route) { popUpTo... }` 邏輯
- [x] 3.6 `entryProvider` 暫時只註冊一個佔位 Composable 作為 start key，並於程式碼註解標明為 feature module 導入前的暫時狀態
- [x] 3.7 確認 `androidApp/build.gradle.kts` 未殘留／未新增任何 classic `androidx.navigation:navigation-compose` 依賴

## 4. MainViewModel 改用 Koin 注入（androidApp）

- [x] 4.1 `androidApp/build.gradle.kts` 新增 `implementation(libs.koin.compose.viewmodel)`
- [x] 4.2 新增 `androidApp/src/main/kotlin/.../di/MainModule.kt`，定義 `mainModule()` 提供 `MainViewModel(getConfigurationUseCase = get(), userDataRepository = get())`
- [x] 4.3 `JetpackMovieApplication.onCreate()` 改為 `loadKoinModules(uiModule(), mainModule())`
- [x] 4.4 `MainActivity.kt` 移除 `private val viewModel: MainViewModel by viewModels()`，改在 `setContent` 內使用 `koinViewModel<MainViewModel>()` 取得實例
- [x] 4.5 補上 `MainViewModel` 的單元測試（沿用專案既有 AAA 模式），驗證 `configuration`／`userData`／`retryConfiguration()` 行為維持正確（新增 `androidApp/src/test`，含 `MainViewModelTest` 與 `MainViewModelTestFakes`）

## 5. 驗證

- [x] 5.1 執行 `./gradlew ktlintFormat ktlintCheck` 確認格式與風格通過
- [x] 5.2 執行 `./gradlew :androidApp:assembleDebug` 確認可成功建置（過程中額外發現並修正：`Theme.JetpackMovieCompose` parent 找不到 resource、adaptive icon 誤引用 `@color/ic_launcher_background`、`loadKoinModules` 不支援 varargs、`installSplashScreen` 需要 `SplashScreen.Companion.installSplashScreen` import、Koin `viewModel` DSL 改用非 deprecated 的 `org.koin.core.module.dsl.viewModel`）；另補跑 `./gradlew :androidApp:testDebugUnitTest`，4 個測試全數通過
- [x] 5.3 在實機上完整跑一次冷啟動流程，確認 Splash → Loading/Error/Success 畫面切換皆無 crash（`adb install -r` + `am start` 驗證，process 存活、logcat 無 FATAL/Exception）
- [ ] 5.4 執行 `openspec archive fix-main-activity-reintegration`（待使用者確認實作完成後）將本次 change 歸檔並同步 `openspec/specs/android-app-entry/spec.md`
