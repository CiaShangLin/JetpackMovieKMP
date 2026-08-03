## 1. shared/app：KoinHelper 擴充

- [x] 1.1 在 `shared/app/src/iosMain/kotlin/com/shang/jetpackmoviekmp/KoinHelper.kt` 新增 `getMovieRecommendUseCase(): GetMovieRecommendUseCase` accessor，比照既有 `getMovieDetailUseCase()` 寫法
- [x] 1.2 執行 `.\gradlew.bat :shared:app:compileKotlinIosSimulatorArm64`（或對應平台 target）確認新 accessor 可編譯，不影響既有 Android 建置

## 2. iosApp：MovieDetail 模組 — UiState

- [x] 2.1 新增 `iosApp/iosApp/Detail/MovieDetailUiState.swift`：定義 `MovieDetailUiState`（`.loading`／`.success(MovieDetailBean)`／`.failure(String)`）（實際檔案放在 `Detail/` 資料夾，非原規劃的 `MovieDetail/`）
- [x] 2.2 ~~在同檔案定義泛型 `DetailSectionState<T>`~~：討論後決定不做泛型共用型別（Kotlin generic 匯出到 iOS 需 `onEnum(of:)` 橋接，比原生 Swift enum 麻煩）。演員與推薦電影區塊改各自用專屬型別：`MovieActorUiState`、`MovieRecommendUiState`（皆為 `.loading`／`.success`／`.failure(String)`，各自放在 `Detail/` 資料夾）

## 3. iosApp：MovieDetail 模組 — ViewModel（主內容 + 收藏）

- [x] 3.1 新增 `iosApp/iosApp/Detail/MovieDetailViewModel.swift`：`@Observable @MainActor final class`，建構子注入 `movieId`（實際為 `Int`，非原規劃的 `Int32`）、`movieRepository`、`getMovieDetailUseCase`、`getMovieRecommendUseCase`
- [x] 3.2 實作主內容載入：`fetchMovieDetail()`（命名與原規劃 `loadDetail()` 不同）以 `for await` 消費 `getMovieDetailUseCase.invoke(movieId:)`，更新 `uiState`
- [x] 3.3 實作 retry：`fetchMovieDetail()` 本身是 single-shot（拿到第一筆即 `return`），`ErrorView` 的 `onRetry` 直接重新呼叫 `fetchMovieDetail()` 即可達成重試，不需要額外的 Task 取消邏輯
- [x] 3.4 實作收藏狀態觀察：`observeCollectStatus()` 以獨立 `Task` 消費 `movieRepository.getMovieCollectEntityById(id:)`，更新 `isCollect`
- [x] 3.5 實作 `toggleMovieCollectStatus(data: MovieCardResult)`（命名與原規劃 `toggleCollect(_:)` 不同）：依 ViewModel 自身觀察到的 `isCollect` 呼叫 `insertMovieCollect()` 或 `deleteMovieCollect()`

## 4. iosApp：MovieDetail 模組 — ViewModel（演員 + 推薦電影）

- [x] 4.1 實作演員載入：`fetchMovieActor()` 獨立 `Task` 消費 `movieRepository.getMovieActor(id:)`，更新 `actorUiState`，失敗僅記錄 `.failure`、不影響其他狀態
- [x] 4.2 實作推薦電影載入：`fetchMovieRecommend()` 獨立 `Task` 消費 `getMovieRecommendUseCase.invoke(movieId:)`，更新 `recommendUiState`；此 Flow 為持續監聽（不像 `fetchMovieDetail()`／`fetchMovieActor()` 拿到第一筆就 `return`），因為 Kotlin 端 `GetMovieRecommendUseCase` 內部用 `.combine(getCollectedMovieIds())` 即時合併收藏狀態，收藏動作寫入 DB 後會自動重新推送更新後的清單
- [x] 4.3 `fetchMovieDetail()`／`observeCollectStatus()`／`fetchMovieActor()`／`fetchMovieRecommend()` 四個獨立 `Task` 各自掛在 `MovieDetailView.body` 的 `.task { }`，彼此不互相 `await`

## 5. iosApp：MovieDetail 模組 — View（主內容 + 收藏 + Loading/Error）

- [x] 5.1 新增 `iosApp/iosApp/Detail/MovieDetailView.swift`：`init(movieId:)` 透過 `KoinHelper.shared` 取得三個 shared 依賴並建立 `MovieDetailViewModel`
- [x] 5.2 實作主內容區塊：背景圖（重用既有 `RemoteAsyncImage`）、標題、評分、上映日期、片長、簡介
- [x] 5.3 依 `uiState` 切換 Loading（重用 `LoadingView`）／Failure（重用 `ErrorView` + retry 按鈕呼叫 `fetchMovieDetail()`）／Success 三種畫面
- [x] 5.4 加上收藏愛心按鈕，依 `isCollect` 顯示狀態並呼叫 `toggleMovieCollectStatus()`；僅在主內容 Success 狀態顯示

## 6. iosApp：MovieDetail 模組 — View（演員 + 推薦電影區塊）

