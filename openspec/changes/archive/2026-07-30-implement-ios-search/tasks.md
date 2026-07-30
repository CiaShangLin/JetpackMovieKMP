## 1. shared/app iOS Paging bridge

- [x] 1.1 在 `shared/app/src/iosMain/.../presenter` 新增 `SearchMoviePagingDataPresenter`，以 `PagingDataPresenter<MovieCardResult>` 接收搜尋資料事件。
- [x] 1.2 新增 `SearchMovieListLoadState`／`SearchMovieListLoadStates` 自有 DTO，將 AndroidX Paging 的 refresh／append `LoadState` 映射成 Swift 可互通的 loading、idle、error 狀態，不 export paging-common 型別。
- [x] 1.3 新增 query-bound `SearchMovieListPresenter`：以 `GetSearchMovieListUseCase`、trimmed query、IO dispatcher 與內部 `SupervisorJob` scope 收集 PagingData；提供 `get`、`snapshot`、`retry`、`refresh`、load-state flow、pages-updated flow、`clear` API 與 KDoc。
- [x] 1.4 擴充 `KoinHelper` 的 iOS API，新增 `createSearchMovieListPresenter(query:)` factory，解析既有 `GetSearchMovieListUseCase` 與 `CommonDispatcher.IO`。
- [x] 1.5 在 `shared/app/src/iosTest` 補齊 KoinHelper factory 的 AAA 測試，驗證 Koin 初始化後可建立並清理 Search Presenter。

## 2. iosApp Search ViewModel

- [x] 2.1 新增 `iosApp/iosApp/Search/SearchViewModel.swift`，使用 `@Observable` 與 `@MainActor` 管理 initial、loading、results、failure UI state、結果 snapshot、append load state 與目前 Presenter。
- [x] 2.2 實作提交 query：trim 空白，空白提交不得建立搜尋；非空 query 時先取消 observation tasks／clear 舊 Presenter，再從 KoinHelper 建立新 Presenter；系統搜尋欄清除只清空暫存文字、保留最近有效 query 的 Presenter 與結果。開始觀察前先同步讀取 snapshot，避免遺失不重播的 pages-updated event。
- [x] 2.3 實作 pages-updated 與 load-state async sequence 的觀察、索引 prefetch、retry、refresh，並在 `deinit` 取消所有 task 與 clear Presenter；error retry 必須操作既有 Presenter 以保留失敗頁碼，下拉 refresh 必須以目前 query 重建 Presenter 並從第 1 頁開始。
- [x] 2.4 實作搜尋結果的收藏切換，沿用 `MovieCollectAction` 與 `MovieRepository`，並防止重複的 concurrent 收藏寫入。
- [x] 2.5 為 Search ViewModel 撰寫 Swift AAA 單元測試，至少覆蓋空白 query、不同 query 的 Presenter 替換與清理、初始 snapshot 回填、retry／refresh 委派、prefetch 與收藏切換。

## 3. iosApp Search SwiftUI 與本地化

- [x] 3.1 將 `SearchView` placeholder 改為 iOS 原生搜尋 UI：在此 tab 的 `NavigationStack` 使用 `navigationTitle` 與 `.searchable`；輸入文字保留在 View `@State`，僅在 `.onSubmit(of: .search)` 提交，系統清除搜尋欄時保留最後結果，從未提交有效 query 時才顯示搜尋引導內容。
- [x] 3.2 使用既有 `LoadingView`、`ErrorView`、`MovieCardView` 與 `LazyVGrid` 呈現首次 loading／error、電影結果、無結果、append loading／error／無更多資料狀態；每張卡片出現時呼叫 ViewModel prefetch。
- [x] 3.3 為結果 grid 加入 `.refreshable` 與收藏回呼；下拉 refresh 必須從第 1 頁重新搜尋，append error retry 則不得清除既有結果或重設為第 1 頁。
- [x] 3.4 在 `Localizable.xcstrings` 新增搜尋標題、`.searchable` prompt、初始引導、無結果、無更多資料等 key 的繁中、英文與預設翻譯，且不以硬編碼文字取代本地化 key。
- [x] 3.5 為 SearchView 的初始／loading／failure／result／無結果／append footer 條件撰寫 SwiftUI 或 ViewModel 層單元測試，驗證重新提交與下拉 refresh 從第 1 頁開始、append retry 保留失敗頁碼。

## 4. 驗證

- [x] 4.1 執行 `./gradlew :shared:app:iosSimulatorArm64Test`，確認 Search Presenter、Koin bridge 與既有 iOS shared tests 通過。
- [x] 4.2 執行 `./gradlew iosFormatCheck`、`./gradlew iosLint` 與 `./gradlew iosCodeStyleCheck`，確認新增 Swift 檔案符合格式與 lint 規範。
- [ ] 4.3 使用 Xcode 或 `xcodebuild` 編譯 iOS simulator target，手動驗證提交搜尋、換 query、下拉刷新、append、錯誤重試與收藏切換。
- [x] 4.4 執行 `./gradlew ktlintCheck`，確認新增 iOS Kotlin Presenter 與 Koin bridge 符合 Kotlin 格式規範。
