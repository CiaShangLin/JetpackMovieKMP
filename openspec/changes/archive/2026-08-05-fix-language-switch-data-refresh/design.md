## Context

目前語言切換分成兩個獨立層次，且只有其中一層已經正確運作：

- **靜態字串**（底部導覽列、標題等 `stringResource`）：`MainActivity.kt` 用 `remember(userData.languageMode) { LanguageSettingUtils.updateActivityLocale(...) }` 同步更新 `Activity.resources` 的 Configuration，再用 `key(userData.languageMode) { MainScreen(...) }` 強制整棵子樹重組讀取新字串——這是 2026-08-04 已歸檔的 `fix-android-language-switch-refresh` 解決的問題，本次不變更。
- **ViewModel 內的遠端資料**（本次要修的問題）：`SuccessScreen` 用 `remember(backStack) { movableContentOf { NavDisplay(...) } }` 讓 Nav3 的 backStack／NavEntry 組合狀態在外層 `key()` 重組時「搬移」而非「銷毀重建」，這是刻意設計（避免語言切換時使用者的導覽深度、scroll 位置被重置）。代價是 NavEntry-scope 的 ViewModel（`HomeContentViewModel`、`SearchViewModel`、`MovieDetailViewModel` 等）不會因語言切換被 Koin 重新建立，而目前也沒有任何機制讓它們主動偵測語言變化、重新呼叫 TMDB API，導致切換語言後畫面內容停留在舊語言。

已確認 `feature/collect`、`feature/history` 的資料來源是本地 Room 資料庫（`MovieRepository.getAllMovieCollect()`、`GetHistoryMovieListUseCase()`），並非依當前語言即時呼叫 TMDB，因此語言切換不會改變這兩個畫面顯示的內容，不在本次範圍內（見 Non-Goals）。

`HomeViewModel` 建構子已經注入 `UserDataRepository`，但目前完全沒有使用它——這次剛好可以直接用來加上語言監聽，不需新增建構參數；其餘三個 ViewModel（`HomeContentViewModel`、`SearchViewModel`、`MovieDetailViewModel`）目前未注入 `UserDataRepository`，需要新增。

## Goals / Non-Goals

**Goals:**
- 使用者切換語言後，`HomeViewModel.movieGenres`、`HomeContentViewModel.movieList`、`SearchViewModel.movieSearchPager`、`MovieDetailViewModel` 的 `movieDetail`／`movieRecommendations`／`movieActors` 都會以新語言重新呼叫對應 UseCase
- Paging 資料（Home 片單、Search 結果）語言變化時從 page 1 重新載入，捨棄舊語言分頁快取
- 不改變 Navigation3 的 back stack 深度、不觸發 NavEntry-scope ViewModel 重建，維持使用者當下的導覽狀態
- 沿用專案既有的 `flatMapLatest` 慣例（`SearchViewModel`、`MovieDetailViewModel` 已有前例），不引入新的架構模式

**Non-Goals:**
- 不處理 `feature/collect`／`feature/history` 本地資料庫內容隨語言重新翻譯（需要 Repository 層的語言感知重新抓取，屬於更大範圍的變更，留待未來 change）
- 不引入 `BaseViewModel` 或跨模組共用 abstraction；本次先讓 4 個 ViewModel 各自實作，若未來有更多畫面出現同樣需求，再評估抽出共用小工具
- 不變更 `MainActivity` 現有的 `key(userData.languageMode)`／`updateActivityLocale`／`movableContentOf` 機制
- 不影響 iOS：`iosApp` 的 SwiftUI 頁面走獨立的 `ios-localization` 機制與各自的 ViewModel 生命週期，與本次 Android-only 的 Navigation3／Koin 現況無關
- `LanguageSettingUtils.setApplicationLocales()`（`AppCompatDelegate` 版本）僅作為切換測試用的替代方法新增，本次不接入正式流程，也不移除現有 `updateActivityLocale`（見 Open Questions）

## Decisions

### 1. 各 ViewModel 直接注入 `UserDataRepository`，不新增 Nav3 生命週期或 Koin scope 機制

討論過兩個方案：(a) 讓 Nav3 生命週期跟著語言切換重建 NavEntry-scope ViewModel（比照 Theme 的重組方式），(b) 各 ViewModel 自行注入 `UserDataRepository` 監聽 `languageMode`。選擇 (b)，因為 (a) 會打破 `movableContentOf` 刻意保留的導覽狀態設計——語言切換會連帶重置使用者當下的導覽深度與已離開視野畫面的狀態，且 Nav3 沒有內建「局部失效」機制，需要土炮實作、複雜度高、牽動所有 feature 的 navigation entry 寫法。(b) 的風險侷限在單一 ViewModel 內，且符合現有「Repository Flow → ViewModel 收集」慣例。

### 2. 語言訊號的取得方式：`userDataRepository.userData.map { it.languageMode }.distinctUntilChanged()`

各 ViewModel 統一用這個 pattern 取得語言變化訊號，避免 `userData` 其他欄位（如 `themeMode`）變化時誤觸發資料重新載入。

### 3. Paging 資料改用「語言訊號驅動 `flatMapLatest`」重建 `Pager.flow`

