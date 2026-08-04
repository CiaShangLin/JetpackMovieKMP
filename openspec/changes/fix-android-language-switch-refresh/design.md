## Context

`MainActivity`（`androidApp/src/main/kotlin/com/shang/jetpackmoviekmp/ui/MainActivity.kt:76-105`）目前用 `LaunchedEffect(userData.languageMode)` 非同步呼叫 `LanguageSettingUtils.updateActivityLocale()`（手動 `resources.updateConfiguration()`），同時外層用 `key(languageMode) { ... }` 同步重建整個內容子樹（含 `rememberNavBackStack(HomeKey)`）。兩者沒有時序保證，且手動 `updateConfiguration()` 不會觸發 Compose 的 `LocalConfiguration` 更新或系統 `onConfigurationChanged`，導致字串/畫面不會可靠刷新；`key()` 又意外把 backstack 一併重置回首頁。

所有 ViewModel（`HomeContentViewModel`、`MovieDetailViewModel` 等）透過 `koinViewModel(key = "...")` 取得，因為 Navigation3 目前沒有安裝 per-entry `ViewModelStoreNavEntryDecorator`（`gradle/libs.versions.toml` 未引入 `lifecycle-viewmodel-navigation3`，`NavDisplay` 未傳 `entryDecorators`），所以全部落在 `MainActivity` 的 `ViewModelStore`。Android 對 configuration-change 式的重建（含顯式呼叫 `activity.recreate()`）會透過 `onRetainNonConfigurationInstance()` 保留 `ViewModelStore`，因此即使做 `recreate()`，已快取的 `StateFlow`／Paging 資料也不會自動失效重抓。

比對舊專案 `JetpackMovieCompose`（純 Android + 傳統 `androidx.navigation.compose.NavHost`）後確認：舊專案的 Locale 套用邏輯與本專案幾乎相同、同樣沒有解決字串刷新時序問題；但因為傳統 Navigation Compose 的 `hiltViewModel()` 預設 scope 到 `NavBackStackEntry`，而 `rememberNavController()` 恰好也被包在 `key(languageMode)` 內，語言切換時整個 `NavController` 連同所有 entry 的 ViewModelStore 一起被丟棄重建，「意外」讓 API 資料跟著刷新。這證實了資料過期問題的根因是 ViewModel 生命週期範圍，而非單純套用 Locale 的方式。

## Goals / Non-Goals

**Goals:**
- 語言切換後，系統字串與 Navigation3 畫面內容立即以新語言正確顯示。
- 語言切換後，使用者停留在原本所在的畫面（不強制跳回首頁）。
- 語言切換後，已顯示但資料來自 TMDB API（電影列表、詳情、演員、推薦）的內容會自動以新語言重新查詢，不需要使用者手動下拉重試。
- `androidApp`、`feature/collect`、`feature/history` 三個模組補齊英文字串資源。
- 移除 `shared/network` 未被實際使用的 `DefaultLanguageProvider` 選項，Koin 直接依賴 `DatastoreLanguageProvider`。

**Non-Goals:**
- 不導入 `AppCompatDelegate.setApplicationLocales()`／Android per-app language 官方機制（會需要改變 `MainActivity` 繼承關係或額外處理 pre-33 相容性，範圍超出本次 bug 修復）。
- 不導入 Navigation3 的 `ViewModelStoreNavEntryDecorator`（`lifecycle-viewmodel-navigation3`）。因為本次決定「切語言後停留原地、不重置 backstack」，per-entry ViewModelStore 在沒有 entry 被移除的情況下同樣會被 `recreate()` 保留，無法解決資料過期問題，加了也沒有實際效果。
- 不做全域 `viewModelStore.clear()`。會誤傷與語言無關的 Activity-scoped 暫存狀態（例如 `SearchViewModel` 的 `mutableSearchQuery` 沒有 `SavedStateHandle` 備份，切語言時若使用者正在搜尋情境會被無預警清空）。
- 不處理 iOS 端語言切換（本次 change 僅涵蓋 Android）。

