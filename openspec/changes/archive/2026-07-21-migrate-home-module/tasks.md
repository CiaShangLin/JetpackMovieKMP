## 1. feature/home：Gradle 依賴補齊

- [x] 1.1 盤點 `HomeViewModel`／`HomeContentViewModel`／`HomeScreen`／`HomeNavigation` 實際用到的 API（Compose runtime/foundation/material3/ui/tooling-preview、`lifecycle-viewmodel-compose`、`paging-compose`、`navigation3-runtime`/`navigation3-ui`、`koin-compose`/`koin-compose-viewmodel`），對照 `gradle/libs.versions.toml` 既有 alias
- [x] 1.2 在 `feature/home/build.gradle.kts` 補上對應 `implementation(libs.xxx)` 宣告（比照 `core/ui/build.gradle.kts` 的既有寫法），不新增 Version Catalog 條目
- [x] 1.3 執行 `./gradlew :feature:home:build`，確認不再出現「缺少依賴」的編譯錯誤（此階段可能仍有其他程式碼問題，留待後續任務處理）

## 2. feature/home：修正 import 與 Koin 化

- [x] 2.1 修正 `HomeViewModel.kt` 的 import：`com.shang.data.repository.*` → `com.shang.jetpackmoviekmp.data.repository.*`
- [x] 2.2 修正 `HomeScreen.kt` 的 import：`com.shang.designsystem.component.*` → `com.shang.jetpackmoviekmp.core.designsystem.component.*`、`com.shang.model.MovieGenreBean` → `com.shang.jetpackmoviekmp.model.MovieGenreBean`、`com.shang.ui.*` → `com.shang.jetpackmoviekmp.core.ui.*`
- [x] 2.3 修正 `HomeUiState.kt` 的 import：`com.shang.model.MovieGenreBean` → `com.shang.jetpackmoviekmp.model.MovieGenreBean`
- [x] 2.4 確認 `HomeContentViewModel.kt` 既有的 `com.shang.jetpackmoviekmp.*` import 皆對應實際存在的類別（`MovieCardData`、`asMovieCardResult`、`MovieRepository`、`GetHomeMovieListUseCase`、`MovieGenreBean`）
- [x] 2.5 移除 `HomeContentViewModel.kt` 的 `@HiltViewModel`／`@AssistedInject`／`@Assisted`／`@AssistedFactory` 註解與其 `Factory` interface，改為一般建構子（`movieGenre` 改為一般建構子參數）
- [x] 2.6 新增 `feature/home` 的 `di/HomeModule.kt`（原路徑 `com/shang/home/di/`，見任務 6 已搬遷至 `com/shang/jetpackmoviekmp/feature/home/di/`）：`homeModule()` 內以 `viewModel { }` 綁定 `HomeViewModel`，以參數化 `viewModel { (genre: MovieGenreBean.MovieGenre) -> }` 綁定 `HomeContentViewModel`
- [x] 2.7 修正 `HomeScreen.kt` 取得 ViewModel 的方式：移除 `androidx.hilt.navigation.compose.hiltViewModel` import，改用 `org.koin.compose.viewmodel.koinViewModel()`；`HomeContentViewModel` 呼叫改為帶 `key` 與 `parameters = { parametersOf(genre) }` 的 `koinViewModel()`
- [x] 2.8 執行 `./gradlew :feature:home:build`，確認編譯通過且不再殘留 Hilt 相關 import

## 3. feature/home：Navigation3 遷移

- [x] 3.1 改寫 `HomeNavigation.kt`：移除 `androidx.navigation.NavController`／`NavGraphBuilder`／`NavOptions`／`androidx.navigation.compose.composable` 與字串路由 `HOME_ROUTE`
- [x] 3.2 新增 `@Serializable data object HomeKey : NavKey`（`androidx.navigation3.runtime.NavKey`），比照 `MainActivity.kt` 的 `PlaceholderKey` 慣例
- [x] 3.3 提供產生 `NavEntry` 的方式（例如 `fun homeEntry(onMovieClick: (Int) -> Unit): Pair<NavKey, NavEntry<NavKey>>`），供 `androidApp` 的 `entryProvider` 使用
- [x] 3.4 執行 `./gradlew :feature:home:build`，確認 Navigation3 改寫後仍可編譯

