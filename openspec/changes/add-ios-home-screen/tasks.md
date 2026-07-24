## 1. 前置準備

- [ ] 1.1 對照閱讀 Android 端首頁完整流程作為心智模型：`feature/home/src/main/java/com/shang/jetpackmoviekmp/feature/home/ui/HomeViewModel.kt`（分類載入）、`HomeContentViewModel.kt`（單一分類的電影清單）、`HomeScreen.kt`（Tab + Pager + Loading/Error 組裝）、`HomeUiState.kt`（狀態定義）
- [ ] 1.2 對照閱讀 `core/ui/src/main/kotlin/com/shang/jetpackmoviekmp/core/ui/MovieCard.kt`（卡片版面）、`LoadingScreen.kt`（Lottie 用法）、`ErrorScreen.kt`（失敗畫面與重試）
- [ ] 1.3 對照閱讀 iOS 既有 Splash 流程作為狀態機／DI 呼叫慣例參考：`iosApp/iosApp/Splash/SplashViewModel.swift`、`SplashUiState.swift`、`SplashView.swift`
- [ ] 1.4 確認 `openspec/changes/add-ios-home-screen/design.md` 的 5 個 Decisions 沒有疑問（若有想調整的地方，先回頭修改 design.md 再開始寫程式）

## 2. Lottie 依賴與 Loading 動畫（先獨立驗證，再接進首頁）

- [ ] 2.1 在 Xcode 專案（`iosApp.xcodeproj`）用 Swift Package Manager 新增 `lottie-ios` 依賴（Airbnb/lottie-ios）
- [ ] 2.2 將 Android 使用的 `core/ui/src/main/res/raw/loading.json` 複製一份到 iOS 專案（例如 `iosApp/iosApp/Common/Resources/loading.json`），加入 target 的 bundle resources
- [ ] 2.3 新建一個暫時的測試畫面（例如在 `HomeView` 裡先寫死顯示 Lottie 動畫），確認 `loading.json` 能被讀取並持續循環播放，畫面與 Android `LoadingScreen`（`animateLottieCompositionAsState` + `IterateForever`）效果相近
- [ ] 2.4 驗證通過後，將這段 Lottie 播放程式碼整理成獨立元件（例如 `iosApp/iosApp/Common/LoadingView.swift`），移除步驟 2.3 的暫時寫死呼叫

## 3. 電影卡片 UI 資料模型搬移（`kmp-movie-card-ui-model`）

- [ ] 3.1 在 `shared/model/src/commonMain/kotlin/com/shang/jetpackmoviekmp/model/` 新增 `MovieCardData.kt`，內容對照現有 `core/ui/src/main/kotlin/com/shang/jetpackmoviekmp/core/ui/MovieCardData.kt`（含 `MovieCardData` data class 與 `asMovieCardResult()`／`asMovieCardData()` 轉換函式），欄位與轉換邏輯保持不變，只調整 package 為 `com.shang.jetpackmoviekmp.model`
- [ ] 3.2 刪除 `core/ui/src/main/kotlin/com/shang/jetpackmoviekmp/core/ui/MovieCardData.kt`，修正 `core/ui/MovieCard.kt` 與 `feature/home`（`HomeContentViewModel.kt`、`HomeScreen.kt`）內對 `MovieCardData`／`asMovieCardResult`／`asMovieCardData` 的 import，改指向 `shared/model` 新位置
- [ ] 3.3 執行 `./gradlew :androidApp:assembleDebug` 或 `./gradlew :feature:home:build`，確認 Android 端搬移後仍可正常編譯
- [ ] 3.4 執行 `./gradlew ktlintCheck`，確認新增／修改的 Kotlin 檔案格式正確

## 4. 電影卡片元件（`ios-movie-card`）

