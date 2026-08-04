## Context

`MainActivity`（`androidApp/src/main/kotlin/com/shang/jetpackmoviekmp/ui/MainActivity.kt:76-105`）目前用 `LaunchedEffect(userData.languageMode)` 非同步呼叫 `LanguageSettingUtils.updateActivityLocale()`（手動 `resources.updateConfiguration()`），同時外層用 `key(languageMode) { ... }` 同步重建整個內容子樹（含 `rememberNavBackStack(HomeKey)`）。兩者沒有時序保證，且手動 `updateConfiguration()` 不會觸發 Compose 的 `LocalConfiguration` 更新或系統 `onConfigurationChanged`，導致字串/畫面不會可靠刷新；`key()` 又意外把 backstack 一併重置回首頁。

所有 ViewModel（`HomeContentViewModel`、`MovieDetailViewModel` 等）透過 `koinViewModel(key = "...")` 取得，因為 Navigation3 目前沒有安裝 per-entry `ViewModelStoreNavEntryDecorator`（`gradle/libs.versions.toml` 未引入 `lifecycle-viewmodel-navigation3`，`NavDisplay` 未傳 `entryDecorators`），所以全部落在 `MainActivity` 的 `ViewModelStore`。Android 對 configuration-change 式的重建（含顯式呼叫 `activity.recreate()`）會透過 `onRetainNonConfigurationInstance()` 保留 `ViewModelStore`，因此即使做 `recreate()`，已快取的 `StateFlow`／Paging 資料也不會自動失效重抓。

比對舊專案 `JetpackMovieCompose`（純 Android + 傳統 `androidx.navigation.compose.NavHost`）後確認：舊專案的 Locale 套用邏輯與本專案幾乎相同、同樣沒有解決字串刷新時序問題；但因為傳統 Navigation Compose 的 `hiltViewModel()` 預設 scope 到 `NavBackStackEntry`，而 `rememberNavController()` 恰好也被包在 `key(languageMode)` 內，語言切換時整個 `NavController` 連同所有 entry 的 ViewModelStore 一起被丟棄重建，「意外」讓 API 資料跟著刷新。這證實了資料過期問題的根因是 ViewModel 生命週期範圍，而非單純套用 Locale 的方式。

> **後續更新**：本 change 第一版曾採用 `activity.recreate()` 解決字串刷新問題（見決策 1 原方案），並在 `HomeContentViewModel`／`MovieDetailViewModel` 加入語言 reactive flow 解決資料過期問題（見決策 2 原方案），且已全數實作並通過驗證。但實機測試發現 `recreate()` 會導致語言切換後 Splash 畫面卡住無法消失，因此改採決策 1 目前記載的「同步套用 Locale ＋ 縮小範圍 `key(languageMode)`」方案；同時使用者決定收斂本次 change 範圍，資料過期問題（決策 2）先還原、延後處理。以下 Decisions 段落已反映此更新後的最終方案。

## Goals / Non-Goals

**Goals:**
- 語言切換後，系統字串與 Navigation3 畫面內容立即以新語言正確顯示。
- 語言切換後，使用者停留在原本所在的畫面（不強制跳回首頁）。
- 語言切換不會導致 Splash 畫面卡住無法消失。
- `androidApp`、`feature/collect`、`feature/history` 三個模組補齊英文字串資源。
- 移除 `shared/network` 未被實際使用的 `DefaultLanguageProvider` 選項，Koin 直接依賴 `DatastoreLanguageProvider`。

**Non-Goals:**
- 不導入 `AppCompatDelegate.setApplicationLocales()`／Android per-app language 官方機制（會需要改變 `MainActivity` 繼承關係或額外處理 pre-33 相容性，範圍超出本次 bug 修復）。
- 不導入 Navigation3 的 `ViewModelStoreNavEntryDecorator`（`lifecycle-viewmodel-navigation3`）。
- 不做全域 `viewModelStore.clear()`。會誤傷與語言無關的 Activity-scoped 暫存狀態（例如 `SearchViewModel` 的 `mutableSearchQuery` 沒有 `SavedStateHandle` 備份，切語言時若使用者正在搜尋情境會被無預警清空）。
- **不處理已顯示但資料來自 TMDB API（電影列表、詳情、演員、推薦）的內容語言相依重抓**（原本規劃在決策 2，已還原並移出本次範圍——見決策 2 說明）。語言切換後這些內容仍是切換前語言，需使用者手動下拉重試或重新進入畫面才會更新。
- 不處理 iOS 端語言切換（本次 change 僅涵蓋 Android）。

