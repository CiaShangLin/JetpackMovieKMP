## ADDED Requirements

### Requirement: `feature/home` 模組 MUST 可獨立編譯

`feature/home` 模組的 `build.gradle.kts` MUST 宣告其原始碼實際使用到的所有 Gradle 依賴（Compose、Koin、Navigation3、Paging、lifecycle-viewmodel-compose 等），MUST NOT 殘留 Hilt 相關依賴假設。

#### Scenario: 模組獨立建置成功

- **WHEN** 執行 `./gradlew :feature:home:build`
- **THEN** 建置 MUST 成功完成，不得因缺少 Compose Runtime／Koin／Navigation3／Paging 等依賴而編譯失敗

#### Scenario: 依賴僅使用既有 Version Catalog alias

- **WHEN** 檢查 `feature/home/build.gradle.kts` 新增的 `implementation(libs.xxx)` 宣告
- **THEN** 使用的 alias MUST 全數存在於 `gradle/libs.versions.toml`（不得新增未定義的 alias，也不得新增 buildSrc 相關檔案）

### Requirement: `feature/home` 的 import 路徑 MUST 對齊本專案實際命名空間

`HomeViewModel`、`HomeContentViewModel`、`HomeScreen`、`HomeUiState` MUST 使用本專案實際存在的 package（`com.shang.jetpackmoviekmp.data.repository.*`、`com.shang.jetpackmoviekmp.model.*`、`com.shang.jetpackmoviekmp.core.ui.*`、`com.shang.jetpackmoviekmp.core.designsystem.*`、`com.shang.jetpackmoviekmp.domain.usecase.*`），MUST NOT 殘留來源專案的舊 package（`com.shang.data.*`、`com.shang.model.*`、`com.shang.designsystem.*`、`com.shang.ui.*` 等不含 `jetpackmoviekmp` 中綴的路徑）。

#### Scenario: 不存在舊 package import

- **WHEN** 檢查 `feature/home/src/main/java` 下所有 `.kt` 檔案的 import 陳述式
- **THEN** MUST NOT 出現 `com.shang.data.`、`com.shang.model.`、`com.shang.designsystem.`、`com.shang.ui.`（不含 `jetpackmoviekmp`）開頭的 import

#### Scenario: `feature/home` 自身原始碼 package 對齊 namespace

- **WHEN** 檢查 `feature/home` 模組 `src/main`／`src/test`／`src/androidTest` 下所有 `.kt` 檔案的 `package` 宣告
- **THEN** MUST 使用 `com.shang.jetpackmoviekmp.feature.home`（含 `.ui`／`.navigation`／`.di` 子套件）為字首，對齊 `build.gradle.kts` 已宣告的 `namespace = "com.shang.jetpackmoviekmp.feature.home"`
- **AND** MUST NOT 殘留來源專案遺留的 `com.shang.home.*`（不含 `jetpackmoviekmp` 中綴）

### Requirement: `HomeViewModel`／`HomeContentViewModel` MUST 透過 Koin 注入

`feature/home` MUST 提供 Koin module（`homeModule()`）供應 `HomeViewModel` 與 `HomeContentViewModel`，MUST NOT 使用 Hilt（`@HiltViewModel`／`@AssistedInject`／`@Assisted`／`@AssistedFactory`）。`HomeContentViewModel` 建構時所需、只有在執行期才知道的 `movieGenre` 參數，MUST 透過 Koin 的參數化 `viewModel { params -> }` 提供，而非改變 `MovieRepository`／`GetHomeMovieListUseCase` 的既有介面。

#### Scenario: Koin module 提供兩個 ViewModel

- **WHEN** 檢查 `feature/home` 的 `di/HomeModule.kt`
- **THEN** MUST 存在 `homeModule()`，內含以 Koin `viewModel { }` 綁定 `HomeViewModel`（注入 `UserDataRepository`／`MovieRepository`）與以參數化 `viewModel { (genre) -> }` 綁定 `HomeContentViewModel`（注入 `MovieRepository`／`GetHomeMovieListUseCase`，並接收執行期傳入的 `movieGenre`）

