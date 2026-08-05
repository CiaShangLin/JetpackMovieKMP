## 1. feature/home

- [ ] 1.1 `HomeViewModel.kt`：把既有 `_refreshTrigger.flatMapLatest { movieRepository.getMovieGenres() }` 改為 `combine(_refreshTrigger, userDataRepository.userData.map { it.languageMode }.distinctUntilChanged()) { _, _ -> Unit }.flatMapLatest { ... }`（`userDataRepository` 已注入，不需新增建構參數）
- [ ] 1.2 `HomeContentViewModel.kt`：新增建構子參數注入 `UserDataRepository`，把 `getMovieGenreUseCase(movieGenre.id.toString(), viewModelScope)` 包進 `userDataRepository.userData.map { it.languageMode }.distinctUntilChanged().flatMapLatest { getMovieGenreUseCase(...) }`
- [ ] 1.3 更新 `di/HomeModule.kt`（或對應 Koin module）：`HomeContentViewModel` 的參數化 `viewModel { params -> }` 補上 `UserDataRepository` 注入
- [ ] 1.4 `HomeViewModelTest.kt`：新增測試「languageMode 變化時重新呼叫 getMovieGenres」與「僅 themeMode 變化不觸發重新載入」
- [ ] 1.5 `HomeContentViewModelTest.kt`：新增測試「languageMode 變化時 movieList 以新語言從第一頁重新載入，不沿用舊分頁快取」
- [ ] 1.6 執行 `./gradlew :feature:home:testDebugUnitTest`（或本模組實際使用的 test task）確認全數通過

## 2. feature/search

- [ ] 2.1 `SearchViewModel.kt`：新增建構子參數注入 `UserDataRepository`，把 `movieSearchPager` 的巢狀 `flatMapLatest`（debounce → retryTrigger → UseCase）改為 `combine(debounce 後的 searchQuery, retryTrigger, languageMode) { query, _, _ -> query }.flatMapLatest { ... }` 攤平寫法，維持「空白關鍵字回傳 `PagingData.empty()` 且不呼叫 UseCase」的既有行為
- [ ] 2.2 更新 `di/SearchModule.kt`（或對應 Koin module）：補上 `UserDataRepository` 注入
- [ ] 2.3 `SearchViewModelTest.kt`：新增測試「已有搜尋關鍵字時 languageMode 變化，以相同關鍵字重新從第一頁呼叫 getSearchMovieListUseCase」與「尚未輸入關鍵字時 languageMode 變化，MUST NOT 呼叫 getSearchMovieListUseCase」
- [ ] 2.4 執行 `./gradlew :feature:search:testDebugUnitTest`（或本模組實際使用的 test task）確認全數通過

## 3. feature/detail

- [ ] 3.1 `MovieDetailViewModel.kt`：新增建構子參數注入 `UserDataRepository`，把 `movieDetail` 的 `retryTrigger.flatMapLatest { getMovieDetailUseCase(movieId) }` 改為 `combine(retryTrigger, languageMode) { _, _ -> Unit }.flatMapLatest { ... }`
- [ ] 3.2 `MovieDetailViewModel.kt`：`movieRecommendations` 改為 `languageMode.flatMapLatest { getMovieRecommendUseCase(movieId) }.map { ... }`
- [ ] 3.3 `MovieDetailViewModel.kt`：`movieActors` 改為 `languageMode.flatMapLatest { movieRepository.getMovieActor(movieId) }.map { ... }`
- [ ] 3.4 更新 `di/DetailModule.kt`（或對應 Koin module）：`MovieDetailViewModel` 的參數化 `viewModel { params -> }` 補上 `UserDataRepository` 注入
- [ ] 3.5 `MovieDetailViewModelTest.kt`：新增測試「languageMode 變化時 movieDetail／movieRecommendations／movieActors 分別重新呼叫對應 UseCase」，並補一個測試確認 `retryMovieDetail()` 的既有範圍（僅重試 movieDetail）不受影響
- [ ] 3.6 執行 `./gradlew :feature:detail:testDebugUnitTest`（或本模組實際使用的 test task）確認全數通過

## 4. androidApp（AppCompatDelegate 測試方法）

- [ ] 4.1 `androidApp/build.gradle.kts` 新增 `implementation(libs.androidx.appcompat)`（alias 已存在於 `gradle/libs.versions.toml`）
- [ ] 4.2 `LanguageSettingUtils.kt` 新增 `setApplicationLocales(languageMode: LanguageMode)`，使用 `AppCompatDelegate.setApplicationLocales()`，`SYSTEM_DEFAULT` 對應 `LocaleListCompat.getEmptyLocaleList()`
- [ ] 4.3 於 `MainActivity.kt` 的 `remember(userData.languageMode) { ... }` 區塊註記兩種呼叫方式的切換點（供手動測試比較，暫不變更預設呼叫 `updateActivityLocale` 的行為）

## 5. 跨模組驗證

- [ ] 5.1 執行 `./gradlew ktlintCheck` 確認格式通過
- [ ] 5.2 執行 `./gradlew :androidApp:assembleDebug` 確認整體可建置
- [ ] 5.3 手動驗證：在實機／模擬器上依序切換語言，確認 Home 分類清單與片單、Search 結果（已輸入關鍵字時）、Detail 詳情／推薦／演員皆以新語言重新載入，且 back stack 深度與 scroll 狀態未被重置
- [ ] 5.4 執行 `openspec validate fix-language-switch-data-refresh --type change --strict --no-interactive` 確認 change 產物通過驗證