## Decisions

### 1. 字串／畫面刷新：`resources.updateConfiguration()` + `activity.recreate()`，取代 `key(languageMode)`

沿用既有 `LanguageSettingUtils.updateActivityLocale()`（不改動），但在 `MainActivity.kt` 的 `LaunchedEffect(userData.languageMode)` 內，套用完成後立即呼叫 `activity.recreate()`；移除包住 `rememberNavBackStack(HomeKey)` 的 `key(languageMode)`。

- **為什麼不用 `AppCompatDelegate.setApplicationLocales()`**：`MainActivity` 目前繼承 `ComponentActivity`。此 API 在 API 33+ 由系統 `LocaleManager` 原生處理 recreate，但 API < 33 的自動 recreate backport 是靠 `AppCompatActivity` 的 lifecycle callback 達成，若不改繼承關係仍需自己手動 `recreate()`——與方案二改動幅度相當，卻多引入新依賴與 locale 持久化策略需要設計，超出本次 bug-fix 範圍。
- **為什麼移除 `key(languageMode)`**：`rememberNavBackStack(HomeKey)` 目前被包在裡面，導致每次切語言都把 backstack 重置回首頁，這是非預期的副作用。Navigation3 的 `rememberNavBackStack` 本身有序列化保存機制（類似 `rememberSaveable`），`recreate()` 後會自動還原原有 backstack 內容，不需要 `key()` 這種強制重建手段。
- **`recreate()` 只解決字串/畫面刷新，不解決資料過期**：因為 Android 對 `recreate()` 的處理方式與畫面旋轉造成的 configuration change 走同一條路徑，`ViewModelStore` 會被保留給重建後的新 Activity 實例，這是 `ViewModel` 元件設計的本意（避免組態變更時遺失資料），因此需要另外的機制處理資料重抓（見決策 2）。

### 2. API 資料重抓：ViewModel 內部 reactive flow，取代全域清除或 nav 架構調整

在 `HomeContentViewModel` 與 `MovieDetailViewModel` 內，把語言狀態（`userDataRepository.userData.map { it.languageMode }.distinctUntilChanged()`）併入既有資料流：

- `HomeContentViewModel`：目前建構時呼叫一次 `getMovieGenreUseCase(...)` 並 `cachedIn(viewModelScope)`。改為語言 flow 觸發 `flatMapLatest` 重新呼叫 use case（重建新 `Pager`），外層再 `cachedIn(viewModelScope)`。`flatMapLatest` 確保語言改變時舊 Pager 被取消，不會有競態或資源浪費。
- `MovieDetailViewModel`：既有 `retryTrigger`（`MovieDetailViewModel.kt:39-51`）已用於 `movieDetail` 的手動 retry。比照同樣模式，把語言改變事件與既有 `retryTrigger` 用 `merge()` 合成同一個 trigger flow；`movieRecommendations`／`movieActors` 目前是建構時一次性 `stateIn`，改為訂閱合成後的 trigger flow 再 `flatMapLatest`／`stateIn`。

**為什麼不用全域 `viewModelStore.clear()`**：雖然改動範圍最小（只動 `MainActivity` 一處），但會清除所有 Activity-scoped ViewModel，包含與語言無關的暫存狀態。已查證 `SearchViewModel.kt:31` 的 `mutableSearchQuery` 是純記憶體、無 `SavedStateHandle` 備份，若使用者在搜尋情境中切語言，搜尋字串與結果會被無預警清空。

**為什麼不用「backstack 重置回 Home/Setting + 補上 nav3 entry decorator」**：這個方向技術上可行（且更貼近舊專案 `JetpackMovieCompose` 靠 NavController 重建「意外」達到的效果），但代價是使用者切語言後會被導回固定畫面，不能停留原地。經與使用者確認，本次選擇「停留原地」的 UX，因此 per-entry ViewModelStore decorator 沒有實際效果（見 Non-Goals），改用 ViewModel 內部 reactive flow 這個不影響導覽位置、也不影響其他畫面狀態的方案。

