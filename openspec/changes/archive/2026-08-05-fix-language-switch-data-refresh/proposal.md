## Why

語言切換後，畫面靜態文字（底部導覽列、標題等）已可即時刷新（見已歸檔的 `fix-android-language-switch-refresh`），但各 feature ViewModel 內部持有、直接來自 TMDB API 的遠端資料（如 Home 的分類片單、Search 的搜尋結果、Detail 的電影詳情／演員／推薦）不會跟著更新。原因是 Navigation3 的 NavEntry-scope ViewModel 透過 `movableContentOf` 被刻意保留，切換語言不會重建 ViewModel，也沒有任何機制監聽語言變化去重新呼叫 API。使用者切換語言後，需要手動離開再重新進入畫面才能看到新語言內容，體驗不一致。

## What Changes

- `HomeViewModel`、`HomeContentViewModel`、`SearchViewModel`、`MovieDetailViewModel` 各自訂閱 `UserDataRepository.userData` 的 `languageMode` 變化（`distinctUntilChanged()`），變化時觸發對應資料重新載入
- 分頁資料（`HomeContentViewModel.movieList`、`SearchViewModel.movieSearchPager`）透過 `flatMapLatest` 讓 `languageMode` 變化重新建立底層 `Pager.flow`，從 page 1 重新載入，捨棄舊語言的分頁快取（沿用 `SearchViewModel` 現有的 `flatMapLatest` 慣例）
- 一次性資料（`MovieDetailViewModel.movieDetail`／`movieRecommendations`／`movieActors`）比照現有 `retryTrigger` 模式，改為同時對 `languageMode` 變化與既有 retry 觸發做 `flatMapLatest`，重新呼叫對應 UseCase
- 不新增 Navigation3 生命週期機制、不改變現有 NavEntry-scope ViewModel 的建立／銷毀時機，維持使用者導覽深度與 back stack 不因語言切換而被重置（`MainActivity` 現有的 `key(userData.languageMode)` + `movableContentOf` 搭配方式維持不變）
- `androidApp` 新增 `LanguageSettingUtils.setApplicationLocales()`（使用 `AppCompatDelegate.setApplicationLocales`），作為切換語言的替代實作，供在 `MainActivity` 的 `remember(userData.languageMode)` 區塊中切換測試比較；現有 `updateActivityLocale` 同步呼叫與 `key(languageMode)` 搭配時序維持不變，此方法暫不視為正式行為

## Capabilities

### New Capabilities
- `android-content-language-refresh`：定義顯示 TMDB 遠端內容的 Android ViewModel（Home、Search、Detail）於使用者切換語言時，MUST 主動重新載入資料（含分頁重置），且 MUST NOT 影響既有導覽 back stack 深度或觸發 ViewModel 重建

### Modified Capabilities

（無——本次不變更既有模組已驗收的 Requirement，僅新增跨模組的語言刷新行為）

## Impact

- `feature/home`（Android）：`HomeViewModel.kt`（`movieGenres` 需一併對語言變化重新載入）、`HomeContentViewModel.kt`（新增注入 `UserDataRepository`，`movieList` 分頁需對語言變化重置）
- `feature/search`（Android）：`SearchViewModel.kt`（新增注入 `UserDataRepository`，`movieSearchPager` 需對語言變化重置）
- `feature/detail`（Android）：`MovieDetailViewModel.kt`（新增注入 `UserDataRepository`，`movieDetail`／`movieRecommendations`／`movieActors` 需對語言變化重新載入）
- `androidApp`：`utils/LanguageSettingUtils.kt`（新增 `setApplicationLocales()`）、`build.gradle.kts`（新增 `implementation(libs.androidx.appcompat)`，alias 已存在於 `gradle/libs.versions.toml`，`feature/home`、`feature/collect`、`feature/search`、`feature/history`、`feature/setting` 已使用同一 alias）
- **明確排除於本次範圍**：`feature/collect`、`feature/history` 的資料來源是本地 Room 資料庫（`MovieRepository.getAllMovieCollect()`／`GetHistoryMovieListUseCase()`），並非依目前語言即時向 TMDB 重新請求，切語言不會改變這兩個畫面顯示的內容，本次不處理；若未來需要「已收藏／已看過的片單也要跟著顯示語言重新翻譯」，需另開 change 處理（涉及本地快取重新抓取的更大範圍調整）
- 不影響 iOS（`iosApp`）、不變更 `shared/data`／`shared/domain` 既有公開 API 介面
