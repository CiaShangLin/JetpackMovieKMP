## 1. feature/home：Gradle 依賴補齊

- [ ] 1.1 盤點 `HomeViewModel`／`HomeContentViewModel`／`HomeScreen`／`HomeNavigation` 實際用到的 API（Compose runtime/foundation/material3/ui/tooling-preview、`lifecycle-viewmodel-compose`、`paging-compose`、`navigation3-runtime`/`navigation3-ui`、`koin-compose`/`koin-compose-viewmodel`），對照 `gradle/libs.versions.toml` 既有 alias
- [ ] 1.2 在 `feature/home/build.gradle.kts` 補上對應 `implementation(libs.xxx)` 宣告（比照 `core/ui/build.gradle.kts` 的既有寫法），不新增 Version Catalog 條目
- [ ] 1.3 執行 `./gradlew :feature:home:build`，確認不再出現「缺少依賴」的編譯錯誤（此階段可能仍有其他程式碼問題，留待後續任務處理）

## 2. feature/home：修正 import 與 Koin 化

- [ ] 2.1 修正 `HomeViewModel.kt` 的 import：`com.shang.data.repository.*` → `com.shang.jetpackmoviekmp.data.repository.*`
- [ ] 2.2 修正 `HomeScreen.kt` 的 import：`com.shang.designsystem.component.*` → `com.shang.jetpackmoviekmp.core.designsystem.component.*`、`com.shang.model.MovieGenreBean` → `com.shang.jetpackmoviekmp.model.MovieGenreBean`、`com.shang.ui.*` → `com.shang.jetpackmoviekmp.core.ui.*`
- [ ] 2.3 修正 `HomeUiState.kt` 的 import：`com.shang.model.MovieGenreBean` → `com.shang.jetpackmoviekmp.model.MovieGenreBean`
- [ ] 2.4 確認 `HomeContentViewModel.kt` 既有的 `com.shang.jetpackmoviekmp.*` import 皆對應實際存在的類別（`MovieCardData`、`asMovieCardResult`、`MovieRepository`、`GetHomeMovieListUseCase`、`MovieGenreBean`）
- [ ] 2.5 移除 `HomeContentViewModel.kt` 的 `@HiltViewModel`／`@AssistedInject`／`@Assisted`／`@AssistedFactory` 註解與其 `Factory` interface，改為一般建構子（`movieGenre` 改為一般建構子參數）
- [ ] 2.6 新增 `feature/home/src/main/java/com/shang/home/di/HomeModule.kt`：`homeModule()` 內以 `viewModel { }` 綁定 `HomeViewModel`，以參數化 `viewModel { (genre: MovieGenreBean.MovieGenre) -> }` 綁定 `HomeContentViewModel`
- [ ] 2.7 修正 `HomeScreen.kt` 取得 ViewModel 的方式：移除 `androidx.hilt.navigation.compose.hiltViewModel` import，改用 `org.koin.compose.viewmodel.koinViewModel()`；`HomeContentViewModel` 呼叫改為帶 `key` 與 `parameters = { parametersOf(genre) }` 的 `koinViewModel()`
- [ ] 2.8 執行 `./gradlew :feature:home:build`，確認編譯通過且不再殘留 Hilt 相關 import

## 3. feature/home：Navigation3 遷移

- [ ] 3.1 改寫 `HomeNavigation.kt`：移除 `androidx.navigation.NavController`／`NavGraphBuilder`／`NavOptions`／`androidx.navigation.compose.composable` 與字串路由 `HOME_ROUTE`
- [ ] 3.2 新增 `@Serializable data object HomeKey : NavKey`（`androidx.navigation3.runtime.NavKey`），比照 `MainActivity.kt` 的 `PlaceholderKey` 慣例
- [ ] 3.3 提供產生 `NavEntry` 的方式（例如 `fun homeEntry(onMovieClick: (Int) -> Unit): Pair<NavKey, NavEntry<NavKey>>`），供 `androidApp` 的 `entryProvider` 使用
- [ ] 3.4 執行 `./gradlew :feature:home:build`，確認 Navigation3 改寫後仍可編譯

## 4. androidApp：接上 feature:home 與導覽設定

- [ ] 4.1 `androidApp/build.gradle.kts` 新增 `implementation(projects.feature.home)`
- [ ] 4.2 `MainNavItem.kt` 取消註解並補上 `HOME` 項目，`key` 指向 `feature/home` 的 `HomeKey`
- [ ] 4.3 `MainActivity.kt` 的 `NavDisplay` entryProvider 改為依 `navKey` 分派：`HomeKey` 分派到 `feature/home` 的 `HomeScreen`（暫以 no-op 或 log callback 作為 `onMovieClick`），其餘情況維持回退到 `PlaceholderScreen`
- [ ] 4.4 `JetpackMovieApplication.onCreate()` 的 `loadKoinModules(...)` 加入 `feature/home` 的 `homeModule()`
- [ ] 4.5 執行 `./gradlew :androidApp:assembleDebug`，確認整合後可成功建置

## 5. 驗證與測試

- [ ] 5.1 為 `HomeViewModel` 補上單元測試（`feature/home/src/test`）：以 MockK 模擬 `MovieRepository`／`UserDataRepository`，涵蓋 `movieGenres` 的 Loading/Success/Error 狀態與 `retry()` 行為，採 AAA 模式
- [ ] 5.2 為 `HomeContentViewModel` 補上單元測試：以 MockK 模擬 `MovieRepository`／`GetHomeMovieListUseCase`，涵蓋 `movieList` 資料流與 `toggleMovieCollectStatus()`（收藏/取消收藏兩種分支）
- [ ] 5.3 執行 `./gradlew :feature:home:testAndroidHostTest`（或對應 test task），確認新增測試通過
- [ ] 5.4 執行 `./gradlew ktlintCheck`，確認格式符合規範
- [ ] 5.5 執行 `./gradlew :androidApp:assembleDebug :feature:home:build check`，完整驗證整合結果
