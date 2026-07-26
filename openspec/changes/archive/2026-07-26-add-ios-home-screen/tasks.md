## 1. 前置準備

- [x] 1.1 對照閱讀 Android 端首頁完整流程作為心智模型：`feature/home/src/main/java/com/shang/jetpackmoviekmp/feature/home/ui/HomeViewModel.kt`（分類載入）、`HomeContentViewModel.kt`（單一分類的電影清單）、`HomeScreen.kt`（Tab + Pager + Loading/Error 組裝）、`HomeUiState.kt`（狀態定義）
- [x] 1.2 對照閱讀 `core/ui/src/main/kotlin/com/shang/jetpackmoviekmp/core/ui/MovieCard.kt`（卡片版面）、`LoadingScreen.kt`（Lottie 用法）、`ErrorScreen.kt`（失敗畫面與重試）
- [x] 1.3 對照閱讀 iOS 既有 Splash 流程作為狀態機／DI 呼叫慣例參考：`iosApp/iosApp/Splash/SplashViewModel.swift`、`SplashUiState.swift`、`SplashView.swift`
- [x] 1.4 確認 `openspec/changes/add-ios-home-screen/design.md` 的 5 個 Decisions 沒有疑問（若有想調整的地方，先回頭修改 design.md 再開始寫程式）

## 2. Lottie 依賴與 Loading 動畫（先獨立驗證，再接進首頁）

- [x] 2.1 在 Xcode 專案（`iosApp.xcodeproj`）用 Swift Package Manager 新增 `lottie-ios` 依賴（Airbnb/lottie-ios）
- [x] 2.2 將 Android 使用的 `core/ui/src/main/res/raw/loading.json` 複製一份到 iOS 專案（例如 `iosApp/iosApp/Common/Resources/loading.json`），加入 target 的 bundle resources
- [x] 2.3 新建一個暫時的測試畫面（例如在 `HomeView` 裡先寫死顯示 Lottie 動畫），確認 `loading.json` 能被讀取並持續循環播放，畫面與 Android `LoadingScreen`（`animateLottieCompositionAsState` + `IterateForever`）效果相近
- [x] 2.4 驗證通過後，將這段 Lottie 播放程式碼整理成獨立元件（例如 `iosApp/iosApp/Common/LoadingView.swift`），移除步驟 2.3 的暫時寫死呼叫（延後到第 6 節重寫 `HomeView.swift` 時一併處理）

## 3. 電影卡片 UI 資料模型搬移（`kmp-movie-card-ui-model`）

- [x] 3.1 在 `shared/model/src/commonMain/kotlin/com/shang/jetpackmoviekmp/model/` 新增 `MovieCardData.kt`，內容對照現有 `core/ui/src/main/kotlin/com/shang/jetpackmoviekmp/core/ui/MovieCardData.kt`（含 `MovieCardData` data class 與 `asMovieCardResult()`／`asMovieCardData()` 轉換函式），欄位與轉換邏輯保持不變，只調整 package 為 `com.shang.jetpackmoviekmp.model`
- [x] 3.2 刪除 `core/ui/src/main/kotlin/com/shang/jetpackmoviekmp/core/ui/MovieCardData.kt`，修正 `core/ui/MovieCard.kt` 與 `feature/home`（`HomeContentViewModel.kt`、`HomeScreen.kt`、`HomeContentViewModelTest.kt`）內對 `MovieCardData`／`asMovieCardResult`／`asMovieCardData` 的 import，改指向 `shared/model` 新位置
- [x] 3.3 執行 `./gradlew :androidApp:assembleDebug` 或 `./gradlew :feature:home:build`，確認 Android 端搬移後仍可正常編譯
- [x] 3.4 執行 `./gradlew ktlintCheck`，確認新增／修改的 Kotlin 檔案格式正確

## 4. 電影卡片元件（`ios-movie-card`）

