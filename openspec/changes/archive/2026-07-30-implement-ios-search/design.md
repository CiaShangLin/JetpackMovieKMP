## Context

`iosApp/iosApp/Search/SearchView.swift` 現在只顯示 placeholder，但 `MainTab` 已提供 Search tab。共用層的 `GetSearchMovieListUseCase(query, scope)` 已把 `MovieRepository.getMovieSearchPager(query)` 與收藏 id flow 合併，產生 `PagingData<MovieCardResult>`；Android Search 已使用它完成 query、refresh／append 與收藏行為。

SwiftUI 無法直接消費 AndroidX Paging 的 `PagingData`。首頁已在 `shared/app/src/iosMain` 以 `HomeMovieListPresenter` 包裝 `PagingDataPresenter`，並暴露 snapshot、pages-updated flow、轉換後的 load state、`retry()` 與 `refresh()`。Search 與首頁的差異是 Presenter 的生命週期由已提交 query 決定：每次提交新 query 時都必須停止舊 paging stream，避免舊搜尋結果覆寫新畫面。

## Goals / Non-Goals

**Goals:**

- 讓 iOS 使用者從既有 Search tab 以非空白關鍵字提交搜尋並瀏覽 TMDB 分頁結果。
- 將 Paging 3 實作細節留在 `shared/app` 的 iOS Kotlin bridge，Swift 只消費穩定、自有的 presenter API 與 load-state 型別。
- 明確處理初始提示、首次載入／錯誤、append 載入／錯誤、下拉 refresh、下一頁預取、資料結尾及收藏切換。
- 沿用 iOS 既有 `@Observable` + `@MainActor` ViewModel、KoinHelper accessor、`MovieCardView`、`LoadingView` 與 `ErrorView` 的模式。

**Non-Goals:**

- 不修改 `shared:data`、`shared:domain` 的 public contract、TMDB API、資料庫 schema 或 Android Search。
- 不新增搜尋建議、歷史紀錄、篩選、電影詳情導覽、跨裝置同步或第三方依賴。
- 不將 AndroidX Paging 或其 `LoadState` 類型 export 到 iOS framework。

## Decisions

### 建立 query-bound 的 `SearchMovieListPresenter`

在 `shared/app` 的 `iosMain` 新增 `SearchMovieListPresenter` 與內部 `PagingDataPresenter`。其建構子接受 `GetSearchMovieListUseCase`、不可變的 trimmed query 與 IO dispatcher，建立自己的 `SupervisorJob` scope，並以 `collectLatest` 收集該 query 的 paging flow。對 Swift 暴露 `get(index)`、`snapshot()`、`retry()`、`refresh()`、`loadStateFlow`、`onPagesUpdatedFlow` 與 `clear()`。

Presenter 不直接暴露 AndroidX Paging 型別；它沿用並泛化首頁的自有 load-state DTO，至少區分 refresh／append 的 loading、idle、error。若需表示「已無下一頁」，Swift 由 append idle 與清單更新呈現既有的結尾文案，不把 Paging 內部 flag 擴散到 Swift。

替代方案是從 Swift 直接收集 `Flow<PagingData<…>>` 或 export paging-common；前者沒有可消費的 collection 語意，後者會重現目前 `DEBUG` C 巨集衝突與第三方 API 洩漏，故不採用。另一方案是將首頁 Presenter 改造成泛型共用類別；Kotlin/Swift generic interop 與現有已穩定的首頁 bridge 會擴大遷移風險，本次採用結構對齊的專用 Presenter。

### Swift ViewModel 擁有 query 生命週期與非同步觀察工作

新增 `SearchViewModel`（`@Observable`、`@MainActor`）。輸入文字保持在 `SearchView` 的 `@State`；使用者在 keyboard Search 或搜尋按鈕提交後，ViewModel trim query。空 query 會清除目前 presenter、取消觀察 task，並回到 initial 狀態。非空 query 則先取消舊 task、`clear()` 舊 presenter，再透過 `KoinHelper.createSearchMovieListPresenter(query:)` 建立新實例並開始觀察 pages-updated 與 load-state flows。

snapshot 更新時 ViewModel 將 Kotlin collection 轉為 `[MovieCardResult]`；卡片即將出現時呼叫 `get(index)` 以驅動 Paging prefetch。ViewModel 將 refresh load error 映射到初始 loading／failure／results 狀態，並另存 append state。append／refresh error 的 retry 一律交給目前 Presenter 的 `retry()`，保留 Paging 原本失敗頁碼；使用者下拉 refresh 則以目前已提交 query 重建 Presenter，強制由第 1 頁重新載入。`deinit` 必須取消 task 並 clear presenter。