- [x] 6.1 實作演員橫向清單區塊，依 `actorUiState` 顯示；`.failure` 與空清單皆整段隱藏（含標題），不顯示錯誤 UI，比對 Android `feature/detail` 的 `DetailSectionState.Error -> Unit` 行為對齊
- [x] 6.2 實作推薦電影橫向清單區塊，依 `recommendUiState` 顯示，重用既有 `MovieCardView`（含收藏按鈕，`onCollectTap` 接 `toggleRecommendCollectStatus()`）；`.failure` 與空清單同演員區塊，整段隱藏（含標題）
- [x] 6.3 推薦電影卡片的 `onMovieTap` 接上導覽（見第 11 章節）

## 7. iosApp：導覽 — 首頁 tab 接入電影詳情頁

- [x] 7.1 `HomeView` 已具備 `NavigationStack(path: $path)`
- [x] 7.2 掛上 `.navigationDestination(for: Int.self) { movieId in MovieDetailView(movieId: movieId, path: $path) }`（實際用 `Int`，非原規劃的 `Int32`；`HomeContentView` push 時需額外 `Int(movie.movieCardId)` 轉型，因為 Kotlin `Int` 匯出到 Swift 是 `Int32`，與 `Int` 是不同型別，曾因型別不符導致點擊卡片後顯示 SwiftUI 導覽警告畫面，已修正；`path:` 參數是第 11 章節為了支援推薦電影再導覽而追加的）
- [x] 7.3 `MovieCardView(onMovieTap:)` 已改為把 `Int(movie.movieCardId)` `append` 進 `NavigationPath`
- [x] 7.4 手動在模擬器點擊首頁電影卡，驗證確實進入詳情頁且底部 Tab Bar 維持可見（已於 iPhone 16e 模擬器點擊「超級少女」驗證）

## 8. iosApp：導覽 — 搜尋 tab 接入電影詳情頁

- [x] 8.1 在 `SearchView` 既有的 `NavigationStack` 上掛 `.navigationDestination(for: Int.self) { movieId in MovieDetailView(movieId: movieId, path: $path) }`（注意：實際慣例是 Swift 原生 `Int`，不是原規劃的 `Int32`——Kotlin `Int` 匯出到 Swift 是 `Int32`，`MovieCardData.movieCardId` push 前要轉 `Int(movie.movieCardId)`，否則會跟 Home tab 一樣因型別不符顯示導覽警告畫面）
- [x] 8.2 將搜尋結果 `MovieCardView(onMovieTap:)` 接上該 `NavigationPath`，push 時記得 `Int(movie.movieCardId)` 轉型
- [x] 8.3 手動在模擬器搜尋並點擊結果卡片，驗證進入詳情頁（已由使用者手測確認）

## 9. iosApp：導覽 — 收藏 tab 接入電影詳情頁

- [x] 9.1 在 `FavoritesView` 外層包上 `NavigationStack` 並掛 `.navigationDestination(for: Int.self) { movieId in MovieDetailView(movieId: movieId, path: $path) }`（型別注意事項同第 8 章節）
- [x] 9.2 將 `MovieCardView(onMovieTap:)` 接上該 `NavigationPath`，push 時記得 `Int(movie.movieCardId)` 轉型
- [x] 9.3 手動在模擬器點擊收藏電影卡，驗證進入詳情頁（已由使用者手測確認）

## 10. iosApp：導覽 — 歷史 tab 接入電影詳情頁

- [x] 10.1 在 `HistoryView` 外層包上 `NavigationStack` 並掛 `.navigationDestination(for: Int.self) { movieId in MovieDetailView(movieId: movieId, path: $path) }`（型別注意事項同第 8 章節）
- [x] 10.2 將 `MovieCardView(onMovieTap:)` 接上該 `NavigationPath`，push 時記得 `Int(movie.movieCardId)` 轉型
- [x] 10.3 手動在模擬器點擊歷史電影卡，驗證進入詳情頁（已由使用者手測確認）

## 11. iosApp：詳情頁內推薦電影再導覽（多層 push）

- [x] 11.1 `MovieDetailView` 新增 `path: Binding<NavigationPath>`（由 `HomeView` 的 `.navigationDestination(for: Int.self) { MovieDetailView(movieId:, path: $path) }` 傳入），推薦電影卡 `onMovieTap` 透過 `onRecommendMovieTap` closure 呼叫 `path.append(Int(movie.movieCardId))`；因為 `.navigationDestination(for: Int.self)` 只需在 `NavigationStack` 註冊一次即可支援同型別重複 push，不需要額外註冊
- [x] 11.2 手動驗證：從任一 tab 進入詳情頁 → 點擊推薦電影卡 → 進入下一層詳情頁 → 返回一次只 pop 一層，回到上一部電影的詳情頁（已由使用者手測確認）

## 12. 收尾驗證

- [x] 12.1 四個 tab（首頁／搜尋／收藏／歷史）逐一手動驗證卡片點擊皆可進入詳情頁，且切換 tab 後各自的導覽堆疊狀態不互相干擾（已由使用者手測確認）
- [x] 12.2 手動驗證主內容失敗時的 retry 行為、演員／推薦電影區塊失敗時的隱藏行為（已由使用者手測確認）
- [x] 12.3 執行 `ktlintCheck` 確認 Kotlin 變更（`KoinHelper.kt`）通過格式檢查
- [x] 12.4 執行 `iosFormatCheck` 與 `iosCodeStyleCheck` 確認新增 Swift 檔案符合風格規範