## Decisions

### 1.（已取代）字串／畫面刷新：改為同步套用 Locale ＋ 縮小範圍的 `key(languageMode)`，不再用 `activity.recreate()`

> 本節原方案為 `resources.updateConfiguration()` + `activity.recreate()`。上線後實測發現 `recreate()` 會導致語言切換後 Splash 畫面卡住無法消失，因此改採下述方案，不再呼叫 `recreate()`。

沿用既有 `LanguageSettingUtils.updateActivityLocale()`（不改動），但呼叫方式與重組範圍都調整：

- **同步套用 Locale**：`MainActivity.kt` 改用 `remember(userData.languageMode) { LanguageSettingUtils.updateActivityLocale(...) }`（而非 `LaunchedEffect`）。`updateActivityLocale()` 本身是同步函式，不需要 coroutine；用 `remember` 讓套用動作在同一次 composition 中、且保證早於下方 `key(languageMode)` 觸發的重組執行，解決了原方案「`LaunchedEffect` 非同步套用與 `key()` 重組沒有時序保證」的問題（`LaunchedEffect` 的 effect 在 commit 之後才執行，重組當下讀到的可能還是套用前的舊字串）。
- **`key(languageMode)` 範圍縮小到只包住畫面內容**：只包住 `MainScreen`（即 `ThemeProvider` 的 `content` lambda），`rememberNavBackStack(HomeKey)` 留在 `key()` 外面、只呼叫一次。語言改變時只有畫面內容子樹被丟棄重建（觸發 `stringResource()` 重新讀取新語言字串），`backStack` 這個物件本身不受影響，因此不會被重置回首頁。
- **不再需要 `activity.recreate()`**：`recreate()` 是為了確保 Compose 的 `LocalConfiguration`／`stringResource()` 正確反映新語言，但實測發現重建 Activity 後 Splash 的 `setKeepOnScreenCondition` 有機率卡住不消失；上述「同步套用 + 局部 `key()` 重組」已能達成同樣的字串刷新效果，且風險更小、對使用者體感更輕（只重組畫面內容，不是整個 Activity 重建閃爍）。
- **為什麼不用 `AppCompatDelegate.setApplicationLocales()`**：`MainActivity` 目前繼承 `ComponentActivity`。此 API 在 API 33+ 由系統 `LocaleManager` 原生處理 recreate，但 API < 33 的自動 recreate backport 是靠 `AppCompatActivity` 的 lifecycle callback 達成，若不改繼承關係仍需自己手動處理——與本方案改動幅度相當，卻多引入新依賴與 locale 持久化策略需要設計，超出本次 bug-fix 範圍。

### 2.（已還原）API 資料重抓：語言改變不再自動重新查詢 API 資料

> 本節原方案是在 `HomeContentViewModel`／`MovieDetailViewModel` 內加入語言 reactive flow，語言改變時自動重新查詢 API 資料。上線後與決策 1 的 `recreate()` 問題一併重新檢視時，使用者決定先收斂本次 change 的範圍：只解決「Splash 卡住」與「字串/畫面正確刷新＋停留原地」，API 資料語言相依重抓的需求延後、不在本次 change 範圍內。因此 `HomeContentViewModel`／`MovieDetailViewModel` 已還原回建構時一次性查詢，不再觀察 `userData.languageMode`。

還原後的行為：

- `HomeContentViewModel`：`movieList` 回到 `getMovieGenreUseCase(movieGenre.id.toString(), viewModelScope)`，建構時查詢一次，語言改變不會重建 `Pager`。
- `MovieDetailViewModel`：`movieDetail`／`movieRecommendations`／`movieActors` 回到只綁 `retryTrigger`（或建構時一次性 `stateIn`），語言改變不會觸發重新查詢；既有的手動 `retryMovieDetail()` 行為不受影響。
- `HomeModule.kt`／`DetailModule.kt` 也移除了對應的 `userDataRepository` 注入參數。