- [ ] 4.1 建立 `iosApp/iosApp/Common/MovieCard/MovieCardView.swift`，輸入型別使用 `Shared` framework 匯出的 `MovieCardData`（第 3 節搬移後的共用型別，對照 design.md 決策 2）
- [ ] 4.2 依 Android `MovieCard.kt` 的版面實作：海報圖（先用 SwiftUI `AsyncImage` 讀取 `movieCardPosterPath`）、標題（`movieCardTitle`，對照 `MovieTitle`）、上映日期（`movieCardReleaseDate`，對照 `MovieReleaseTitle`）、評分（`movieCardVoteAverage`，對照 `MovieRating`）
- [ ] 4.3 加入收藏按鈕（對照 `MovieCollectButton`），以 `movieCardIsCollect: Bool` 決定圖示（愛心實心／空心），點擊時呼叫外部傳入的 `onCollectTap: (MovieCardData) -> Void` closure，元件本身不直接處理收藏邏輯
- [ ] 4.4 加入點擊卡片本體的 `onMovieTap: (MovieCardData) -> Void` closure（對照 Android `onMovieClick`）
- [ ] 4.5 用 SwiftUI Preview 搭配 1-2 筆假的 `MovieCardData` 資料，肉眼確認卡片版面正確（比照 Android `MovieCardPreview`）

## 5. 首頁狀態與假資料

- [ ] 5.1 建立 `iosApp/iosApp/Home/HomeUiState.swift`：定義 `loading`、`success(genres: [MovieGenreBean.MovieGenre], movies: [Int: [MovieCardResult]])`（key 為分類 id，內容維持業務層的 `MovieCardResult`，對照 Android `HomeContentViewModel.movieList` 也是 `MovieCardResult`，轉成 `MovieCardData`的時機留到畫面層）、`failure(debugMessage: String)` 三種狀態（對照 Android `HomeUiState` 三個 case，並吸收 design.md 決策 4 提到「單頁快照」的資料形狀）
- [ ] 5.2 建立假資料檔（例如 `iosApp/iosApp/Home/HomeMockData.swift`）：手刻 2-3 個 `MovieGenreBean.MovieGenre`（比照 Android `HomeScreenPreview` 的 `mockGenres`）與每個分類 3-5 筆 `MovieCardResult` 假電影資料
- [ ] 5.3 建立 `iosApp/iosApp/Home/HomeViewModel.swift`：先只用步驟 5.2 的假資料，`uiState` 直接指派為 `.success(...)`，尚未注入任何 UseCase（對照 design.md 決策 5「先假資料」階段）

## 6. 首頁 Tab + Pager 畫面（先用假資料跑通）

- [ ] 6.1 建立 `iosApp/iosApp/Home/HomeView.swift`，依 `HomeViewModel.uiState` 的三種狀態分派畫面（loading → 步驟 2.4 的 Loading 元件；success → 本節 Tab/Pager 內容；failure → 第 7 節失敗畫面），對照 Android `HomeScreen.kt` 的 `when (state)` 分派邏輯
- [ ] 6.2 在 success 畫面頂部用 SwiftUI 呈現可橫向捲動的分類 Tab（可先用 `ScrollView(.horizontal)` + 自訂按鈕列，或評估原生 `TabView` 樣式），對照 Android `JMScrollableTabRow` + `JMTab` 的分類清單呈現
- [ ] 6.3 用 `TabView(selection:)` 搭配 `.tabViewStyle(.page(indexDisplayMode: .never))` 實作橫向分頁本體，對照 Android `HorizontalPager`
- [ ] 6.4 讓步驟 6.2 的 Tab 選取狀態與步驟 6.3 的 Pager 目前頁面雙向同步（點 Tab 換頁、滑動換頁時 Tab 跟著變），對照 Android `HomeSuccessScreen` 用 `selectedTabIndex` + `LaunchedEffect` 做的雙向同步
- [ ] 6.5 在每個分頁內用 `LazyVGrid`（或 `List`）呈現該分類的電影清單：先把 `HomeUiState.success` 對應分類 id 的 `[MovieCardResult]` 逐筆轉成 `MovieCardData`（呼叫第 3 節搬移的 `asMovieCardData()`），再交給第 4 節的 `MovieCardView` 呈現，對照 Android `HomeScreenPager` 內 `movieList[it]?.asMovieCardData()` 的轉換時機
- [ ] 6.6 用 SwiftUI Preview 或模擬器執行，確認：切換分類 Tab／滑動分頁都能看到假資料電影卡片，且各分類資料不互相混淆