替代方案是直接呼叫既有 Presenter 的 `refresh()`；AndroidX Paging 會由 `PagingSource.getRefreshKey()` 根據目前 anchor position 推算重新載入頁碼，對目前 `MovieSearchPagingSource` 無法保證第 1 頁，故不採用。每次 retry 都重建 Presenter 也不採用，因 append retry 必須重試原本失敗頁碼。

### 搜尋畫面採用 SwiftUI 原生搜尋模式並沿用 Home grid

`SearchView` 在 Search tab 內建立自己的 `NavigationStack`，以 `navigationTitle` 與 SwiftUI `.searchable(text:prompt:)` 提供系統搜尋欄。`.searchable` 是 iOS 原生搜尋控制項，提供系統清除按鈕與鍵盤行為；不再手刻固定的 `TextField` 或清除按鈕。畫面透過 `.onSubmit(of: .search)` 才將文字交給 ViewModel 提交，因此輸入過程不會建立 Presenter 或發送 API 請求。清除系統搜尋欄只清除暫存輸入文字，保留最後一次已提交 query 的結果與 Presenter；下一次提交有效 query 才替換結果。

結果仍使用既有 `ScrollView` + `LazyVGrid` + `MovieCardView`、`.refreshable` 與 append footer；卡片收藏回呼交由 ViewModel 使用 `MovieRepository` 切換。初始狀態與「有效 query 但結果為空」必須分開：後者顯示原生 SF Symbol 與本地化的無結果訊息。新增 search-specific keys 至 `Localizable.xcstrings` 的繁中、英文與預設值，避免硬編碼使用者可見文字。

替代方案是手刻頁面頂部 `TextField`；雖然也是原生控制項，但會自行負責系統搜尋欄已有的 clear、focus 與呈現慣例，不符合本次「全部採 iOS 原生搜尋 UI」的方向，故不採用。引入新的 grid、圖片或狀態元件也會與 iOS Home 視覺及測試模式分叉，故不採用。

### 將 Koin 解析封裝於既有 bridge

`KoinHelper` 新增具名 factory `createSearchMovieListPresenter(query:)`，內部解析 `GetSearchMovieListUseCase` 與 `CommonDispatcher.IO`。Swift 不直接解析 Koin generic API，也不自行建立 Kotlin coroutine scope。補一個 iOS host test，確認 `doInitKoinIos` 後 factory 可解析並可 clear。

## Risks / Trade-offs

- [新 query 的舊 presenter 或觀察 task 未取消] → ViewModel 在建立新 Presenter 前先取消 task、clear 舊 Presenter，並在 `deinit` 重複清理。
- [pages-updated flow 不重播，訂閱前已完成 initial load] → 開始觀察前同步讀 snapshot；若已有項目，立即轉為 results。
- [SwiftUI `TabView` 保留 view state] → ViewModel 僅在 explicit submit 或使用者下拉 refresh 時重建 query，不在 tab 再次顯示時自動重送請求；清除搜尋欄也不取消最後一次結果。
- [Paging API 變更後跨 Swift 互通失敗] → Presenter 僅公開自有 DTO 與 `MovieCardResult`，不 export `paging-common`。
- [共享收藏狀態異動造成 item 更新] → Presenter 持續收集 UseCase 的合併 flow，pages-updated 後重新讀 snapshot。
- [無資料或已到末頁的 UX 不明確] → 首頁沿用的 grid 可呈現空 grid；本次額外以本地化結尾文字清楚顯示 append 已完成。

## Migration Plan

1. 先在 `shared/app` 新增 Presenter、load-state 型別與 KoinHelper factory，並補 Kotlin iOS host test。
2. 再建立 Swift Search ViewModel、SearchView、localization 與 Swift tests。
3. 以 `./gradlew :shared:app:iosSimulatorArm64Test`、`./gradlew iosFormatCheck`、`./gradlew iosLint` 與 iOS simulator build/test 驗證。
4. 若需回退，只將 `SearchView` 恢復 placeholder 並移除 search-specific Presenter／factory／Swift 檔案；shared data 與既有首頁不受影響。

## Open Questions

- 無；本次與 Android Search 一致，以「明確送出」而非輸入即搜尋為準，並沿用 iOS Home 的 collection toggle 與 Paging UX。
