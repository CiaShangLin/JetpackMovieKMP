## 1. shared/domain

- [x] 1.1 完成 `GetSearchMovieListUseCase`，合併搜尋 Pager 與收藏電影 id，並以 `Set` 標記每筆 `MovieCardResult.isCollect`。
- [x] 1.2 將 `GetSearchMovieListUseCase` 註冊至 `domainModule()`，維持既有 Dispatcher 與 Koin 注入慣例。
- [x] 1.3 為搜尋 UseCase 補齊 `commonTest`，驗證初始收藏狀態及收藏資料流變更後的更新行為。

## 2. feature/search

- [x] 2.1 調整 `SearchViewModel` 改由 `GetSearchMovieListUseCase` 建立搜尋 Pager，並保留 query、debounce、retry 與 cachedIn 行為。
- [x] 2.2 在 `SearchViewModel` 實作收藏切換：未收藏時新增、已收藏時刪除，並使用既有資料 model mapper。
- [x] 2.3 將 `SearchScreen`、搜尋結果卡片與 `SearchNavigation` 串接 `onMovieClick`、`onCollectClick` callback。
- [x] 2.4 擴充 SearchViewModel JVM 單元測試，驗證 UseCase 呼叫、收藏新增與刪除，以及收藏狀態更新行為；測試採 AAA 結構。

## 3. androidApp

- [x] 3.1 將搜尋 Navigation3 entry 的 `onMovieClick` callback 串接至 `SearchScreen`；電影詳情目的地不在本 change 範圍內，並確認不影響收藏與歷史 entry。

## 4. 驗證

- [x] 4.1 執行 `./gradlew :shared:domain:testAndroidHostTest :feature:search:testDebugUnitTest`，修正本次變更造成的測試或編譯問題。
- [x] 4.2 執行 `./gradlew ktlintCheck`，確認本次 Kotlin 與 Gradle 變更符合格式規範。