## 4. androidApp：接上 feature:home 與導覽設定

- [x] 4.1 `androidApp/build.gradle.kts` 新增 `implementation(projects.feature.home)`
- [x] 4.2 `MainNavItem.kt` 取消註解並補上 `HOME` 項目，`key` 指向 `feature/home` 的 `HomeKey`
- [x] 4.3 `MainActivity.kt` 的 `NavDisplay` entryProvider 改為依 `navKey` 分派：`HomeKey` 分派到 `feature/home` 的 `HomeScreen`（暫以 no-op 或 log callback 作為 `onMovieClick`），其餘情況維持回退到 `PlaceholderScreen`
- [x] 4.4 `JetpackMovieApplication.onCreate()` 的 `loadKoinModules(...)` 加入 `feature/home` 的 `homeModule()`
- [x] 4.5 執行 `./gradlew :androidApp:assembleDebug`，確認整合後可成功建置

## 5. 驗證與測試

- [x] 5.1 為 `HomeViewModel` 補上單元測試（`feature/home/src/test`）：比照 `androidApp` `MainViewModelTest` 既有慣例，以 Fake 測試替身（`FakeMovieRepository`／`FakeUserDataRepository`，而非 MockK——本專案 `shared/*`／`androidApp` 皆用 Fake 替身，`feature/home` 沿用同一慣例）模擬 `MovieRepository`／`UserDataRepository`，涵蓋 `movieGenres` 的 Success/Error 狀態與 `retry()` 行為，採 AAA 模式
- [x] 5.2 為 `HomeContentViewModel` 補上單元測試：以同一組 Fake 替身模擬 `MovieRepository`（`GetHomeMovieListUseCase` 直接用真實實例＋Fake repository），涵蓋 `movieList` 資料流（collectible smoke test，比照 `GetHomeMovieListUseCaseTest` 慣例，不斷言 `PagingData` 內容）與 `toggleMovieCollectStatus()`（收藏/取消收藏兩種分支，以 `CountDownLatch` 確定性等待 `Dispatchers.IO` 上的呼叫完成）
- [x] 5.3 執行 `./gradlew :feature:home:testDebugUnitTest`，確認新增測試通過（7/7 全數通過）
- [x] 5.4 執行 `./gradlew ktlintCheck`，確認格式符合規範
- [x] 5.5 執行 `./gradlew :androidApp:assembleDebug :feature:home:build`，兩者皆 BUILD SUCCESSFUL（`:feature:home:build` 已包含其 `check`）。根 `check` 額外會觸發 `shared:domain:compileAndroidHostTest`，該任務因 `TestDatabaseBuilder`／`InMemoryPreferencesDataStore` 缺少 Room／DataStore 測試依賴而編譯失敗——與本次 `feature/home` 遷移無關（`shared/domain` 未被本次變更觸碰），視為既有問題，不阻擋本 change 完成

## 6. feature/home：套件命名統一（`com.shang.home` → `com.shang.jetpackmoviekmp.feature.home`）

- [x] 6.1 將 `feature/home/src/main/java/com/shang/home/**`（`ui`／`navigation`／`di`）搬遷至 `feature/home/src/main/java/com/shang/jetpackmoviekmp/feature/home/**`，並更新各檔案 `package` 宣告與模組內 cross-file import（`HomeModule.kt`、`HomeNavigation.kt`）
- [x] 6.2 同步搬遷 `feature/home/src/test/java/com/shang/home/**`、`feature/home/src/androidTest/java/com/shang/home/**`，更新 `package` 宣告；`ExampleInstrumentedTest.kt` 內硬編碼的 `appContext.packageName` 斷言字串一併修正為 `com.shang.jetpackmoviekmp.feature.home.test`
- [x] 6.3 更新 `androidApp` 對 `feature/home` 的 import：`MainActivity.kt`（`HomeKey`／`homeEntry`）、`JetpackMovieApplication.kt`（`homeModule`）、`MainNavItem.kt`（`HomeKey`），並修正 import 排序
- [x] 6.4 執行 `./gradlew ktlintFormat ktlintCheck`，確認格式與 import 排序符合規範
- [x] 6.5 執行 `./gradlew :feature:home:build :androidApp:assembleDebug`，確認搬遷後兩者皆 BUILD SUCCESSFUL 且既有 7 項單元測試全數通過