- [x] 4.1 建立 `iosApp/iosApp/Common/MovieCard/MovieCardView.swift`，輸入型別使用 `Shared` framework 匯出的 `MovieCardData`（第 3 節搬移後的共用型別，對照 design.md 決策 2）
- [x] 4.2 依 Android `MovieCard.kt` 的版面實作：海報圖（先用 SwiftUI `AsyncImage` 讀取 `movieCardPosterPath`）、標題（`movieCardTitle`，對照 `MovieTitle`）、上映日期（`movieCardReleaseDate`，對照 `MovieReleaseTitle`）、評分（`movieCardVoteAverage`，對照 `MovieRating`）
- [x] 4.3 加入收藏按鈕（對照 `MovieCollectButton`），以 `movieCardIsCollect: Bool` 決定圖示（愛心實心／空心），點擊時呼叫外部傳入的 `onCollectTap: (MovieCardData) -> Void` closure，元件本身不直接處理收藏邏輯
- [x] 4.4 加入點擊卡片本體的 `onMovieTap: (MovieCardData) -> Void` closure（對照 Android `onMovieClick`）
- [x] 4.5 用 SwiftUI Preview 搭配 1-2 筆假的 `MovieCardData` 資料，肉眼確認卡片版面正確（比照 Android `MovieCardPreview`）

## 5. 首頁狀態與假資料

- [x] 5.1 建立 `iosApp/iosApp/Home/HomeUiState.swift`：定義 `loading`、`success(genres: [MovieGenreBean.MovieGenre], movies: [Int: [MovieCardResult]])`（key 為分類 id，內容維持業務層的 `MovieCardResult`，對照 Android `HomeContentViewModel.movieList` 也是 `MovieCardResult`，轉成 `MovieCardData`的時機留到畫面層）、`failure(debugMessage: String)` 三種狀態（對照 Android `HomeUiState` 三個 case，並吸收 design.md 決策 4 提到「單頁快照」的資料形狀）
- [x] 5.2 建立假資料檔（例如 `iosApp/iosApp/Home/HomeMockData.swift`）：手刻 2-3 個 `MovieGenreBean.MovieGenre`（比照 Android `HomeScreenPreview` 的 `mockGenres`）與每個分類 3-5 筆 `MovieCardResult` 假電影資料
- [x] 5.3 建立 `iosApp/iosApp/Home/HomeViewModel.swift`：先只用步驟 5.2 的假資料，`uiState` 直接指派為 `.success(...)`，尚未注入任何 UseCase（對照 design.md 決策 5「先假資料」階段）

## 6. 首頁 Tab + Pager 畫面（先用假資料跑通）

- [x] 6.1 建立 `iosApp/iosApp/Home/HomeView.swift`，依 `HomeViewModel.uiState` 的三種狀態分派畫面（loading → 步驟 2.4 的 Loading 元件；success → 本節 Tab/Pager 內容；failure → 第 7 節失敗畫面），對照 Android `HomeScreen.kt` 的 `when (state)` 分派邏輯
- [x] 6.2 在 success 畫面頂部用 SwiftUI 呈現可橫向捲動的分類 Tab（可先用 `ScrollView(.horizontal)` + 自訂按鈕列，或評估原生 `TabView` 樣式），對照 Android `JMScrollableTabRow` + `JMTab` 的分類清單呈現
- [x] 6.3 用 `TabView(selection:)` 搭配 `.tabViewStyle(.page(indexDisplayMode: .never))` 實作橫向分頁本體，對照 Android `HorizontalPager`
- [x] 6.4 讓步驟 6.2 的 Tab 選取狀態與步驟 6.3 的 Pager 目前頁面雙向同步（點 Tab 換頁、滑動換頁時 Tab 跟著變），對照 Android `HomeSuccessScreen` 用 `selectedTabIndex` + `LaunchedEffect` 做的雙向同步
- [x] 6.5 在每個分頁內用 `LazyVGrid`（或 `List`）呈現該分類的電影清單：先把 `HomeUiState.success` 對應分類 id 的 `[MovieCardResult]` 逐筆轉成 `MovieCardData`（呼叫第 3 節搬移的 `asMovieCardData()`），再交給第 4 節的 `MovieCardView` 呈現，對照 Android `HomeScreenPager` 內 `movieList[it]?.asMovieCardData()` 的轉換時機
- [x] 6.6 用 SwiftUI Preview 或模擬器執行，確認：切換分類 Tab／滑動分頁都能看到假資料電影卡片，且各分類資料不互相混淆

## 7. 失敗畫面與重試

