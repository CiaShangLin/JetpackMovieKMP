## Context

`feature/home` 的原始碼是從另一個使用 Hilt + classic Navigation Compose 的專案手動複製過來的，尚未套用本專案的架構慣例：

- 依賴管理：Gradle Version Catalog（`gradle/libs.versions.toml`），無 buildSrc。
- DI：Koin，每模組一個 `di/*Module.kt`，於 `shared/app`／`androidApp` 的 Koin 啟動流程 `loadKoinModules` 載入（範例：`core/ui` 的 `uiModule()`、`androidApp` 的 `mainModule()`）。
- Navigation：Navigation3（`androidx.navigation3:navigation3-runtime`／`navigation3-ui`），以 `NavKey`／`NavEntry`／`NavBackStack`／`NavDisplay` 取代 classic `NavHost`／字串路由，`MainActivity.kt`（`f637cd6`）已建立骨架，`entryProvider` 目前只回傳 `PlaceholderScreen`。
- package 命名空間統一為 `com.shang.jetpackmoviekmp.*`（`core.ui`、`core.designsystem`、`model`、`data.repository`、`domain.usecase`），複製進來的原始碼仍用舊專案的 `com.shang.data.*`／`com.shang.model.*`／`com.shang.designsystem.*`／`com.shang.ui.*`（無 `jetpackmoviekmp` 中綴），且部分 import 已經是新路徑（`HomeContentViewModel` 用 `com.shang.jetpackmoviekmp.*`），呈現不一致狀態，需統一改正。`feature/home` 模組自身的原始碼 package 也殘留來源專案的 `com.shang.home.*`，與 `build.gradle.kts` 的 `namespace = "com.shang.jetpackmoviekmp.feature.home"` 不一致，也與 `core/ui`（`com.shang.jetpackmoviekmp.core.ui`）等既有模組的命名慣例不一致，一併改正（見決策 5）。

已確認 `shared/domain` 的 `GetHomeMovieListUseCase`、`shared/data` 的 `MovieRepository.getMovieGenres()`／`insertMovieCollect`／`deleteMovieCollect`、`UserDataRepository` 皆已存在且介面與 `HomeViewModel`／`HomeContentViewModel` 的呼叫方式相符，不需修改 `shared/*` 任何程式碼。

## Goals / Non-Goals

**Goals:**
- `feature/home` 模組能獨立編譯通過（`./gradlew :feature:home:build`）。
- `HomeViewModel`／`HomeContentViewModel` 改由 Koin 注入，移除 Hilt 依賴假設。
- `HomeNavigation.kt` 改用 Navigation3 API，可被 `androidApp` 的 `NavDisplay` entryProvider 消費。
- `androidApp` 接上 `feature:home`：導覽列可切換到首頁並實際渲染 `HomeScreen`。

**Non-Goals:**
- 不調整 `shared/data`／`shared/domain`／`shared/model` 既有介面或實作。
- 不新增 `MainNavItem` 中 Home 以外的分頁（COLLECT／SEARCH／HISTORY／SETTING 仍維持註解狀態，留待各自的遷移 change 處理）。
- 不處理 `feature/home` 的 iOS 對應（`feature/home` 目前是 Android-only library module，`shared/*` 才是跨平台層）。

## Decisions

### 1. 沿用既有 Repository / UseCase 分層，不修改 `shared/*`
`HomeViewModel` 直接注入 `MovieRepository`／`UserDataRepository`（取類型清單），`HomeContentViewModel` 注入 `MovieRepository` 與 `GetHomeMovieListUseCase`（取單一類型的分頁電影列表）。兩者呼叫的方法簽章皆已存在於 `shared/data`／`shared/domain`，故只需修正 import path，不建立新的 UseCase 或 Repository 方法。

### 2. Koin 注入：一般 `viewModel { }` + 帶參數 `viewModel { params -> }` 取代 Hilt Assisted Inject
`HomeContentViewModel` 原本用 `@HiltViewModel(assistedFactory = ...)` + `@AssistedInject` 傳入執行期才知道的 `movieGenre: MovieGenreBean.MovieGenre`。Koin 沒有獨立的 assisted-factory 機制，改用 Koin 原生的參數注入：

```kotlin
// feature/home/src/main/java/com/shang/jetpackmoviekmp/feature/home/di/HomeModule.kt
fun homeModule() = module {
    viewModel {
        HomeViewModel(userDataRepository = get(), movieRepository = get())
    }
    viewModel { (movieGenre: MovieGenreBean.MovieGenre) ->
        HomeContentViewModel(
            movieRepository = get(),
            getMovieGenreUseCase = get(),
            movieGenre = movieGenre,
        )
    }
}
```

`HomeScreen.kt` 呼叫端對應改為：

```kotlin
viewModel: HomeViewModel = koinViewModel()
// ...
viewModel: HomeContentViewModel = koinViewModel(
    key = "HomeContentViewModel_${page}_${genre.id}",
    parameters = { parametersOf(genre) },
)
```

沿用原本以 `page`／`genre.id` 組成的 `key`，確保 `HorizontalPager` 各分頁的 ViewModel 實例維持獨立（不被 Koin 的預設 scope 快取覆蓋）。

**替代方案考慮**：曾考慮把 `movieGenre` 從建構子拿掉、改成 `HomeContentViewModel` 內部用 `SavedStateHandle` 或外部傳入 function 參數持有 genre 狀態，但這樣會把「每個分頁一個獨立 ViewModel 實例」的既有設計改為單一 ViewModel + 外部狀態切換，改動範圍與風險都更大；Koin 參數注入是對原始碼改動最小、语意最貼近原設計的方案。

