## 1. shared/app：KoinHelper 擴充

- [ ] 1.1 在 `shared/app/src/iosMain/kotlin/com/shang/jetpackmoviekmp/KoinHelper.kt` 新增 `getMovieRecommendUseCase(): GetMovieRecommendUseCase` accessor，比照既有 `getMovieDetailUseCase()` 寫法
- [ ] 1.2 執行 `.\gradlew.bat :shared:app:compileKotlinIosSimulatorArm64`（或對應平台 target）確認新 accessor 可編譯，不影響既有 Android 建置

## 2. iosApp：MovieDetail 模組 — UiState

- [ ] 2.1 新增 `iosApp/iosApp/MovieDetail/MovieDetailUiState.swift`：定義 `MovieDetailUiState`（`.loading`／`.success(MovieDetailBean)`／`.failure(String)`）
- [ ] 2.2 在同檔案定義泛型 `DetailSectionState<T>`（`.loading`／`.success(T)`／`.failure(String)`），供演員與推薦電影區塊各自使用

## 3. iosApp：MovieDetail 模組 — ViewModel（主內容 + 收藏）

- [ ] 3.1 新增 `iosApp/iosApp/MovieDetail/MovieDetailViewModel.swift`：`@Observable @MainActor final class`，建構子注入 `movieId: Int32`、`movieRepository: MovieRepository`、`getMovieDetailUseCase: GetMovieDetailUseCase`、`getMovieRecommendUseCase: GetMovieRecommendUseCase`
- [ ] 3.2 實作主內容載入：`loadDetail()` 以 `Task` 包住 `for await` 消費 `getMovieDetailUseCase.invoke(movieId:)`，更新 `detailState`；`start()` 呼叫一次
- [ ] 3.3 實作 `retryDetail()`：取消先前的 detail `Task` 並重新呼叫 `loadDetail()`
- [ ] 3.4 實作收藏狀態觀察：以獨立 `Task` 消費 `movieRepository.getMovieCollectEntityById(movieId:)`，更新 `isCollected`
- [ ] 3.5 實作 `toggleCollect(_ data: MovieCardResult)`：依 `movieCardIsCollect` 呼叫 `insertMovieCollect()` 或 `deleteMovieCollect()`

## 4. iosApp：MovieDetail 模組 — ViewModel（演員 + 推薦電影）

- [ ] 4.1 實作演員載入：獨立 `Task` 消費 `movieRepository.getMovieActor(movieId:)`，更新 `castState`（`DetailSectionState<[MovieCastAndCrewBean.Cast]>`），失敗僅記錄 `.failure`、不影響其他狀態
- [ ] 4.2 實作推薦電影載入：獨立 `Task` 消費 `getMovieRecommendUseCase.invoke(movieId:)`，更新 `recommendationState`（`DetailSectionState<[MovieCardResult]>`）
- [ ] 4.3 確認 `start()` 會同時觸發主內容、收藏、演員、推薦電影四個獨立 `Task`，彼此不互相 `await`

## 5. iosApp：MovieDetail 模組 — View（主內容 + 收藏 + Loading/Error）

- [ ] 5.1 新增 `iosApp/iosApp/MovieDetail/MovieDetailView.swift`：`init(movieId:)` 透過 `KoinHelper.shared` 取得三個 shared 依賴並建立 `MovieDetailViewModel`
- [ ] 5.2 實作主內容區塊：背景圖（重用既有 `RemoteAsyncImage`）、標題、評分、上映日期、片長、簡介
- [ ] 5.3 依 `detailState` 切換 Loading（重用 `LoadingView`）／Error（重用 `ErrorView` + retry 按鈕呼叫 `retryDetail()`）／Success 三種畫面
- [ ] 5.4 加上收藏愛心按鈕，依 `isCollected` 顯示狀態並呼叫 `toggleCollect()`；僅在主內容 Success 狀態顯示

## 6. iosApp：MovieDetail 模組 — View（演員 + 推薦電影區塊）

