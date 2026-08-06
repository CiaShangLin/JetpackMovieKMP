## 1. core/ui：新增共用 Paging LoadState 元件

- [x] 1.1 新增 `PagingRefreshContent`（或依實作慣例定名）Composable：依 `LazyPagingItems.loadState.refresh` 顯示整頁 `LoadingScreen()` / `ErrorScreen(onRetry)` / 實際內容，對應 `specs/android-paging-load-state-ui` 的整頁 refresh 元件三個 Scenario。
- [x] 1.2 新增 `pagingLoadStateFooter`（或依實作慣例定名）Composable：吃單一 `LoadState` 參數，供 `LazyGridScope.item {}` 呼叫，依 Loading／Error／`endOfPaginationReached`／其餘狀態分四種情況顯示行內 Loading、行內 Error（含重試）、到底提示文字、或不顯示，對應 `specs/android-paging-load-state-ui` 的行內 Footer 元件四個 Scenario。
- [x] 1.3 確認 `LoadingScreen(modifier, size)` 可直接以縮小 `size` 重用於 Footer；若排版不符需求，改為 Footer 內另組行內 Loading 排版（不修改 `LoadingScreen`／`ErrorScreen` 既有簽名與行為，避免影響其他呼叫端）。
- [x] 1.4 確認 Footer 文案（重試按鈕、「沒有更多了」）字串資源策略：優先重用/搬移 `feature/search` 既有字串資源到 `core/ui`（若 Search 沿用），或在 `core/ui` 新增對應字串資源；避免各模組各自定義重複文案。
- [x] 1.5 執行 `./gradlew ktlintFormat ktlintCheck`，確認新檔案格式通過。

## 2. feature/search：改用共用元件

- [x] 2.1 `SearchScreen.kt` 改為呼叫 `core/ui` 的整頁 refresh 元件包住搜尋結果內容，移除既有 `SearchLoadingScreen`／`SearchErrorScreen` 私有 Composable。
- [x] 2.2 `SearchResultScreen` 內既有 inline `when (movieSearchPager.loadState.append)` 區塊改為呼叫 `core/ui` 的行內 Footer 元件（傳入 `movieSearchPager.loadState.append`），保留 `onRetry = movieSearchPager::retry` 既有重試行為。
- [x] 2.3 人工於模擬器／實機驗證：搜尋 first load Loading、first load 失敗重試、捲動載入更多 Loading、載入更多失敗重試、到達「沒有更多了」五種情境，行為與改動前一致（對應 `specs/android-paging-load-state-ui` 的 Search 相關兩個 Scenario）。
  - 實機驗證通過；驗證過程中發現 `PagingLoadStateFooter` 在 `LazyGridScope.item {}` 內未指定 `span`，Loading／Error 僅佔半格（單欄寬度）。修正：`item(span = { GridItemSpan(maxLineSpan) }) { ... }` 讓 Footer 橫跨整列，修正後重跑 `ktlintFormat ktlintCheck` 及跨模組 `assembleDebug` 皆通過。
- [x] 2.4 檢查 `feature/search` 既有測試（若有涵蓋 `SearchScreen` 相關邏輯）維持通過；若目前無對應 UI 層測試，於本任務註明「Compose UI 手動驗證，無自動化測試覆蓋」作為替代驗證方式紀錄。
  - `./gradlew :feature:search:test` 通過（`SearchViewModelTest` 未受本次 UI 重構影響）。`SearchScreen.kt` 無對應 Compose UI 自動化測試，本次改動採「Compose UI 手動驗證，無自動化測試覆蓋」作為替代驗證方式，實際驗證見任務 2.3。

## 3. feature/home：套用共用元件

- [x] 3.1 `HomeScreenPager`（`ui/HomeScreen.kt`）改為以 `core/ui` 整頁 refresh 元件包住 `JMLazyVerticalGrid`，`onRetry` 呼叫 `movieList` 對應 `LazyPagingItems.retry()`。
- [x] 3.2 於 `JMLazyVerticalGrid` 的 `items {}` 之後新增 `item { }` 呼叫 `core/ui` 行內 Footer 元件，傳入 `movieList.loadState.append`，`onRetry` 呼叫 `LazyPagingItems.retry()`；確認 Footer 在多欄 Grid 中正確橫跨整列（比照 `SearchResultScreen.kt` 既有寫法）。
- [x] 3.3 人工於模擬器／實機驗證：切換首頁分類分頁時 first load 顯示整頁 Loading、first load 失敗顯示整頁 Error 並可重試、捲動到底觸發載入更多顯示行內 Loading、載入更多失敗顯示行內錯誤可重試、資料載完顯示「沒有更多了」，對應 `specs/android-paging-load-state-ui` 的 Home 相關三個 Scenario。
  - 實機驗證通過（同任務 2.3 發現並修正 Footer span 橫跨整列的問題，Home 與 Search 共用同一元件已一併修正）。
- [x] 3.4 確認 `HomeViewModel`／`HomeUiState`（分類清單層 Loading）不受影響，未被誤動。
  - `git status` 確認 `feature/home` 本次僅異動 `HomeScreen.kt`，`HomeViewModel.kt`／`HomeUiState.kt` 未被修改。

## 4. core/ui：移除舊 dead code

- [x] 4.1 全域搜尋確認 `MovieListPagerScreen` 在 `androidApp`、`core/*`、`feature/*` 皆無任何 import 或呼叫端。
- [x] 4.2 刪除 `core/ui/MovieListPagerScreen.kt`。

## 5. 跨模組驗證

- [x] 5.1 執行 `./gradlew :core:ui:assembleDebug :feature:home:assembleDebug :feature:search:assembleDebug :androidApp:assembleDebug`，確認刪除舊元件與新增元件後皆可正常編譯。
- [x] 5.2 執行 `./gradlew ktlintCheck`，確認整體格式規則通過。
- [x] 5.3 執行 `openspec validate unify-paging-load-state --type change --strict --no-interactive`，確認 change 文件通過驗證。
- [x] 5.4 於實機／模擬器完整跑一次 Home 與 Search 的 first load、載入更多、錯誤重試、到底提示情境（彙整任務 2.3、3.3 的驗證結果），確認無回歸。
  - 彙整結果：Home 與 Search 各情境皆正常，Footer span 問題已於 2.3/3.3 修正並重新驗證，無回歸。