### 3. Navigation3：`HomeKey` 取代字串路由 `HOME_ROUTE`
比照 `MainActivity.kt` 的 `PlaceholderKey : NavKey` 慣例，`HomeNavigation.kt` 改為：

```kotlin
package com.shang.jetpackmoviekmp.feature.home.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.shang.jetpackmoviekmp.feature.home.ui.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeKey : NavKey

fun homeEntry(onMovieClick: (Int) -> Unit): Pair<NavKey, NavEntry<NavKey>> =
    HomeKey to NavEntry(HomeKey) { HomeScreen(onMovieClick = onMovieClick) }
```

`androidApp` 端的 `MainActivity.kt` 之 `entryProvider` 從目前的單一 `NavEntry(navKey) { PlaceholderScreen() }` 改為依 `navKey` 分派（`when (navKey) { HomeKey -> ...; else -> PlaceholderScreen() }`），`MainNavItem.HOME` 的 `key` 指向 `HomeKey`。移除 `NavController`／`NavOptions`／`NavGraphBuilder` 等 classic Navigation Compose API 依賴與 import。

### 4. `androidApp` 依賴新增而非 `feature/home` 反向依賴
`androidApp/build.gradle.kts` 新增 `implementation(projects.feature.home)`（比照 `projects.core.ui`／`projects.core.designsystem` 既有寫法），維持既有的「上層 app 組裝、下層模組不知道彼此」依賴方向；`feature/home` 不會意識到 `androidApp` 或 `MainNavItem` 的存在。

### 5. `feature/home` 原始碼 package 統一為 `com.shang.jetpackmoviekmp.feature.home`
來源專案的 `com.shang.home.*` 只是巧合沿用，與本模組 `build.gradle.kts` 已宣告的 `namespace = "com.shang.jetpackmoviekmp.feature.home"` 本來就不一致（AGP 允許 namespace 與程式碼 package 不同名，但會造成命名混淆）。比照 `core/ui`（`com.shang.jetpackmoviekmp.core.ui`）、`core/designsystem`（`com.shang.jetpackmoviekmp.core.designsystem`）的既有慣例，將 `ui`／`navigation`／`di` 三個子套件與對應資料夾（`src/main`、`src/test`、`src/androidTest`）一併搬到 `com/shang/jetpackmoviekmp/feature/home/` 下，`androidApp` 端引用 `HomeKey`／`homeEntry`／`homeModule` 的 import 隨之更新。純機械性重新命名，不改變任何行為。

**替代方案考慮**：曾考慮維持 `com.shang.home.*` 不變（改動範圍較小），但會讓這是專案裡唯一一個「namespace 與程式碼 package 不同」的模組，之後新增 feature module 時容易複製到錯誤慣例；改用統一慣例對長期可維護性較好，故選擇重新命名。

## Risks / Trade-offs

- **[Risk]** `feature/home` 原始碼混雜兩種 package 慣例（`com.shang.*` 與 `com.shang.jetpackmoviekmp.*`），逐一修正時容易漏改，導致編譯錯誤才被發現。
  → **Mitigation**：以 `./gradlew :feature:home:build` 逐步編譯驗證，並在 tasks 中將「修正 import」拆成獨立可驗證的步驟。
- **[Risk]** Koin 參數化 `viewModel { (genre) -> }` 若忘記在呼叫端傳入 `parametersOf(genre)`，會在 runtime 才丟出 `NoDefinitionFoundException`，編譯期不會提示。
  → **Mitigation**：`HomeContentViewModel` 對應的 Koin module 定義完成後，先以既有慣例（如 `DomainModuleTest.kt`）補一個簡單的 Koin module 驗證測試（`checkModules` 或手動 `get<HomeContentViewModel>(parameters = { parametersOf(fakeGenre) })`），在 CI 階段就能抓到遺漏。
- **[Risk]** `MainActivity.kt` 的 `entryProvider` 目前只有 `PlaceholderScreen` 一個分支，改成依 `navKey` 分派後，若後續其他分頁（COLLECT／SEARCH 等）尚未實作，需確保 `else` 分支仍回退到 `PlaceholderScreen`，避免其他 `MainNavItem` 項目啟用時整支 App crash。
  → **Mitigation**：`entryProvider` 保留 `else -> NavEntry(navKey) { PlaceholderScreen() }` 作為預設分支。
- 本次變更不涉及 Room schema，無 migration 風險。

## Migration Plan

1. 補齊 `feature/home/build.gradle.kts` 依賴 → 確認 `./gradlew :feature:home:build` 通過（此階段允許因程式碼問題失敗，但不應再有「找不到依賴」的錯誤）。
2. 修正 `HomeViewModel`／`HomeContentViewModel`／`HomeScreen` 的 import 與 Hilt → Koin 改寫，新增 `HomeModule.kt`。
3. 改寫 `HomeNavigation.kt` 為 Navigation3 API。
4. `androidApp` 接線：新增依賴、`MainNavItem.HOME`、`MainActivity.kt` entryProvider 分派、Koin 啟動流程載入 `homeModule()`。
5. 全模組驗證：`./gradlew :feature:home:build :androidApp:assembleDebug ktlintCheck`。

無需 rollback 策略——純新增/修正既有未整合的模組，不影響其他已上線功能；若驗證失敗可直接在同一 change 內修正後重跑。

## Open Questions

- `HomeScreen` 的 `onMovieClick: (Int) -> Unit` 目前沒有實際導覽目的地（電影詳情頁尚未遷移）。本次先讓 `androidApp` 傳入一個暫時的 no-op 或記錄 log 的 callback，待電影詳情 feature module 遷移時再補上實際導覽邏輯。
