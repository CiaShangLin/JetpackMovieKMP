## Why

`feature/home` 的 `HomeScreenPager` 目前完全沒有判斷 Paging 的 `loadState`，first load 時畫面直接渲染空白 Grid，使用者看不到任何 Loading 提示，往下滑載入更多時也沒有任何提示或錯誤重試。`core/ui` 雖已存在 `MovieListPagerScreen.kt`，但其行為（`refresh`／`append`／`prepend` 任一 Loading/Error 都整頁蓋掉）並不合理，實際上從未被任何畫面呼叫。`feature/search` 則已手刻出正確的行為（first load 整頁 Loading/Error、載入更多時用底部局部提示），但邏輯寫死在 `SearchScreen.kt` 內，未被抽成共用元件。此次重構要建立一套可跨畫面重用、行為正確的 Paging LoadState UI 元件，讓 Home 補齊缺少的 first-load Loading 與載入更多提示，並讓 Search 收斂到同一套邏輯。

## What Changes

- 在 `core/ui` 新增「Paging refresh 整頁 wrapper」Composable：依 `LazyPagingItems.loadState.refresh` 顯示整頁 `LoadingScreen()` / `ErrorScreen(onRetry)` / 實際內容。
- 在 `core/ui` 新增「Paging 分頁 Footer」Composable：吃單一 `LoadState` 參數（呼叫端傳入 `loadState.append` 或 `loadState.prepend`），在 `LazyGridScope` 的 `item {}` 內顯示行內 Loading spinner／行內錯誤重試／「沒有更多了」文字，不影響已顯示內容；不重用 `ErrorScreen`／`LoadingScreen`（兩者為 `fillMaxSize()` 整頁設計，不適合行內顯示）。
- **BREAKING（module-internal）**：移除未被使用的 `core/ui/MovieListPagerScreen.kt`（無對外呼叫端，不影響其他模組編譯）。
- `feature/home` 的 `HomeScreenPager` 套用新的 refresh wrapper 與 append footer，補齊 first-load Loading 顯示與載入更多的提示/重試（新增行為）。
- `feature/search` 的 `SearchScreen.kt` 移除手刻的 `SearchLoadingScreen`／`SearchErrorScreen`／inline append `when` 區塊，改為呼叫新的共用元件（重構既有邏輯，對外行為不變）。

## Capabilities

### New Capabilities
- `android-paging-load-state-ui`：定義 `core/ui` 共用 Paging LoadState UI 元件（refresh 整頁 wrapper、分頁 Footer）的行為契約，以及 `feature/home`、`feature/search` 套用後的驗收準則。

### Modified Capabilities
（無；`android-home-module`、`android-search-module`、`android-ui-module` 既有 spec 僅涵蓋模組遷移／結構驗收準則，不涉及本次的 Paging LoadState 顯示行為，故不需修改既有 Requirement。）

## Impact

- `core/ui`（Android-only module）：新增兩個 Composable（refresh wrapper、分頁 Footer），刪除 `MovieListPagerScreen.kt`。
- `feature/home`（Android-only module）：`HomeScreenPager`（`ui/HomeScreen.kt`）套用新元件，新增 first-load Loading 與 append 提示行為。
- `feature/search`（Android-only module）：`SearchScreen.kt` 重構為呼叫共用元件，移除重複的手刻 Loading/Error/append 判斷邏輯。
- 平台範圍：僅 Android（Compose UI），不涉及 `shared/*` 或 iOS。
- 不在本次範圍：`prepend` 方向目前 Home／Search 皆無實際下拉刷新到頂部重載的情境，Footer 元件設計上支援 `prepend`，但本次不強制在任一畫面實際呼叫 `prepend` 版本。