- [x] 7.1 建立 `iosApp/iosApp/Common/ErrorView.swift`（或 `Home` 資料夾內，視是否已確定要跨頁面共用而定）：顯示錯誤文案與重試按鈕，對照 Android `ErrorScreen.kt`
- [x] 7.2 在 `HomeViewModel` 暫時新增一個測試用的方法（或直接改假資料為觸發 `.failure(...)`），手動驗證 `HomeView` 在 failure 狀態下能正確顯示步驟 7.1 的失敗畫面
- [x] 7.3 實作重試按鈕的 action：呼叫 `HomeViewModel` 的 `retry()`，暫時先重新指派回假資料的 `.success(...)`（真正重打 API 留到第 9 節），確認點擊後畫面能從 failure 切回 success

## 8. 調整 MovieRepository.getMovieGenres() 為 AppResult，並新增電影清單快照 UseCase

- [x] 8.1 修改 `shared/data/src/commonMain/kotlin/com/shang/jetpackmoviekmp/data/repository/MovieRepository.kt` 的 `getMovieGenres()` 介面簽章，回傳型別從 `Flow<Result<MovieGenreBean>>` 改為 `Flow<AppResult<MovieGenreBean>>`（對照 design.md 決策 3，此為刻意覆蓋 `GetConfigurationUseCase.kt` KDoc 記載既有慣例的例外）
- [x] 8.2 修改 `MovieRepositoryImpl.kt` 的 `getMovieGenres()` 實作：成功時 emit `AppResult.Success(...)`，失敗時用 `shared/common` 既有的 `Throwable.toAppError()` 轉換後 emit `AppResult.Failure(...)`
- [x] 8.3 修正 `shared/data` 的 `MovieRepositoryImplTest.kt` 裡 `getMovieGenres_emits_success_when_response_succeeds`／`getMovieGenres_emits_failure_when_response_fails` 兩個測試，斷言對象從 `Result.getOrNull()` 改成比對 `AppResult.Success`／`AppResult.Failure`
- [x] 8.4 修正以下測試替身的 `getMovieGenres()` fake 實作，回傳型別與內容改成 `AppResult`：`feature/home/.../HomeViewModelTestFakes.kt`（含 `movieGenresResult` 變數型別，從 `Result<MovieGenreBean>` 改為 `AppResult<MovieGenreBean>`）、`shared/domain/.../DomainTestFakes.kt`、`shared/app/.../AppDiagnosticsTest.kt`、`androidApp/.../MainViewModelTestFakes.kt`
- [x] 8.5 修改 `feature/home/.../HomeViewModel.kt`：把 `movieRepository.getMovieGenres().map { it.fold(onSuccess = ..., onFailure = ...) }` 改成對 `AppResult.Success`／`AppResult.Failure` 的判斷（例如 `when` 表達式）。實作時額外發現原本 `HomeUiState.Error(it.cause)` 只傳 `cause`（且 Home 畫面實際上沒有消費這個 throwable），改寫後直接傳完整的 `AppError`（`HomeUiState.Error(result.error)`），資訊更完整，`HomeUiState.Error` 建構子維持 `Throwable?` 不用改（`AppError` 本身是 `Exception` 子類）
- [x] 8.6 修正 `feature/home/.../HomeViewModelTest.kt`：把 4 處 `movieGenresResult = Result.success(...)`／`Result.failure(...)` 賦值改成 `AppResult.Success(...)`／`AppResult.Failure(...)`
- [x] 8.7 執行 `./gradlew :shared:data:testAndroidHostTest :feature:home:testDebugUnitTest :shared:app:testAndroidHostTest :androidApp:testDebugUnitTest`，確認 8.1-8.6 修改後測試通過（四個模組皆 BUILD SUCCESSFUL）
- [ ] 8.8 依 design.md 決策 4，新增一支不分頁的電影清單 UseCase（暫定 `GetHomeMovieListSnapshotUseCase`）：內部呼叫既有 `MovieRepository.getMovieListPager(withGenres)`，將 `Flow<PagingData<MovieCardResult>>` 轉為單頁 `Flow<AppResult<List<MovieCardResult>>>`（可先用 `PagingData` 提供的手段取出目前已載入的第一批資料，實作前先確認 Paging 3 API 是否有現成方法，若沒有則與使用者討論改由 `MovieRepository` 額外提供一支非分頁查詢方法）
- [ ] 8.9 為步驟 8.8 的 UseCase 補上單元測試（AAA 模式），涵蓋成功回傳清單與失敗回傳 `AppResult.Failure` 兩種情境
- [ ] 8.10 執行 `./gradlew :shared:domain:testAndroidHostTest` 確認新測試通過
- [x] 8.11（部分完成）在 `shared/app/src/iosMain/kotlin/com/shang/jetpackmoviekmp/KoinHelper.kt` 已新增 `getMovieRepository()`（解析 `MovieRepository`，供 iOS 直接呼叫 `getMovieGenres()`；命名沿用既有 `get*()` accessor 慣例，與 design.md 決策 3 已同步更新一致）；`getHomeMovieListSnapshotUseCase()` 隨 8.8-8.10 一併暫緩

