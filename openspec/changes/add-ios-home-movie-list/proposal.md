## Why

iOS 首頁目前只完成分類 Tab（Genre）的載入與顯示，選擇分類後畫面內容仍是空白——`HomeView.swift` 的 `TabView` 內容目前只是佔位，等待「電影清單」串接。現有的 `GetHomeMovieListUseCase` 回傳 `Flow<PagingData<MovieCardResult>>` 且 `invoke()` 簽章要求呼叫端傳入 Android `CoroutineScope`（供 `cachedIn(scope)` 使用），iOS 沒有 `ViewModelScope` 這種呼叫端天然持有的協程作用域可以傳入，也沒有 Compose Paging 的 `collectAsLazyPagingItems()` 可用。因此需要一個 iOS 專用的轉接層（Presenter），內部建立自己的 `CoroutineScope` 並沿用既有 `GetHomeMovieListUseCase`，同時借助 AndroidX Paging 3 本身提供的 `PagingDataPresenter` 擴充點（`AsyncPagingDataDiffer`／`LazyPagingItems` 底層共用的同一個機制，且已隨 `paging-common` 發布 iOS target）驅動分頁載入，讓 iOS 能以「捲動觸發載入下一頁」「下拉刷新」的方式取得電影清單，並完成 `HomeContentView` 的畫面串接。

## What Changes

- **推翻**先前 `add-ios-home-screen` change 的 Decision 4（原規劃 iOS 首頁第一版不做分頁，只載入單頁快照）：本次改為直接實作真正的分頁載入，且**沿用**既有 `GetHomeMovieListUseCase`／`MovieRepository.getMovieListPager()`，不新增平行的 Repository 方法或 UseCase
- 新增 iOS 專用 Presenter（`shared/app` iosMain）：內部建立並持有一個 `CoroutineScope`，傳入既有 `GetHomeMovieListUseCase(withGenres, scope)` 取得 `Flow<PagingData<MovieCardResult>>`，並用該 scope 持續收集此 Flow、餵給內部包裝的 `PagingDataPresenter<MovieCardResult>` 子類別；對外暴露 `get(index)`（存取即觸發分頁載入，比照 Compose `LazyPagingItems` 的行為）、`retry()`、`refresh()`、`loadStateFlow`、`onPagesUpdatedFlow`，並提供明確的 `clear()` 方法供 Swift 在畫面消失時呼叫，避免協程洩漏
- 新增 iOS `HomeContentView`（SwiftUI）：接收目前選定的 Genre，顯示對應分類的電影清單（沿用既有 `MovieCardView`），下拉刷新呼叫 `refresh()`，畫面渲染每一列時呼叫 `get(index)` 觸發必要的分頁載入
- `HomeView.swift` 串接 `HomeContentView`，取代目前的空白佔位內容

## Capabilities

### New Capabilities
- `ios-home-movie-list`：iOS 端電影清單的分頁載入（沿用 AndroidX Paging 3 的 `PagingDataPresenter` 擴充點）、下拉刷新、Presenter 狀態與事件契約，以及 `HomeContentView` 的畫面行為

### Modified Capabilities
（無——本次不修改任何既有 spec 的需求，`GetHomeMovieListUseCase`／`MovieRepository` 既有行為維持不變）

## Impact

- **shared/app**：`KoinHelper.kt`（iosMain）新增建立 Presenter 的工廠方法；新增 Presenter 實作檔案（`iosMain`，包裝 `PagingDataPresenter<MovieCardResult>`，自管 `CoroutineScope`）
- **iosApp**（Xcode 專案，非 Gradle 模組）：新增 `HomeContentView.swift`；`HomeView.swift`／`HomeViewModel.swift` 調整以持有並在畫面消失時釋放 Presenter
- **不受影響**：`shared/domain`（`GetHomeMovieListUseCase`）、`shared/data`（`MovieRepository`／`MovieGenrePagingSource`）、Android 端 `feature/home` 皆維持現狀不動，本次純粹是在既有資料流之上新增 iOS 專用的消費端轉接層