- `HomeContentViewModel.movieList`：新增注入 `UserDataRepository`，把現有 `getMovieGenreUseCase(movieGenre.id.toString(), viewModelScope)` 包進 `languageMode.flatMapLatest { getMovieGenreUseCase(...) }`
- `SearchViewModel.movieSearchPager`：目前巢狀順序是 `searchQuery.debounce(...).flatMapLatest { retryTrigger.flatMapLatest { getSearchMovieListUseCase(...) } }`。加入 `languageMode` 後改用 `combine(debounce 後的 query, retryTrigger, languageMode) { query, _, _ -> query }.flatMapLatest { ... }` 攤平，避免三層巢狀 `flatMapLatest` 影響可讀性

### 4. 一次性資料改用「語言訊號併入既有 retry 觸發鏈」的 `flatMapLatest`

- `MovieDetailViewModel.movieDetail`：目前是 `retryTrigger.flatMapLatest { getMovieDetailUseCase(movieId) }`，改為 `combine(retryTrigger, languageMode) { _, _ -> Unit }.flatMapLatest { getMovieDetailUseCase(movieId) }`
- `movieRecommendations`、`movieActors`：目前沒有 retry 觸發，直接是 `map` 產生的 `StateFlow`，新增 `languageMode.flatMapLatest { ... }` 包裝即可，不影響 `retryMovieDetail()` 現有只重試 `movieDetail` 的行為

### 5. `HomeViewModel.movieGenres` 沿用既有 `_refreshTrigger.flatMapLatest` 結構

目前是 `_refreshTrigger.flatMapLatest { movieRepository.getMovieGenres() }`，比照 Detail 的做法把 `languageMode` 併入同一個觸發鏈（`combine(_refreshTrigger, languageMode) { _, _ -> Unit }.flatMapLatest { ... }`），不需新增建構參數（`userDataRepository` 已存在）。

### 6. 首次訂閱不額外做 `drop(1)`

因為每個 UseCase 呼叫本身就是 `flatMapLatest` 的資料來源、不是額外的副作用，第一次 collect 時 `flatMapLatest` 本來就會呼叫一次（等同原本行為）。若誤加 `drop(1)`，會導致 ViewModel 剛建立時完全不載入資料，是需要避免的實作陷阱。

### 7. 不在畫面外的 ViewModel 做特殊「延遲刷新」處理

只要 NavEntry-scope ViewModel 還存在（在 backStack 中，包含目前不可見的畫面），語言變化時都會立即重新呼叫 API。這是為求實作簡單、且與現有 `StateFlow.WhileSubscribed(5_000)` 語意一致的決定：若某畫面已經超過 5 秒沒人訂閱，其 Flow 會被取消，等使用者重新導覽回去時才會用最新 `languageMode` 重新訂閱並重新載入，等同自然处理了「離開視野很久」的情境，不需額外程式碼。

### 8. `AppCompatDelegate.setApplicationLocales()` 新增為獨立方法，不接入正式流程

新增 `LanguageSettingUtils.setApplicationLocales(languageMode)`，供在 `MainActivity` 的 `remember(userData.languageMode)` 區塊手動切換測試比較，`androidApp/build.gradle.kts` 新增 `implementation(libs.androidx.appcompat)`。是否最終取代 `updateActivityLocale`，留待實測後另行決定（見 Open Questions）。

## Risks / Trade-offs

- **[Risk]** 每個相關 ViewModel 各自寫 `flatMapLatest`／`combine` 邏輯，未來新增畫面容易忘記接上語言監聽 → **Mitigation**：先以本次 4 個 ViewModel 驗證這個 pattern 是否足夠穩定；若日後出現第 5、6 個同類需求，再評估抽出共用小工具（例如 `Flow<UserData>.languageChanges()` extension），而非直接上 `BaseViewModel`
- **[Risk]** `SearchViewModel` 目前的巢狀 `flatMapLatest`（debounce → retryTrigger → UseCase）在加入 `languageMode` 後若沒有攤平，巢狀層級會更深、可讀性下降 → **Mitigation**：改用 `combine` 攤平成單一資料流再接一層 `flatMapLatest`（見 Decision 3）
- **[Risk]** 每次語言切換會對所有「存在中」的 NavEntry ViewModel（含目前不可見但還在 `WhileSubscribed(5_000)` 視窗內的）立即重新打 API，短時間內可能觸發多個並行網路請求 → **Mitigation**：這是刻意的簡化決定（見 Decision 7），影響範圍侷限在使用者剛好於 5 秒內來回切換畫面的情境，可接受
- **[Risk]** `MovieDetailViewModel` 的 `movieRecommendations`／`movieActors` 目前沒有任何 retry 機制，新增 `languageMode.flatMapLatest` 包裝屬於新行為，需確認測試涵蓋「語言變化時重新載入」且不影響 `retryMovieDetail()` 現有「只重試 movieDetail」的既定行為

## Migration Plan

本次為單一 Android App 內部行為修正，不涉及資料庫 schema 或需要分階段上線的部署策略。依 `tasks.md` 逐一調整 4 個 ViewModel，各自可獨立撰寫測試與提交，無需 feature flag 或特殊 rollback 步驟。

## Open Questions

- `AppCompatDelegate.setApplicationLocales()` 與現有 `updateActivityLocale` 之間，最終要保留哪一個（或兩者並存供 debug 切換）？本次僅新增測試用方法，尚未決定
- `feature/collect`／`feature/history` 未來若要支援「已收藏／已看過清單也顯示為目前語言」，是否要在 Repository 層新增語言感知的重新抓取機制？本次不處理，留待未來 change