## 9. iOS 端串接真實 API（`GetHomeMovieListSnapshotUseCase` 暫緩，電影清單先繼續用假資料）

- [x] 9.1 修改 `HomeViewModel.swift`：建構子改為注入 `MovieRepository`（透過 `KoinHelper.shared.getMovieRepository()`）；`GetHomeMovieListSnapshotUseCase` 依 8.8-8.11 的決議暫緩，先不注入，比照 `SplashView` 建構 `SplashViewModel` 的方式
- [x] 9.2 實作分類載入：對照 `SplashViewModel.loadConfiguration()` 的 `for await result in ... + onEnum(of:)` 模式，呼叫 `movieRepository.getMovieGenres()`，成功時取得分類清單、失敗時將 `uiState` 設為 `.failure(...)`。實作時 `HomeUiState.success` 的 `movies` 欄位直接整個移除（而非保留假資料），`HomeSuccessView` 對應的電影清單渲染（`genrePage(for:)`）先註解掉，`TabView` 目前只有分類 Tab、頁面內容空白，等 9.3 才會恢復
- [ ] 9.3（暫緩，等 8.8-8.11 完成後再做）分類載入成功後，對每個分類呼叫 `getHomeMovieListSnapshotUseCase`，組合成 `HomeUiState.success` 所需的 `[Int: [MovieCardResult]]`；`HomeUiState.success` 要把 `movies` 欄位加回來，`HomeSuccessView` 的 `genrePage(for:)` 也要跟著恢復（目前註解掉的部分）
- [ ] 9.4 移除第 5 節建立的假資料呼叫（`HomeMockData` 檔案可保留供 Preview 使用，但 `HomeViewModel` 正式流程不再讀取它）
- [ ] 9.5 重新走一次第 6、7 節的手動驗證（Tab 切換、Pager 滑動、Loading、Error、重試），確認接上真實 API 後行為與假資料階段一致

## 10. 收尾與整合驗證

- [ ] 10.1 修改 `iosApp/iosApp/Main/MainTab.swift`，將 `.home` case 的 `content` 從目前的 placeholder `HomeView()`（純文字版本）改為串接完成的實際 `HomeView`（若檔名／型別已相同，確認 import 與建構子參數正確即可）
- [ ] 10.2 在 `iosApp/iosApp/Localizable.xcstrings` 新增首頁相關文案 key（例如失敗訊息、重試按鈕文字），比照 `ios-splash-rewrite`／`ios-localization` 既有的 key 命名與雙語（zh-Hant／en）填寫方式
- [ ] 10.3 確認 `main_home_placeholder` 是否還被其他地方使用；若無，評估是否清理該 key（不確定就先保留，避免影響其他尚未實作的頁面）
- [ ] 10.4 在實機或模擬器完整跑一次首頁：切換分類、滑動分頁、下拉或重進頁面觸發 Loading、（可暫時斷網）觸發失敗畫面與重試、點擊收藏按鈕確認 callback 有觸發
- [ ] 10.5 執行 `./gradlew ktlintCheck`（若有修改 Kotlin 檔案）與 `./gradlew :shared:domain:testAndroidHostTest`，確認 shared 端改動未破壞既有測試
- [ ] 10.6 視需要執行 `./gradlew iosFormat iosLint`（需本機已安裝 SwiftFormat／SwiftLint）確認新增的 Swift 檔案符合專案風格
- [ ] 10.7 將「iOS 首頁分頁／無限捲動」待辦記錄到 `openspec/backlog.md`（可用 `/flow-note`），供後續開新 change 處理