**代價**：只有 `feature/home`、`feature/detail` 這兩個已知有語言相依 API 資料的模組被處理；未來若新增其他有語言相依資料的畫面，需要比照這個模式個別加上 reactive flow，沒有全域自動涵蓋的機制（相對於 `viewModelStore.clear()` 或 per-entry decorator 方案）。此取捨在 Non-Goals 已說明是刻意選擇。

### 3. 移除 `DefaultLanguageProvider` 與 `provideDefaultLanguageProvider` 參數

已查證：production（`InitKoin.kt:43`）與所有測試模組（`DataModuleTest.kt`、`DomainModuleTest.kt`）皆已明確傳入 `provideDefaultLanguageProvider = false`，且都同時安裝 `datastoreModule` 提供 `DatastoreLanguageProvider`；`DefaultLanguageProvider` 只有自己的單元測試在用。移除是安全的死碼清理：刪除 `DefaultLanguageProvider.kt`、`networkModule()` 的參數、`DefaultLanguageProviderTest.kt`，並同步移除呼叫端的 `provideDefaultLanguageProvider = false` 引數。`LanguageMode.SYSTEM_DEFAULT` 的 `toLanguageCode()` 轉換邏輯（`DatastoreLanguageProvider.kt:42-46`）不變。

### 4. 遵循既有模式

- ViewModel 資料流沿用專案既有的 `flatMapLatest`／`retryTrigger`／`merge()`／`stateIn` 慣例（`MovieDetailViewModel.kt` 已有先例），不引入新的狀態管理抽象。
- 不偏離 Repository / UseCase / Koin module 分層；不改動 `shared/domain` 的 UseCase 簽名，語言 reactive 邏輯留在 ViewModel 層（UI 層關注點），Repository／UseCase 保持純粹的一次性查詢語意。

## Risks / Trade-offs

- **[Risk]** `activity.recreate()` 會讓使用者短暫看到畫面閃爍（黑屏或重繪）。→ **Mitigation**：這是預期中、使用者主動觸發語言切換這種低頻操作的合理代價；沒有比 `recreate()` 更輕量、又能保證 Compose `LocalConfiguration` 正確更新的做法。
- **[Risk]** `LaunchedEffect(userData.languageMode)` 在新 Activity 實例的初次 composition 一定會執行一次，但如果套用 Locale 的時機晚於畫面首次繪製，可能有極短暫的舊語言閃爍。→ **Mitigation**：影響範圍小（僅限 recreate 後的極短暫時間），沿用既有 `updateActivityLocale()` 實作，不在本次範圍內進一步優化。
- **[Risk]** Home／Detail 以外、未來新增的語言相依資料畫面，若沒有比照加上 reactive flow，會重現同樣的資料過期 bug。→ **Mitigation**：本次決策已知的取捨（見決策 2），建議後續補上一份開發規範或 checklist 提醒「新增語言相依 API 資料的 ViewModel 時需加上語言 reactive 邏輯」，但此項不在本次 change 範圍內。
- **[Risk]** `distinctUntilChanged()` 依賴 `LanguageMode` 的 `equals()`／`hashCode()`，若未來 `UserData`／`LanguageMode` 改為包含其他易變欄位的資料類別，可能誤判導致不必要的重抓或漏抓。→ **Mitigation**：`LanguageMode` 目前是純 enum（`shared/model/.../LanguageMode.kt`），無此風險；若未來型別變更需重新檢視。
- 不涉及資料庫 schema 變更，無 Room migration 需求。

## Migration Plan

不涉及資料遷移或部署順序考量，純程式碼修正；一次性 PR／commit 完成即可上線，無需分階段 rollout。若上線後發現 `recreate()` 造成的畫面閃爍或極端裝置上的相容性問題，可透過還原 commit 回滾。

## Open Questions

無。