#### Scenario: 不存在 Hilt 注解

- **WHEN** 檢查 `HomeViewModel.kt`、`HomeContentViewModel.kt`、`HomeScreen.kt` 的原始碼
- **THEN** MUST NOT 出現 `@HiltViewModel`、`@AssistedInject`、`@Assisted`、`@AssistedFactory` 註解，或 `androidx.hilt.navigation.compose.hiltViewModel` 的 import
- **AND** `HomeScreen.kt` 取得 ViewModel 的方式 MUST 使用 `org.koin.compose.viewmodel.koinViewModel()`

#### Scenario: `HomeContentViewModel` 依 page/genre 維持獨立實例

- **WHEN** `HomeScreen.kt` 的 `HorizontalPager` 為不同分頁呼叫 `koinViewModel<HomeContentViewModel>()`
- **THEN** MUST 以包含 `page` 與 `genre.id` 的 key（例如 `"HomeContentViewModel_${page}_${genre.id}"`）與 `parametersOf(genre)` 取得實例，確保各分頁的 `HomeContentViewModel` 不互相共用或覆蓋

### Requirement: `feature/home` 的導覽 MUST 使用 Navigation3

`HomeNavigation.kt` MUST 使用 `androidx.navigation3:navigation3-runtime`（`NavKey`／`NavEntry`）取代 classic `androidx.navigation:navigation-compose`（`NavGraphBuilder.composable`、字串路由 `HOME_ROUTE`），比照 `MainActivity.kt` 既有的 `NavKey` 慣例。

#### Scenario: 不存在 classic Navigation Compose 依賴

- **WHEN** 檢查 `feature/home/build.gradle.kts` 與 `HomeNavigation.kt` 的 import
- **THEN** MUST NOT 出現 `androidx.navigation:navigation-compose` 依賴，或 `androidx.navigation.NavController`／`androidx.navigation.NavGraphBuilder`／`androidx.navigation.compose.composable` 的 import

#### Scenario: 提供 Navigation3 相容的進入點

- **WHEN** 檢查 `HomeNavigation.kt`
- **THEN** MUST 定義一個 `NavKey`（例如 `HomeKey`），並提供可產生對應 `NavEntry` 的方式，供呼叫端（`androidApp`）的 `entryProvider` 使用

### Requirement: `androidApp` MUST 接上 `feature/home` 並可實際導覽至首頁

`androidApp` MUST 依賴 `feature:home`，`MainNavItem` MUST 提供對應首頁的項目，`MainActivity` 的 `NavDisplay` entryProvider MUST 依 `HomeKey` 分派到 `feature/home` 的 `HomeScreen`，且 `feature/home` 所需的 Koin module MUST 在 App 啟動流程中被載入。

#### Scenario: Gradle 依賴接線

- **WHEN** 檢查 `androidApp/build.gradle.kts`
- **THEN** MUST 包含 `implementation(projects.feature.home)`

#### Scenario: 導覽列可切換至首頁並渲染 `HomeScreen`

- **WHEN** 使用者點擊底部導覽列的首頁項目
- **THEN** `MainNavItem.HOME` 對應的 `NavKey` MUST 被加入 `backStack`
- **AND** `NavDisplay` 的 entryProvider MUST 依該 `NavKey` 回傳渲染 `HomeScreen` 的 `NavEntry`，而非目前的 `PlaceholderScreen`

#### Scenario: 其他尚未遷移的分頁仍安全回退

- **WHEN** 使用者切換到 `MainNavItem` 中除首頁外、尚未實作對應畫面的項目（若存在）
- **THEN** entryProvider MUST 回退到 `PlaceholderScreen`，不得因缺少對應分支而拋出例外

#### Scenario: `homeModule()` 隨 App 啟動載入

- **WHEN** 檢查 `JetpackMovieApplication.onCreate()`（或對應 Koin 啟動流程）
- **THEN** MUST 將 `feature/home` 提供的 `homeModule()` 加入 `loadKoinModules(...)` 呼叫，確保 `HomeScreen` 渲染時 `koinViewModel()` 可成功解析依賴
