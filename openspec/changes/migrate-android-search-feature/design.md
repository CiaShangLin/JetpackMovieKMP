## Context

目前 `shared:data` 已以 `MovieRepository.getMovieSearchPager(query)` 暴露 TMDB 搜尋的 `PagingData<MovieCardResult>`，但 Android App 尚無 `feature/search` 與 Navigation3 entry，且 `MainNavItem` 的搜尋項目仍被註解。來源專案的搜尋功能使用 Hilt、字串路由與舊 package；目標專案則採 Gradle version catalog、Koin、Navigation3、Material 3 與 `core/ui` 共用元件。使用者已限定本 change 只處理 Android。

## Goals / Non-Goals

**Goals:**

- 以現有 feature 模組結構新增 Android 搜尋頁與可測試的 ViewModel。
- 讓使用者可由底部導覽進入搜尋頁，提交非空關鍵字後取得分頁電影結果。
- 顯示未搜尋、refresh 載入、refresh 錯誤與 append 載入／錯誤／資料結尾等狀態，並提供合理的重試行為。
- 延續現有的本地化資源、`MovieCard`、`JMLazyVerticalGrid` 與 Koin DI 模式。

**Non-Goals:**

- 不新增 iOS 搜尋 UI、shared presenter 或 Swift 測試。
- 不修改 `shared:data`、`shared:network`、TMDB endpoint、Room schema 或共用 Repository 合約。
- 不導入電影詳情導覽、搜尋歷史／建議、篩選、收藏同步或新外部依賴；這些不在來源 search 模組的遷移範圍內。

## Decisions

### 以獨立 `feature/search` Android library 承載功能

新增與 `feature/home`／`feature/collect`／`feature/history` 對齊的 Android library，包含 `di`、`navigation`、`ui`、資源與 JVM ViewModel 測試；在 `settings.gradle.kts` 和 `androidApp` 宣告依賴。這保留 feature 邊界，且避免把畫面邏輯加入 `androidApp`。

替代方案是將畫面直接放入 `androidApp`；因會破壞既有 feature 模組模式並降低可測試性，故不採用。

### 遵循 MVVM + Repository，ViewModel 只編排搜尋狀態

`SearchViewModel` 透過 Koin 注入現有 `MovieRepository`，以已提交的 query state 與 retry trigger 衍生搜尋 pager；非空 query 才呼叫 `getMovieSearchPager`，並在 `viewModelScope` 使用 `cachedIn`。輸入欄位的暫存文字維持在 Compose 畫面，使用者以 IME Search 提交時才更新 ViewModel 並隱藏鍵盤，與來源行為一致。

這是既有 MVVM／Repository 架構的直接延伸，無需為單一 Android UI 新增 UseCase。替代方案是建立搜尋 UseCase 或讓 Composable 直接呼叫 Repository；前者只會增加無商業規則的包裝，後者會違反現有分層，因此均不採用。

### 以 Navigation3 與 `MainNavItem` 接入底部導覽

定義可序列化的 `SearchKey : NavKey` 與 `searchEntry()`，由 `MainActivity` 的 `NavDisplay` 提供 entry，並在 `MainNavItem` 啟用 Search icon 與本地化標籤。底部導覽切換仍沿用目前移除最後一個 entry 後加入目的地的行為。

替代方案是恢復舊字串路由／NavController；目標專案已使用 Navigation3，故不採用。

### 使用既有 Compose UI 元件並明確處理 Paging 狀態

搜尋頁以 Material 3 `TextField` 和 `JMLazyVerticalGrid` 呈現，結果項目重用 `MovieCard`。初始狀態顯示搜尋提示；refresh loading/error 分別使用 `LoadingScreen`／`ErrorScreen`，錯誤重試會重建目前 query 的 pager；append loading/error/end-of-pagination 在清單尾端呈現，append 錯誤重試呼叫 Paging 的 `retry()`。文字全部放入 feature 專屬的預設、繁中與英文 resources。

替代方案是使用目前的 `MovieListPagerScreen`；它沒有區分初始提示與列表尾端狀態，無法完整重現來源功能，故由 search 畫面自行編排。

## Risks / Trade-offs

- [來源程式使用 Hilt／舊 Navigation API] → 以現有 feature module 作為結構範本，並以 Koin module 與 Navigation3 entry 取代。
- [query 變更可能造成舊 paging stream 仍在載入] → 使用 `flatMapLatest`／`cachedIn(viewModelScope)`，讓新提交 query 取消舊 stream。
- [搜尋結果不含收藏狀態同步] → 延續來源模組的純搜尋範圍；日後若要支援，另提 shared 層與 UI 狀態變更。
- [無 Room schema 變更] → 不需要 Room migration。

## Migration Plan

1. 新增 feature/search 並完成 Koin、Navigation3、資源與測試。
2. 將模組接入 settings 與 androidApp，啟用底部搜尋入口。
3. 執行 feature/search 單元測試、`./gradlew ktlintCheck` 與 `./gradlew :androidApp:assembleDebug`。
4. 若需回退，移除 androidApp 的 Search module/entry/nav item 與 feature/search 模組，不影響 shared 或 iOS 功能。

## Open Questions

- 無；本次以來源 search 模組的提交搜尋與結果瀏覽行為為準，並明確排除 iOS 與詳情導覽。
