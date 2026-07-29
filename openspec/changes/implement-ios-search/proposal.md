## Why

iOS 主畫面的搜尋分頁目前仍是 placeholder，使用者無法使用既有的 TMDB 電影搜尋能力。shared domain 已提供帶有收藏狀態同步的 `GetSearchMovieListUseCase`，但其回傳的 `PagingData` 不能直接被 SwiftUI 消費，因此需要建立與首頁一致的 iOS 專用 Presenter bridge。

## What Changes

- 將 iOS `SearchView` 由 placeholder 擴充為可輸入、提交與瀏覽電影搜尋結果的 SwiftUI 畫面。
- 在 `shared/app` iOS source set 新增搜尋分頁 Presenter，將 `PagingData<MovieCardResult>` 轉換為 Swift 可讀取的 snapshot、refresh／append 載入狀態與 retry／refresh 操作。
- 新增 iOS Search ViewModel，負責提交 query 時建立與釋放 Presenter、訂閱分頁狀態、觸發預取與切換收藏。
- 補齊 iOS 搜尋的本地化字串、Koin bridge、Kotlin iOS host test，以及 SwiftUI ViewModel 的單元測試。

## Capabilities

### New Capabilities

- `ios-movie-search`: iOS 使用者可從底部搜尋分頁提交電影關鍵字，瀏覽可分頁的結果，並處理載入、錯誤、重新整理、更多資料與收藏切換。

### Modified Capabilities

- 無。

## Impact

- 受影響 module：`iosApp`（Search SwiftUI 畫面、ViewModel、localization、Swift tests）、`shared/app`（iosMain 的 Search Paging Presenter、KoinHelper、iosTest）。
- 重用 `shared/domain` 的 `GetSearchMovieListUseCase`、`shared/data` 的 `MovieRepository`、`shared/model` 與既有 `shared/app` 的首頁 Paging bridge；不變更 TMDB API、Repository 合約、Room schema 或第三方依賴。
