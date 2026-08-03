## 1. shared/app：KoinHelper 擴充

- [x] 1.1 在 `shared/app/src/iosMain/kotlin/com/shang/jetpackmoviekmp/KoinHelper.kt` 新增 `getMovieRecommendUseCase(): GetMovieRecommendUseCase` accessor，比照既有 `getMovieDetailUseCase()` 寫法
- [x] 1.2 執行 `.\gradlew.bat :shared:app:compileKotlinIosSimulatorArm64`（或對應平台 target）確認新 accessor 可編譯，不影響既有 Android 建置

## 2. iosApp：MovieDetail 模組 — UiState

- [x] 2.1 新增 `iosApp/iosApp/Detail/MovieDetailUiState.swift`：定義 `MovieDetailUiState`（`.loading`／`.success(MovieDetailBean)`／`.failure(String)`）（實際檔案放在 `Detail/` 資料夾，非原規劃的 `MovieDetail/`）
- [ ] 2.2 ~~在同檔案定義泛型 `DetailSectionState<T>`~~：討論後決定不做泛型共用型別（Kotlin generic 匯出到 iOS 需 `onEnum(of:)` 橋接，比原生 Swift enum 麻煩）。演員區塊改用專屬的 `MovieActorUiState`（`Detail/MovieActorUiState.swift`）；推薦電影區塊尚未實作，屆時視情況另建對應狀態型別

## 3. iosApp：MovieDetail 模組 — ViewModel（主內容 + 收藏）

- [x] 3.1 新增 `iosApp/iosApp/Detail/MovieDetailViewModel.swift`：`@Observable @MainActor final class`，建構子注入 `movieId`（實際為 `Int`，非原規劃的 `Int32`）、`movieRepository`、`getMovieDetailUseCase`、`getMovieRecommendUseCase`
- [x] 3.2 實作主內容載入：`fetchMovieDetail()`（命名與原規劃 `loadDetail()` 不同）以 `for await` 消費 `getMovieDetailUseCase.invoke(movieId:)`，更新 `uiState`
- [x] 3.3 實作 retry：`fetchMovieDetail()` 本身是 single-shot（拿到第一筆即 `return`），`ErrorView` 的 `onRetry` 直接重新呼叫 `fetchMovieDetail()` 即可達成重試，不需要額外的 Task 取消邏輯
- [x] 3.4 實作收藏狀態觀察：`observeCollectStatus()` 以獨立 `Task` 消費 `movieRepository.getMovieCollectEntityById(id:)`，更新 `isCollect`
- [x] 3.5 實作 `toggleMovieCollectStatus(data: MovieCardResult)`（命名與原規劃 `toggleCollect(_:)` 不同）：依 ViewModel 自身觀察到的 `isCollect` 呼叫 `insertMovieCollect()` 或 `deleteMovieCollect()`

## 4. iosApp：MovieDetail 模組 — ViewModel（演員 + 推薦電影）

- [x] 4.1 實作演員載入：`fetchMovieActor()` 獨立 `Task` 消費 `movieRepository.getMovieActor(id:)`，更新 `actorUiState`，失敗僅記錄 `.failure`、不影響其他狀態
- [ ] 4.2 實作推薦電影載入：獨立 `Task` 消費 `getMovieRecommendUseCase.invoke(movieId:)`，更新對應狀態（尚未實作）
- [x] 4.3 `fetchMovieDetail()`／`observeCollectStatus()`／`fetchMovieActor()` 三個獨立 `Task` 各自掛在 `MovieDetailView.body` 的 `.task { }`，彼此不互相 `await`；推薦電影的第四個 Task 待 4.2 完成後補上

## 5. iosApp：MovieDetail 模組 — View（主內容 + 收藏 + Loading/Error）

- [x] 5.1 新增 `iosApp/iosApp/Detail/MovieDetailView.swift`：`init(movieId:)` 透過 `KoinHelper.shared` 取得三個 shared 依賴並建立 `MovieDetailViewModel`
- [x] 5.2 實作主內容區塊：背景圖（重用既有 `RemoteAsyncImage`）、標題、評分、上映日期、片長、簡介
- [x] 5.3 依 `uiState` 切換 Loading（重用 `LoadingView`）／Failure（重用 `ErrorView` + retry 按鈕呼叫 `fetchMovieDetail()`）／Success 三種畫面
- [x] 5.4 加上收藏愛心按鈕，依 `isCollect` 顯示狀態並呼叫 `toggleMovieCollectStatus()`；僅在主內容 Success 狀態顯示

## 6. iosApp：MovieDetail 模組 — View（演員 + 推薦電影區塊）

- [x] 6.1 實作演員橫向清單區塊，依 `actorUiState` 顯示；`.failure` 與空清單皆整段隱藏（含標題），不顯示錯誤 UI，比對 Android `feature/detail` 的 `DetailSectionState.Error -> Unit` 行為對齊
- [ ] 6.2 實作推薦電影橫向清單區塊，依推薦電影狀態顯示，重用既有 `MovieCardView`（含收藏按鈕）；`.failure` 時整段隱藏（尚未實作）
- [ ] 6.3 推薦電影卡片的 `onMovieTap` 接上導覽（待 6.2 完成）

## 7. iosApp：導覽 — 首頁 tab 接入電影詳情頁

- [x] 7.1 `HomeView` 已具備 `NavigationStack(path: $path)`
- [x] 7.2 掛上 `.navigationDestination(for: Int.self) { movieId in MovieDetailView(movieId: movieId) }`（實際用 `Int`，非原規劃的 `Int32`；`HomeContentView` push 時需額外 `Int(movie.movieCardId)` 轉型，因為 Kotlin `Int` 匯出到 Swift 是 `Int32`，與 `Int` 是不同型別，曾因型別不符導致點擊卡片後顯示 SwiftUI 導覽警告畫面，已修正）
- [x] 7.3 `MovieCardView(onMovieTap:)` 已改為把 `Int(movie.movieCardId)` `append` 進 `NavigationPath`
- [ ] 7.4 手動在模擬器點擊首頁電影卡，驗證確實進入詳情頁且底部 Tab Bar 維持可見（尚待人工驗收）

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

- [ ] 12.1 四個 tab（首頁／搜尋／收藏／歷史）逐一手動驗證卡片點擊皆可進入詳情頁，且切換 tab 後各自的導覽堆疊狀態不互相干擾（搜尋／收藏／歷史三個 tab 尚未接上導覽，見第 8～10 章節）
- [ ] 12.2 手動驗證主內容失敗時的 retry 行為、演員區塊失敗時的隱藏行為（僅完成程式碼邏輯與 code review，尚未實機／模擬器手動操作驗證；推薦電影區塊尚未實作，待補）
- [x] 12.3 執行 `ktlintCheck` 確認 Kotlin 變更（`KoinHelper.kt`）通過格式檢查
- [x] 12.4 執行 `iosFormatCheck` 與 `iosCodeStyleCheck` 確認新增 Swift 檔案符合風格規範
