## Context

Android 端 `feature/home` 的 `HomeContentViewModel` 直接呼叫 `GetHomeMovieListUseCase(genre.id, viewModelScope)`，取得 `Flow<PagingData<MovieCardResult>>`，畫面用 Compose Paging（`collectAsLazyPagingItems()`）處理捲動載入下一頁與重新整理；分頁本身（讀取下一頁、快取、去重、prefetch 距離）完全交給 AndroidX Paging 3 處理，`collectAsLazyPagingItems()` 內部其實就是把 `Flow<PagingData<T>>` 餵給一個 `PagingDataPresenter` 子類別（`LazyPagingItems`），並在 Compose 渲染每一列時呼叫 `presenter.get(index)` 觸發必要的分頁載入。

iOS 端目前已完成分類 Tab（Genre）：`HomeViewModel.swift` 直接注入 `MovieRepository`（透過 `KoinHelper.shared.getMovieRepository()`），以 `for await result in movieRepository.getMovieGenres()` + SKIE `onEnum(of:)` 取得分類清單。這個模式之所以成立，是因為 `getMovieGenres()` 回傳的 `Flow<AppResult<MovieGenreBean>>` 本身不需要任何呼叫端持有的協程作用域——SKIE 把 `Flow` 橋接成 Swift 的 async sequence，`for await` 迴圈本身的 `Task` 生命週期就足以驅動訂閱與取消。

電影清單無法直接比照這個模式，因為 `GetHomeMovieListUseCase.invoke(withGenres, scope)` 簽章要求呼叫端傳入 Android `CoroutineScope`（供內部 `cachedIn` 使用，讓多個訂閱者共用同一份分頁快取），iOS 沒有 `ViewModelScope` 這種呼叫端天然持有的協程作用域可以傳入；同時 iOS 也沒有 Compose，無法使用 `collectAsLazyPagingItems()`。

**關鍵發現**：本專案實際使用的 `androidx.paging:paging-common` 版本為 `3.5.0`（`gradle/libs.versions.toml:28`），`shared/data` 已經以 `iosArm64()`／`iosSimulatorArm64()` 為 target 並 `api(libs.androidx.paging.common)`（`shared/data/build.gradle.kts`），確認此版本的 `paging-common` 已發布對應的 iOS klib（本機 Gradle cache 內可見 `paging-common-iosarm64`／`paging-common-iossimulatorarm64` artifact），且其 `commonMain` 內含 `androidx.paging.PagingDataPresenter` 抽象類別——這正是 Android `AsyncPagingDataDiffer`（RecyclerView）與 Compose `LazyPagingItems` 底層共用的同一個「自訂 Paging UI binding」擴充點，並非 Android-only 型別。這代表 iOS 端可以直接重用這個官方擴充點驅動分頁，而不需要繞開 Paging 3 自建一套「依頁碼取得單頁列表」的平行機制。

`openspec/changes/archive/2026-07-26-add-ios-home-screen/design.md` 的 Decision 4 原本規劃「第一版不做分頁，只用單頁快照 UseCase」，本次經使用者確認**推翻**該決策，改為實作真正的分頁載入；且在討論過程中進一步確認**沿用**既有 `GetHomeMovieListUseCase`／`MovieRepository.getMovieListPager()`，取代原本規劃的「新增 Repository 方法＋新增 UseCase」方案。

## Goals / Non-Goals

**Goals:**
- 定義 iOS 專用 Presenter 如何在內部建立 `CoroutineScope`、沿用既有 `GetHomeMovieListUseCase` 取得 `Flow<PagingData<MovieCardResult>>`，並透過 `PagingDataPresenter<MovieCardResult>` 子類別驅動分頁狀態
- 定義 Presenter 對外（Swift）的契約：如何存取已載入項目並觸發下一頁載入（`get(index)`）、`retry()`／`refresh()`、載入狀態如何觀察（`loadStateFlow`／`onPagesUpdatedFlow`），以及 Swift 端何時、如何釋放 Presenter 持有的資源
- 定義 `HomeContentView` 如何依 Genre 切換內容、如何觸發下拉刷新與捲動時的分頁載入

