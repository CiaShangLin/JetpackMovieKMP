## Context

`feature/home` 的 `HomeScreenPager`（`ui/HomeScreen.kt`）使用 `viewModel.movieList.collectAsLazyPagingItems()` 取得 `LazyPagingItems<MovieCardResult>`，但完全未讀取 `loadState`，first load 時 `itemCount == 0` 直接渲染空的 `JMLazyVerticalGrid`，沒有 Loading 提示，也沒有載入更多（`append`）的提示或錯誤重試。

`core/ui` 已存在 `MovieListPagerScreen.kt`，把 `refresh`／`append`／`prepend` 混在同一個 `when` 判斷，任一 Loading/Error 就整頁蓋掉顯示 `LoadingScreen()`／`ErrorScreen()`。這代表使用者往下滑載入更多時畫面會整頁閃爍蓋掉既有內容，行為不合理；此元件目前沒有任何呼叫端。

`feature/search` 的 `SearchScreen.kt` 已手刻出正確行為：`loadState.refresh` 才整頁蓋掉（`SearchLoadingScreen`／`SearchErrorScreen`，101-116、146-160 行），`loadState.append`（183-207 行）畫在 Grid 最後一個 `item` 顯示行內 spinner／重試／「沒有更多了」文字，不影響已顯示內容。此邏輯未被抽成共用元件。

`core/ui/ErrorScreen.kt`、`LoadingScreen.kt` 皆以 `fillMaxSize()` 為前提設計，直接重用在行內 Footer 情境會撐滿版面，不適用。

## Goals / Non-Goals

**Goals:**
- 在 `core/ui` 建立一套可跨畫面重用的 Paging LoadState UI 元件，取代行為不合理的舊 `MovieListPagerScreen.kt`。
- 讓 `feature/home` 補齊 first-load Loading 顯示，以及載入更多（`append`）時的提示／錯誤重試／「沒有更多了」提示。
- 讓 `feature/search` 改用同一套共用元件，移除重複的手刻邏輯，對外行為維持不變。
- Footer 元件設計上支援任一方向（`append`／`prepend`），但本次不強制在 Home／Search 實際呼叫 `prepend`。

**Non-Goals:**
- 不引入 `RemoteMediator` 或變更 Paging 資料來源（`MovieGenrePagingSource`、Search 對應 PagingSource 不變）。
- 不變更 `shared/*` 任何 API 或資料流；本次純屬 Android-only UI 層調整。
- 不實作下拉刷新（pull-to-refresh）手勢；`prepend` 僅在元件層面預留擴充能力，不新增互動入口。
- 不調整 `HomeViewModel`／`HomeUiState`（分類清單層的 Loading，與本次 Paging 資料的 Loading 是不同關注點，維持不變）。

## Decisions

### Decision 1：拆成「整頁 refresh wrapper」與「行內分頁 Footer」兩個獨立 Composable，而非單一元件

- **選擇**：`core/ui` 新增 `PagingRefreshContent`（暫名，依 `loadState.refresh` 決定整頁 Loading／Error／`content()`）與 `LazyGridScope.pagingLoadStateFooter`（依傳入的單一 `LoadState` 決定行內 Loading／Error／End 文字，供 `item {}` 呼叫）兩支。
- **原因**：`refresh` 語意上是「蓋住整頁」，`append`／`prepend` 語意上是「附加在既有內容旁邊、不打斷閱讀」，兩者顯示位置與排版完全不同（整頁 vs. 行內），混在同一個 `when`（如舊 `MovieListPagerScreen`）正是導致「載入更多時整頁閃爍」問題的根因。拆開後語意清楚，呼叫端也能各自單獨測試。
- **替代方案**：延用舊 `MovieListPagerScreen` 單一 `when` 寫法，只修正 append/prepend 分支改成不整頁蓋掉——會讓單一函式內同時處理「整頁」與「行內」兩種完全不同的排版責任，可讀性差，且行內 Footer 需要放進 `LazyGridScope.item {}` 內部，無法被包在「先判斷 loadState 再呼叫 content lambda」這種外層 wrapper 的模式裡，技術上也不可行。

### Decision 2：Footer 吃通用 `LoadState` 參數，不寫死 `append` 或 `prepend`

- **選擇**：`pagingLoadStateFooter(loadState: LoadState, onRetry: () -> Unit, ...)`，呼叫端自行傳入 `pager.loadState.append` 或 `pager.loadState.prepend`。
- **原因**：使用者要求 Footer 設計上要能同時支援 append／prepend 兩個方向以利未來重用；`LoadState` 本身已足夠表達 Loading／NotLoading(endOfPaginationReached)／Error 三態，不需要額外綁定方向資訊。
- **替代方案**：分別寫 `pagingAppendFooter` 與 `pagingPrependFooter` 兩支——內部邏輯完全一樣，只是參數來源不同，會造成不必要的重複；否決。

### Decision 3：Footer 不重用 `ErrorScreen`／`LoadingScreen`，改用行內排版

