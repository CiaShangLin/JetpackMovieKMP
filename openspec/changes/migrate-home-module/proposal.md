## Why

`feature/home` 模組的原始碼已從舊專案手動複製進本 repo（`HomeViewModel`、`HomeContentViewModel`、`HomeScreen`、`HomeUiState`、`HomeNavigation`），但尚未依本專案現有架構調整：`build.gradle.kts` 缺少 Compose／Koin／Navigation3／Paging 等必要依賴，ViewModel 仍殘留舊專案的 Hilt 注入（`@HiltViewModel`、`@AssistedInject`）與錯誤的 package 路徑（`com.shang.data.*`、`com.shang.model.*` 而非本專案的 `com.shang.jetpackmoviekmp.*`），Navigation 仍用 classic `androidx.navigation.compose`（`NavGraphBuilder`／`composable`）而非本專案統一改用的 Navigation3（`NavKey`／`NavEntry`／`NavDisplay`，見 `f637cd6` 的 `MainActivity` 重整）。若不處理，`feature:home` 模組完全無法編譯，也無法接上 `androidApp` 的導覽骨架。

## What Changes

- 補齊 `feature/home/build.gradle.kts` 缺少的 Gradle 依賴：Compose（foundation/material3/ui/runtime/tooling-preview）、`androidx-lifecycle-viewmodel-compose`、`androidx-paging-compose`、`androidx-navigation3-runtime`／`navigation3-ui`、`koin-compose`／`koin-compose-viewmodel`，移除不存在於本專案的 Hilt 相關依賴假設。
- 將 `HomeViewModel`、`HomeContentViewModel` 的建構子注入改為 Koin：新增 `feature/home` 的 `di/HomeModule.kt`（`viewModel { }` / `viewModel { (genre) -> }` 對應 assisted 參數），並在 `HomeScreen.kt` 改用 `org.koin.compose.viewmodel.koinViewModel()` 取代 `hiltViewModel()`／`androidx.hilt.navigation.compose`。
- 修正錯誤的 import package 路徑，統一改為本專案實際命名空間：`com.shang.jetpackmoviekmp.data.repository.*`、`com.shang.jetpackmoviekmp.model.*`、`com.shang.jetpackmoviekmp.core.ui.*`、`com.shang.jetpackmoviekmp.core.designsystem.*`、`com.shang.jetpackmoviekmp.domain.usecase.GetHomeMovieListUseCase`。
- 將 `HomeNavigation.kt` 由 classic Navigation Compose（`NavGraphBuilder.composable`、字串路由）改寫為 Navigation3 慣例：定義 `HomeKey : NavKey`，提供 `NavEntry` 建構方式，供 `androidApp` 的 `entryProvider` 與 `NavBackStack` 使用。
- 在 `androidApp` 接上 `feature:home`：`settings.gradle.kts` 確認 `:feature:home` 已 include（已存在）、`androidApp/build.gradle.kts` 新增 `implementation(projects.feature.home)`、`MainNavItem` 補上 `HOME` 項目、`MainActivity.kt` 的 `entryProvider` 依 `HomeKey` 分派到 `HomeScreen`（取代目前的 `PlaceholderScreen` 佔位邏輯）、`JetpackMovieApplication`／Koin 啟動流程載入新增的 `homeModule()`。

## Capabilities

### New Capabilities
- `android-home-module`：`feature/home` 模組的 Gradle 依賴組態、Koin DI 注入、Navigation3 導覽整合與 `androidApp` 接線的驗收標準。

### Modified Capabilities
（無——`android-app-entry` 既有需求 (splash、Navigation3 骨架、`MainViewModel` Koin 注入) 維持不變，僅是把目前的 `PlaceholderScreen` 分派實作換成真正的 `HomeScreen`，不改變任何既有 requirement 的行為契約。）

## Impact

- **受影響 module**：`feature/home`（新增依賴、修正 import、Koin 化、Navigation3 化）、`androidApp`（新增 `feature:home` 依賴、`MainNavItem`、`MainActivity` entryProvider、Koin module 載入）。
- **依賴管理檔案**：本專案使用 Gradle Version Catalog（無 buildSrc），新增依賴僅需確認 `gradle/libs.versions.toml` 既有 alias（`androidx-navigation3-runtime`／`-ui`、`koin-compose`／`koin-compose-viewmodel`、`androidx-paging-compose`、`androidx-lifecycle-viewmodel-compose` 等皆已存在，見 `androidApp/build.gradle.kts` 既有用法），僅需在 `feature/home/build.gradle.kts` 補上對應 `implementation(libs.xxx)` 宣告，不需新增 catalog 條目。
- **不受影響**：`shared/*` 各模組（`MovieRepository.getMovieGenres()`、`GetHomeMovieListUseCase`、`UserDataRepository` 皆已存在且介面相符，不需修改）。