**Non-Goals:**
- 不修改 Android `feature/home` 既有的 `GetHomeMovieListUseCase`／`HomeContentViewModel`／Compose Paging 行為
- 不修改 `shared/domain`、`shared/data`——本次不新增 Repository 方法或 UseCase，純粹在既有 `GetHomeMovieListUseCase` 之上新增 iOS 專用的消費端
- 不處理電影詳情頁、收藏頁、搜尋頁的畫面實作（`MovieCardView` 的 `onMovieTap`／`onCollectTap` callback 介面已存在，本次只負責正確傳入資料）
- 不引入通用的「跨平台分頁框架」；本次是針對「首頁依 Genre 分類的電影清單」這一個具體場景的 iOS Presenter，不追求同時解決搜尋分頁（`MovieSearchPagingSource`）的 iOS 匯出問題

## Decisions

### 1. 沿用既有 `GetHomeMovieListUseCase`，scope 由 iOS Presenter 內部建立

**決策**：不新增 Repository 方法或 UseCase。iOS 專用 Presenter（暫定 `HomeMovieListPresenter`）建構時內部建立一個 `CoroutineScope(SupervisorJob() + ioDispatcher)`，直接呼叫既有 `GetHomeMovieListUseCase(withGenres, internalScope)` 取得 `Flow<PagingData<MovieCardResult>>`。Android 端呼叫方式（`HomeContentViewModel` 傳入 `viewModelScope`）完全不受影響。

**理由**：`GetHomeMovieListUseCase` 要求呼叫端傳入 `CoroutineScope`，單純只是「因為 Android 呼叫端天然有 `viewModelScope` 可以傳」，UseCase 本身的邏輯（`cachedIn` + 標記收藏狀態）與呼叫端是 Android 還是 iOS 無關。iOS 只是沒有天然存在的 scope，解法是「在 iOS 專屬的轉接層裡自己建一個」，而不是重新設計一支不需要 scope 的 UseCase——後者等於為了遷就一個平台特性的缺口，改變 `commonMain` 的既有契約。

**替代方案考慮（已否決）**：
- 新增 Repository 方法「依頁碼取得單頁列表」＋新增不需要 scope 的 UseCase，繞開 `PagingData`——這是本次討論前的初版方案，已被否決：會讓 Android／iOS 出現兩套分頁邏輯（一套走 Paging 3 `cachedIn`，一套手動頁碼＋List 累加），且無法重用 Paging 3 內建的 `retry()`／prefetch 距離等機制，等於重新發明分頁

### 2. Presenter 內部包裝 `PagingDataPresenter<MovieCardResult>` 驅動分頁

**決策**：`HomeMovieListPresenter` 內部持有一個 `PagingDataPresenter<MovieCardResult>` 的具體子類別實例（暫定 `HomeMoviePagingDataPresenter`，只需覆寫 `presentPagingDataEvent`，可為 no-op，因為狀態變化已經透過內建的 `loadStateFlow`／`onPagesUpdatedFlow` 對外可觀察）。Presenter 用內部 scope 啟動一個協程：`getHomeMovieListUseCase(withGenres, internalScope).collectLatest { pagingData -> pagingDataPresenter.collectFrom(pagingData) }`。

對外暴露（透過 SKIE 匯出給 Swift）：
- `get(index): MovieCardResult?`——存取指定 index 的項目，同時觸發 Paging 3 依 `prefetchDistance` 判斷是否要載入下一頁（比照 Compose `LazyPagingItems` 存取 item 的行為）
- `retry()`——重試失敗的載入（不重建 `PagingSource`）
- `refresh()`——建立新一代 `PagingSource`，對應下拉刷新
- `loadStateFlow: StateFlow<CombinedLoadStates?>`——觀察 refresh／append 的載入中／失敗／完成狀態
- `onPagesUpdatedFlow: Flow<Unit>`——每次已呈現的清單內容更新時發出訊號，供 Swift 端觸發重新讀取 `snapshot()`／逐一 `get(index)` 更新畫面
- `clear()`——取消內部 `CoroutineScope`

**理由**：`PagingDataPresenter` 是 AndroidX Paging 3 官方提供、專門給「非 RecyclerView、非 Compose」的自訂 UI binding 使用的擴充點（KDoc 明確以 `AsyncPagingDataDiffer`／`LazyPagingItems` 為例），行為與 Android 端完全一致（同一個 `prefetchDistance`、同一套 `LoadState` 語意），不需要自己重新定義「目前頁碼」「是否還有下一頁」等狀態，也不需要自己判斷什麼時候該發下一頁請求。

**替代方案考慮（已否決）**：
- 自建 `MutableStateFlow` 手動累積清單、追蹤頁碼與 `hasNextPage`（本次討論前的初版方案）——被否決，理由同決策 1，會重新實作 Paging 3 已經內建的邏輯，且容易在邊界情況（例如空頁、重複載入）出錯