- **選擇**：Footer 內部的 Loading 用小型置中 `CircularProgressIndicator`（或現有 `LoadingScreen(modifier, size)` 但覆寫掉 `fillMaxSize` 前提，視 `LoadingScreen` 現有簽名決定是否可縮小尺寸重用；若不可行則另寫行內版本）；Error 用文字＋重試按鈕但不使用 `Modifier.fillMaxSize()`。
- **原因**：`ErrorScreen.kt` 目前 `Column` 直接 `modifier.fillMaxSize()`，在 `LazyGridScope.item {}` 內使用會撐滿剩餘可捲動區域造成版面異常；`LoadingScreen(modifier, size)` 簽名有開放 `modifier`／`size`，是否可直接重用需在實作階段確認其預設 `modifier` 是否隱含 `fillMaxSize`（依現有程式碼，`LoadingScreen` 未見 `fillMaxSize`，可能可以直接重用，`ErrorScreen` 則不行）。
- **替代方案**：修改 `ErrorScreen` 讓 `fillMaxSize()` 改由呼叫端透過 `modifier` 傳入——會影響所有既有呼叫端（含目前整頁情境）的預設行為，牴觸「修改範圍聚焦」原則，故不採用；改在 Footer 內另外組裝行內排版，不動 `ErrorScreen` 既有 API。

### Decision 4：直接刪除 `MovieListPagerScreen.kt`，不保留相容 API

- **選擇**：新元件上線後，直接刪除 `core/ui/MovieListPagerScreen.kt`。
- **原因**：專案內搜尋確認無任何呼叫端（dead code），且其行為本身就是這次要修正的問題根源，保留只會造成兩套邏輯並存、混淆後續維護者。
- **替代方案**：先標記 `@Deprecated` 再分階段移除——因無呼叫端、無對外 API 相容性疑慮，屬過度設計，不採用。

### Decision 5：`feature/home` 與 `feature/search` 的 retry 行為維持既有機制，不新增 ViewModel 方法

- **選擇**：Footer／refresh wrapper 的 `onRetry` 對 Home 直接呼叫 `LazyPagingItems.retry()`（Paging 內建 API，重試最近一次失敗的 load）；Search 維持既有 `viewModel::retrySearch()`（透過 `retryTrigger` 重新觸發 `flatMapLatest`）不變。
- **原因**：Home 的 Paging Flow（`HomeContentViewModel.movieList`）沒有 Search 那種 debounce／`retryTrigger` 組合需求，`LazyPagingItems.retry()` 已足以重試失敗的 `refresh`／`append`；維持 Search 既有機制可避免不必要的行為變動（本次對 Search 是重構而非新增功能）。
- **替代方案**：讓 Home 也比照 Search 加一層 `retryTrigger`——目前 Home 沒有對應需求（無 debounce、無需重新組合 query），屬過度設計，不採用。

## Risks / Trade-offs

- **[風險] Footer 若誤用內建 `ErrorScreen`／`LoadingScreen` 且未調整版面，可能在 Grid 內撐滿剩餘捲動區域，造成畫面跳動或無法捲動** → 實作時需以實機／預覽確認 Footer 在 `LazyVerticalGrid` 內的實際排版（不使用 `fillMaxSize`，改用 `fillMaxWidth` + 固定高度或 `wrapContentHeight`）。
- **[風險] 刪除 `MovieListPagerScreen.kt` 若遺漏隱藏呼叫端（例如僅存在於未編譯的分支或測試檔）會導致編譯失敗** → 刪除前需全域搜尋確認無任何 import／呼叫，並執行 `:core:ui`、`:feature:home`、`:feature:search`、`:androidApp` 相關建置驗證。
- **[風險] Home 新增 append footer 後，Grid 的 `items` 與 footer `item` 需正確搭配欄位跨度（span），否則可能在多欄 Grid 中版面錯位** → 實作時比照 `SearchResultScreen.kt` 既有 `item { ... }` 寫法（同樣是多欄 `JMLazyVerticalGrid`），並在 Home 實機測試確認 footer 橫跨整列。
- **[取捨] Footer 支援 `prepend` 但本次未實際串接** → 屬本次刻意保留的擴充點（依使用者要求），非缺陷；若未來需要下拉刷新到頂部，直接呼叫既有 Footer 傳入 `loadState.prepend` 即可，不需再設計新元件。

## Migration Plan

1. 於 `core/ui` 新增 `PagingRefreshContent` 與 `pagingLoadStateFooter`（含 ktlint 格式化）。
2. `feature/search` 的 `SearchScreen.kt` 改用新元件，移除 `SearchLoadingScreen`／`SearchErrorScreen`／舊 inline append `when`；確認畫面行為與改動前一致（人工驗證 + 既有測試如有涵蓋需維持通過）。
3. `feature/home` 的 `HomeScreenPager` 套用新元件，新增 first-load Loading 與 append footer。
4. 確認 `core/ui/MovieListPagerScreen.kt` 全域無呼叫端後刪除。
5. 執行 `ktlintCheck`、`:core:ui`、`:feature:home`、`:feature:search`、`:androidApp` 相關建置與既有測試，並於實機／模擬器人工驗證 Home 與 Search 的 first load、載入更多、錯誤重試三種情境。

回退策略：純 Android UI 層變更，若發現問題可直接 revert 對應 commit；不涉及資料庫 schema 或已發佈的 shared/iOS API，無資料遷移風險。

## Open Questions

- `LoadingScreen(modifier, size)` 目前的預設排版是否已可直接縮小尺寸用於 Footer，或需要另外新增行內專用的 Loading 排版？留待實作階段依實際簽名與畫面效果決定。
- Footer 顯示的按鈕／文字（重試、「沒有更多了」）文案是否比照 `feature/search` 既有字串資源（`R.string.search_movie_no_more` 等），或需在 `core/ui` 新增共用字串資源避免各模組各自定義？留待 tasks 階段確認 `core/ui` 現有字串資源慣例後決定。