## 7. 失敗畫面與重試

- [ ] 7.1 建立 `iosApp/iosApp/Common/ErrorView.swift`（或 `Home` 資料夾內，視是否已確定要跨頁面共用而定）：顯示錯誤文案與重試按鈕，對照 Android `ErrorScreen.kt`
- [ ] 7.2 在 `HomeViewModel` 暫時新增一個測試用的方法（或直接改假資料為觸發 `.failure(...)`），手動驗證 `HomeView` 在 failure 狀態下能正確顯示步驟 7.1 的失敗畫面
- [ ] 7.3 實作重試按鈕的 action：呼叫 `HomeViewModel` 的 `retry()`，暫時先重新指派回假資料的 `.success(...)`（真正重打 API 留到第 9 節），確認點擊後畫面能從 failure 切回 success

## 8. shared/domain 新增串接真實 API 所需的 UseCase

- [ ] 8.1 對照 `shared/domain/src/commonMain/kotlin/com/shang/jetpackmoviekmp/domain/usecase/GetConfigurationUseCase.kt` 的寫法，新增 `GetMovieGenresUseCase`：注入 `MovieRepository`、`ioDispatcher`，將 `movieRepository.getMovieGenres()` 的 `Flow<Result<MovieGenreBean>>` 轉為 `Flow<AppResult<MovieGenreBean>>`
- [ ] 8.2 在 `shared/domain/src/commonTest/.../domain/usecase/GetMovieGenresUseCaseTest.kt` 補上單元測試（AAA 模式），對照既有 `GetConfigurationUseCaseTest.kt` 的成功／失敗案例寫法
- [ ] 8.3 依 design.md 決策 4，新增一支不分頁的電影清單 UseCase（暫定 `GetHomeMovieListSnapshotUseCase`）：內部呼叫既有 `MovieRepository.getMovieListPager(withGenres)`，將 `Flow<PagingData<MovieCardResult>>` 轉為單頁 `Flow<AppResult<List<MovieCardResult>>>`（可先用 `PagingData` 提供的手段取出目前已載入的第一批資料，實作前先確認 Paging 3 API 是否有現成方法，若沒有則與使用者討論改由 `MovieRepository` 額外提供一支非分頁查詢方法）
- [ ] 8.4 為步驟 8.3 的 UseCase 補上單元測試（AAA 模式），涵蓋成功回傳清單與失敗回傳 `AppResult.Failure` 兩種情境
- [ ] 8.5 執行 `./gradlew :shared:domain:testAndroidHostTest` 確認新測試通過
- [ ] 8.6 在 `shared/app/src/iosMain/kotlin/com/shang/jetpackmoviekmp/KoinHelper.kt` 新增 `getMovieGenresUseCase()`、`getHomeMovieListSnapshotUseCase()`（暫定名稱）兩個具名 accessor，比照既有 `getConfigurationUseCase()` 寫法

## 9. iOS 端串接真實 API

- [ ] 9.1 修改 `HomeViewModel.swift`：建構子改為注入步驟 8.6 新增的兩個 UseCase（透過 `KoinHelper.shared` 取得，比照 `SplashView` 建構 `SplashViewModel` 的方式）
- [ ] 9.2 實作分類載入：對照 `SplashViewModel.loadConfiguration()` 的 `for await result in useCase.invoke()` + `onEnum(of:)` 模式，呼叫 `getMovieGenresUseCase`，成功時取得分類清單、失敗時將 `uiState` 設為 `.failure(...)`
- [ ] 9.3 分類載入成功後，對每個分類呼叫 `getHomeMovieListSnapshotUseCase`（可先用最簡單的循序 `await` 或 `async let` 平行處理），組合成 `HomeUiState.success` 所需的 `[Int: [MovieCardResult]]`
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