### 3. Presenter 生命週期：iOS 呼叫端須持有並在畫面消失時呼叫 `clear()`

**決策**：`KoinHelper.kt` 新增工廠方法（暫定 `fun createHomeMovieListPresenter(withGenres: String): HomeMovieListPresenter`），每個 Genre Tab 各自建立一個 Presenter 實例（比照 Android 每個分類分頁各自一個 `HomeContentViewModel`）。這是本專案 `iosMain` 首次出現「持有狀態、需要顯式釋放」的類別（既有 `KoinHelper.kt` 都是無狀態的一行 accessor）。

**理由**：Presenter 內部的 `CoroutineScope` 沒有 Android `viewModelScope` 那種由 Framework 自動取消的機制，若不顯式釋放會持續執行協程、造成洩漏。

**風險與因應**：詳見 Risks / Trade-offs。

### 4. `HomeContentView` 的下拉刷新與分頁載入觸發方式

**決策**：`HomeContentView`（SwiftUI）使用原生 `.refreshable { presenter.refresh() }` 實作下拉刷新；清單渲染每一列（例如 `ForEach` 搭配 index）時呼叫 `presenter.get(index)` 取得該筆資料——這個呼叫本身就是「存取觸發載入」，不需要額外的「捲到底」判斷邏輯或自訂 `loadNextPage()` 方法。畫面依 `loadStateFlow` 的 append/refresh 狀態決定是否顯示清單底部載入指示或錯誤重試按鈕（呼叫 `retry()`）。

**理由**：這與 Compose `LazyPagingItems` 的使用方式完全對應（`items[index]` 存取即觸發），維持 Android／iOS 兩端「存取即載入」語意一致，不需要另外設計一套「偵測捲到底」的手動觸發邏輯。

## Risks / Trade-offs

- **[Risk] `iosMain` 首次出現「持有狀態、需要顯式釋放」的 Presenter 類別，若 Swift 端忘記呼叫 `clear()` 會造成 `CoroutineScope` 洩漏** → **Mitigation**：`tasks.md` 明確列出「肉眼確認 `clear()` 有被呼叫」的驗證步驟，並在 KDoc 中明確標註呼叫端必須負責釋放
- **[Risk] `PagingDataPresenter.get(index)` 文件標註 `@MainThread`，需確認 Kotlin/Native 的 `Dispatchers.Main` 在 iOS 環境下的行為，以及 Swift 呼叫時的執行緒是否符合預期** → **Mitigation**：`tasks.md` 內建立獨立驗證步驟，先在單一測試畫面確認 `get(index)` 呼叫不會拋出執行緒相關例外，再接入正式 `HomeContentView`
- **[Risk] `PagingDataPresenter` 的 `presentPagingDataEvent` 為 `suspend fun`，且是本次新增類別中少數需要繼承 Paging 3 內部行為的地方，實作時需確保不誤用（例如誤以為要在這裡手動維護清單，而忽略內建的 `pageStore`）** → **Mitigation**：`tasks.md` 要求先閱讀 `PagingDataPresenter` 原始碼與 KDoc（`get`/`peek`/`snapshot`/`collectFrom` 的行為），確認子類別只需要覆寫 `presentPagingDataEvent` 作為通知 hook，不需要自己儲存清單
- **[Trade-off] Presenter 內部自管 `CoroutineScope` 而非讓 Swift 端用原生 `Task` 管理生命週期** → 犧牲「iOS 端生命週期完全由 Swift 原生機制掌控」的單純性，換取「沿用既有 `GetHomeMovieListUseCase`／Paging 3 語意、不需要在 Kotlin 端手動維護分頁狀態」的收益

## Open Questions

- Presenter／內部 `PagingDataPresenter` 子類別的確切命名，留待實作時與使用者確認（本文件暫定 `HomeMovieListPresenter`／`HomeMoviePagingDataPresenter`）
- `HomeMovieListPresenter` 是每個 Genre 各建立一個實例（比照 Android 每個分類一個 `HomeContentViewModel`），還是單一 Presenter 內部依 Genre 切換多組分頁狀態——本文件採前者（較貼近既有 Android 設計），但實作時可再與使用者確認
- Swift 端如何把 `get(index)`／`snapshot()` 轉成 SwiftUI 可用的 `[MovieCardData]` 陣列（例如是否需要一個中介的 `ObservableObject` wrapper 觀察 `onPagesUpdatedFlow` 後重新讀取 `snapshot()`）——這屬於 Swift 端實作細節，留待 `tasks.md` 第 3 節實作時決定
