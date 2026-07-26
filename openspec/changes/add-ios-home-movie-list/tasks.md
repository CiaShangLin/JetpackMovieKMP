## 1. 前置準備

- [x] 1.1 對照閱讀既有 `GetHomeMovieListUseCase`（`shared/domain`）與 Android `HomeContentViewModel`／Compose `collectAsLazyPagingItems()` 的既有呼叫方式
- [x] 1.2 閱讀 `androidx.paging.PagingDataPresenter` 原始碼與 KDoc（`get`/`peek`/`snapshot`/`collectFrom`/`retry`/`refresh`/`loadStateFlow`/`onPagesUpdatedFlow` 的行為），確認子類別只需覆寫 `presentPagingDataEvent` 作為通知 hook，不需要自己儲存清單
- [x] 1.3 對照閱讀 iOS 既有 `HomeViewModel.swift`（`for await` + `onEnum(of:)` 模式）、`KoinHelper.kt` 既有具名 accessor 慣例，以及 `MovieCardView.swift`（`shared/model` 共用資料模型）
- [x] 1.4 確認 `design.md` 的 4 個 Decisions 沒有疑問（若有想調整的地方，先回頭修改 `design.md` 再開始實作）

## 2. shared/app：iOS 專用分頁 Presenter

- [x] 2.1 在 `shared/app/src/iosMain/kotlin/.../presenter/` 新增內部 `PagingDataPresenter<MovieCardResult>` 子類別（暫定 `HomeMoviePagingDataPresenter`），覆寫 `presentPagingDataEvent` 為 no-op（狀態變化交由內建 `loadStateFlow`／`onPagesUpdatedFlow` 對外可觀察）
- [x] 2.2 新增 `HomeMovieListPresenter.kt`：建構子注入 `GetHomeMovieListUseCase` 與 genre，內部建立 `CoroutineScope(SupervisorJob() + ioDispatcher)`，啟動協程 `getHomeMovieListUseCase(withGenres, internalScope).collectLatest { pagingData -> pagingDataPresenter.collectFrom(pagingData) }`
- [x] 2.3 對外暴露 `get(index)`、`retry()`、`refresh()`、`loadStateFlow`、`onPagesUpdatedFlow`（直接委派給內部 `pagingDataPresenter`）——實作時發現 `loadStateFlow` 不能直接暴露 Paging 原生的 `CombinedLoadStates`（第三方型別未列在 iOS framework `export()` 清單，且整包 export `paging-common` 會與其內部 `PagingLogger.DEBUG` 撞名導致 Objective-C header 編譯失敗），改為透過 2.7 新增的自訂型別轉換後再暴露
- [x] 2.4 實作 `clear()`：取消內部 `CoroutineScope`
- [x] 2.5 在 `KoinHelper.kt` 新增工廠方法（暫定 `fun createHomeMovieListPresenter(withGenres: String): HomeMovieListPresenter`），比照既有具名 accessor 慣例
- [x] 2.6 執行 `./gradlew :shared:app:iosSimulatorArm64Test`（若既有測試存在）或視需要為 `HomeMovieListPresenter` 補充可在 `iosTest` 執行的驗證（例如確認 `clear()` 呼叫後 scope 被取消）——已在 `KoinHelperTest.kt` 新增 `createHomeMovieListPresenter_afterInitKoin_resolvesPresenterAndClears`，測試通過
- [x] 2.7（實作時新增，對應 design.md 決策 5）新增 `HomeMovieListLoadState.kt`：`sealed interface HomeMovieListLoadState { Idle / Loading / Error(message) }` 與 `data class HomeMovieListLoadStates(refresh, append)`，`HomeMovieListPresenter.loadStateFlow` 內部用 `mapNotNull` 把 `pagingDataPresenter.loadStateFlow`（`CombinedLoadStates`）轉成這個自訂型別；已跑 `xcodebuild -scheme iosApp -destination 'generic/platform=iOS Simulator' build` 確認 Swift 端可正常使用（`onEnum(of:)` 分支、`Optional` 型別宣告，因 sealed interface 橋接為 protocol 不支援 enum 字面量預設值）

## 3. iOS：HomeContentView 串接分頁電影清單（使用者實作）

- [ ] 3.1 建立一個暫時的測試畫面，驗證 `presenter.get(index)` 在 Swift 端呼叫時不會拋出執行緒相關例外（對應 design.md Risk：`@MainThread` 標註與 Kotlin/Native `Dispatchers.Main` 的行為確認）
- [ ] 3.2 建立 `iosApp/iosApp/Home/HomeContentView.swift`：接收目前 Genre 對應的 `HomeMovieListPresenter`，觀察 `onPagesUpdatedFlow`（`for await`）後讀取 `snapshot()` 更新畫面用的陣列，並觀察 `loadStateFlow` 決定載入中／錯誤狀態
- [ ] 3.3 用 `LazyVGrid`（或既有 Android 對照的版面）呈現電影清單，逐筆將 `MovieCardResult` 轉成 `MovieCardData`（`asMovieCardData()`），交給既有 `MovieCardView` 呈現；每一列渲染時呼叫 `presenter.get(index)` 取得資料（觸發 Paging 3 依 `prefetchDistance` 判斷是否載入下一頁）
- [ ] 3.4 加上 `.refreshable { presenter.refresh() }` 實作下拉刷新
- [ ] 3.5 `loadStateFlow` 反映 append 載入中時，清單底部顯示載入指示；反映失敗時顯示重試按鈕，呼叫 `presenter.retry()`
- [ ] 3.6 `loadStateFlow` 反映 refresh 失敗且目前無任何已載入資料時，顯示既有 `ErrorView.swift`，重試按鈕呼叫 `presenter.refresh()`
- [ ] 3.7 修改 `HomeView.swift`／`HomeViewModel.swift`：每個 Genre Tab 透過 `KoinHelper.shared.createHomeMovieListPresenter(withGenres:)` 建立對應 `HomeMovieListPresenter` 實例並持有；畫面消失（例如 Tab 被銷毀或使用者離開首頁）時呼叫 `presenter.clear()`
- [ ] 3.8 在模擬器或實機驗證：切換分類 Tab 各自顯示正確電影清單、下拉刷新、捲動到清單尾端自動載入下一頁、（可暫時斷網）觸發失敗畫面與重試、點擊收藏按鈕確認 `onCollectTap` callback 有觸發

## 4. 收尾與整合驗證

- [x] 4.1 執行 `./gradlew ktlintCheck`，確認新增／修改的 Kotlin 檔案格式正確
- [x] 4.2 執行 `./gradlew :shared:domain:testAndroidHostTest :shared:data:testAndroidHostTest`，確認未修改任何 `shared/domain`／`shared/data` 程式碼、既有測試仍然通過
- [x] 4.3 執行 `./gradlew :feature:home:testDebugUnitTest`，確認 Android 端既有首頁測試未受影響
- [ ] 4.4 視環境需要執行 `./gradlew iosFormat iosLint`（需本機已安裝 SwiftFormat／SwiftLint），確認新增的 Swift 檔案符合專案風格
- [ ] 4.5 在實機或模擬器完整跑一次首頁：分類切換、電影清單顯示、下拉刷新、捲動自動載入下一頁、斷網失敗與重試、收藏按鈕 callback，確認皆與 Android 端對照行為一致
