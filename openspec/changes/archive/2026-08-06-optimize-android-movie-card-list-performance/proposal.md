## Why

Android 端顯示 `MovieCard` 的四個清單畫面（首頁、搜尋、收藏、觀看歷史）目前沒有替清單項目加上穩定 `key`，海報圖片也不分場景一律請求 TMDB `original`（最大原始解析度）尺寸，捲動時有不必要的重組與過量圖片解碼/流量成本。此議題源自 `bug-fix/fix-nav3-bottom-tab-back-exit` 實作中發現並記錄於 backlog，現在補上對應優化。

## What Changes

- `feature/home`、`feature/search` 的 Paging 清單改用 `LazyPagingItems.itemKey { }` / `itemContentType { }`，以電影 id 作為穩定 key，取代目前純 index-based 的 `items(itemCount) { }`。
- `feature/collect`、`feature/history` 的一般清單 `items(list) { }` 加上 `key = { it.movieCardId }`。
- 補上 Android 端測試，驗證清單加上穩定 key 後，收藏狀態變更仍正確對應到使用者實際操作的電影項目，不因清單重組而錄到錯誤 item（僅驗證 Android 端消費 `MovieCardResult.isCollect` 後的呈現正確性，不變更 `shared/domain` 既有的收藏狀態合併邏輯）。
- `core/designsystem` 的 `JMAsyncImage` 與 `core/ui` 的 `HostInterceptor` 依顯示情境（清單縮圖 vs 詳情頁大圖）請求不同的 TMDB 圖片尺寸，取代目前不分場景一律請求 `original` 尺寸的行為。
- `core/ui` 的 `MovieCard` 精簡 `Box` 上疊加的 `shadow` + `clip` + `background` + `border` 四層 modifier，降低每個 item 的 draw phase 成本；視覺呈現結果與改動前一致，純內部實作優化，不涉及新增或變更外部可觀察行為。

## Capabilities

### New Capabilities
- `android-movie-list-performance`: 定義 Android 端電影清單（首頁分類分頁、搜尋分頁、收藏清單、觀看歷史清單）在項目 key 化、收藏狀態對應正確性、以及海報圖片依顯示情境請求對應尺寸這三方面的行為契約。

### Modified Capabilities
(無；現有 `android-movie-card` spec 涵蓋的標題版面需求不受本次變更影響)

## Impact

- `feature/home/src/main/java/.../ui/HomeScreen.kt`（Android，`HomeScreenPager` 的 `items(movieList.itemCount)` 改為 `itemKey`/`itemContentType`）
- `feature/search/src/main/java/.../ui/SearchScreen.kt`（Android，`SearchResultScreen` 同上）
- `feature/collect/src/main/java/.../ui/CollectScreen.kt`（Android，`items(movieCollectList)` 補 `key`）
- `feature/history/src/main/java/.../ui/HistoryScreen.kt`（Android，`items(historyList)` 補 `key`）
- `core/ui/src/main/kotlin/.../MovieCard.kt`（Android，`MovieCard` 的 `Box` modifier 疊層精簡）
- `core/ui/src/main/kotlin/.../coil/HostInterceptor.kt`（Android，改為依 request 夾帶的尺寸提示組 URL，未提供時維持 `original`，不影響既有呼叫端）
- `core/ui/src/main/kotlin/.../MovieCard.kt`（Android，`MovieCover` 建立 `ImageRequest` 時夾帶清單縮圖尺寸提示；`core/designsystem` 的 `JMAsyncImage` 簽章不變）
- 對應的 `feature/home`、`feature/search`、`feature/collect`、`feature/history`、`core/ui` 測試（Android host test）

**不在本次範圍**：
- `shared/domain` 的 `GetHomeMovieListUseCase` / `GetSearchMovieListUseCase` 收藏狀態 `combine` 邏輯本身不調整（會同時影響 iOS `HomeMoviePagingDataPresenter` / `SearchMoviePagingDataPresenter`，留待後續視實測效能再開新 change 處理）。
- Paging `PagingConfig`（`pageSize` / `prefetchDistance` / `enablePlaceholders`）維持現狀，不在本次調整。
- iOS 端（`iosApp`、`ios-movie-card`、`ios-async-image-component` 等 capability）不受影響。