- [ ] 6.1 實作演員橫向清單區塊，依 `castState` 顯示；`.failure` 時整段隱藏、不顯示錯誤 UI
- [ ] 6.2 實作推薦電影橫向清單區塊，依 `recommendationState` 顯示，重用既有 `MovieCardView`（含收藏按鈕）；`.failure` 時整段隱藏
- [ ] 6.3 推薦電影卡片的 `onMovieTap` 接上導覽（待第 7 章節的 `NavigationPath` 接線後串接，本步驟先預留 closure 參數）

## 7. iosApp：導覽 — 首頁 tab 接入電影詳情頁

- [ ] 7.1 在 `HomeContentView`（或其容器）外層包上 `NavigationStack`／或確認既有 tab 容器已具備可用的 `NavigationPath`
- [ ] 7.2 掛上 `.navigationDestination(for: Int32.self) { movieId in MovieDetailView(movieId: movieId) }`
- [ ] 7.3 將 `MovieCardView(onMovieTap:)` 改為把 `movie.movieCardId` `append` 進該 tab 的 `NavigationPath`
- [ ] 7.4 手動在模擬器點擊首頁電影卡，驗證確實進入詳情頁且底部 Tab Bar 維持可見

## 8. iosApp：導覽 — 搜尋 tab 接入電影詳情頁

- [ ] 8.1 在 `SearchView` 既有的 `NavigationStack` 上掛 `.navigationDestination(for: Int32.self)`（注意與其他 tab 使用一致的 `Int32` 型別）
- [ ] 8.2 將搜尋結果 `MovieCardView(onMovieTap:)` 接上該 `NavigationPath`
- [ ] 8.3 手動在模擬器搜尋並點擊結果卡片，驗證進入詳情頁

## 9. iosApp：導覽 — 收藏 tab 接入電影詳情頁

- [ ] 9.1 在 `FavoritesView` 外層包上 `NavigationStack` 並掛 `.navigationDestination(for: Int32.self)`
- [ ] 9.2 將 `MovieCardView(onMovieTap:)` 接上該 `NavigationPath`
- [ ] 9.3 手動在模擬器點擊收藏電影卡，驗證進入詳情頁

## 10. iosApp：導覽 — 歷史 tab 接入電影詳情頁

- [ ] 10.1 在 `HistoryView` 外層包上 `NavigationStack` 並掛 `.navigationDestination(for: Int32.self)`
- [ ] 10.2 將 `MovieCardView(onMovieTap:)` 接上該 `NavigationPath`
- [ ] 10.3 手動在模擬器點擊歷史電影卡，驗證進入詳情頁

## 11. iosApp：詳情頁內推薦電影再導覽（多層 push）

- [ ] 11.1 將第 6 章節預留的推薦電影卡 `onMovieTap` closure 接上目前詳情頁所在的 `NavigationPath`，`append` 該推薦電影的 `movieCardId`
- [ ] 11.2 手動驗證：從任一 tab 進入詳情頁 → 點擊推薦電影卡 → 進入下一層詳情頁 → 返回一次只 pop 一層，回到上一部電影的詳情頁

## 12. 收尾驗證

- [ ] 12.1 四個 tab（首頁／搜尋／收藏／歷史）逐一手動驗證卡片點擊皆可進入詳情頁，且切換 tab 後各自的導覽堆疊狀態不互相干擾
- [ ] 12.2 手動驗證主內容失敗時的 retry 行為、演員／推薦電影失敗時的區塊隱藏行為
- [ ] 12.3 執行 `.\gradlew.bat ktlintCheck` 確認 Kotlin 變更（`KoinHelper.kt`）通過格式檢查
- [ ] 12.4（若本機已安裝 SwiftFormat／SwiftLint）執行 `.\gradlew.bat iosFormatCheck` 與 `.\gradlew.bat iosCodeStyleCheck` 確認新增 Swift 檔案符合風格規範