**為什麼不用全域 `viewModelStore.clear()`**：會清除所有 Activity-scoped ViewModel，包含與語言無關的暫存狀態。已查證 `SearchViewModel.kt:31` 的 `mutableSearchQuery` 是純記憶體、無 `SavedStateHandle` 備份，若使用者在搜尋情境中切語言，搜尋字串與結果會被無預警清空。

**代價（刻意接受）**：語言切換後，已顯示的電影列表／詳情／推薦／演員資料不會自動以新語言重新查詢，使用者需手動下拉重試或重新進入畫面才會看到新語言的 API 資料。若之後要補上這個能力，需要另開新的 change 依照決策 2 原本的 reactive flow 模式（`flatMapLatest`／`merge()`／`stateIn`）逐一補上，不在本次範圍內。

### 3. 移除 `DefaultLanguageProvider` 與 `provideDefaultLanguageProvider` 參數

已查證：production（`InitKoin.kt:43`）與所有測試模組（`DataModuleTest.kt`、`DomainModuleTest.kt`）皆已明確傳入 `provideDefaultLanguageProvider = false`，且都同時安裝 `datastoreModule` 提供 `DatastoreLanguageProvider`；`DefaultLanguageProvider` 只有自己的單元測試在用。移除是安全的死碼清理：刪除 `DefaultLanguageProvider.kt`、`networkModule()` 的參數、`DefaultLanguageProviderTest.kt`，並同步移除呼叫端的 `provideDefaultLanguageProvider = false` 引數。`LanguageMode.SYSTEM_DEFAULT` 的 `toLanguageCode()` 轉換邏輯（`DatastoreLanguageProvider.kt:42-46`）不變。

### 4. 遵循既有模式

- `MainActivity` 的 Locale 套用與畫面重組沿用 Compose 既有的 `remember`／`key` 慣例，不引入新的狀態管理抽象。
- 不偏離 Repository / UseCase / Koin module 分層；不改動 `shared/domain` 的 UseCase 簽名。

## Risks / Trade-offs

- **[Risk]** `key(languageMode)` 只包住畫面內容子樹，語言切換時該子樹會被丟棄重建，子樹內非 ViewModel 承載的 `remember` 狀態（例如捲動位置、未儲存的暫時性 UI 狀態）可能被重置。→ **Mitigation**：語言切換是使用者主動觸發的低頻操作，且範圍已限縮到只影響目前可見畫面的內容子樹（不含 backStack、不含 ViewModel），影響程度遠小於原本 `activity.recreate()` 整個 Activity 重建；目前沒有觀察到需要跨語言切換保留的畫面內部暫時狀態。
- **[Risk]** 語言切換後，已顯示但資料來自 TMDB API（電影列表、詳情、演員、推薦）的內容不會自動以新語言重新查詢（決策 2 已還原）。→ **Mitigation**：已知取捨，見 Non-Goals；使用者需手動下拉重試或重新進入畫面才會看到新語言資料。若後續要補上，需另開新 change。
- **[Risk]** `remember(userData.languageMode) { LanguageSettingUtils.updateActivityLocale(...) }` 這種「用 `remember` 執行同步 side effect」的寫法不是 Compose 官方建議的慣用模式（官方建議 side effect 一律走 `LaunchedEffect`／`SideEffect`），可能在 code review 時引發疑問。→ **Mitigation**：`updateActivityLocale()` 本身是同步、非 suspend 函式，且必須保證早於下方 `key(languageMode)` 重組執行才能修正原本的字串刷新時序問題；`LaunchedEffect` 的 effect 在 commit 之後才執行，無法滿足這個時序要求。已在程式碼加上註解說明原因。
- 不涉及資料庫 schema 變更，無 Room migration 需求。

## Migration Plan

不涉及資料遷移或部署順序考量，純程式碼修正；一次性 PR／commit 完成即可上線，無需分階段 rollout。若上線後發現 `key(languageMode)` 局部重組造成非預期的畫面狀態遺失問題，可透過還原 commit 回滾。

## Open Questions

無。
