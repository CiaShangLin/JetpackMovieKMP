## Why

Android 首頁（`feature/home` 的 `HomeScreen`）持續滑動分頁載入時，偶爾會拋出
`java.lang.IllegalArgumentException: Key "22" was already used`，導致 App 崩潰。
根因是 TMDB `discover` API 依人氣等排序在多次請求間會即時變動，導致同一部電影
可能同時出現在已載入的第 N 頁與新載入的第 N+1 頁；`shared/data` 的
`MovieGenrePagingSource` 目前直接回傳每頁的原始結果、不會跨頁去重，使
`LazyPagingItems` 中出現重複 `id`，而 Compose 的 `itemKey { it.id }` 對重複 key
沒有任何容錯、直接崩潰。`MovieSearchPagingSource` 的分頁邏輯與此完全相同、
同樣依賴伺服器排序，存在相同風險。此問題目前只在 Android 出現可觀察的崩潰
（Compose 對 key 唯一性做強制檢查），但成因在跨平台共用的 `shared/data`
分頁層，若不修正、日後 iOS 端的清單渲染邏輯也可能因同一批重複資料出現
UI 異常。

## What Changes

- 在 `shared/data` 的分頁層新增「跨頁 id 去重」保護，確保單一 `PagingSource`
  實例在其生命週期內（一次完整的分頁載入到下次 `invalidate`/refresh 之間）
  不會對外送出重複 `id` 的 `MovieCardResult`。
- `MovieGenrePagingSource`（`discover` 分頁）套用去重邏輯，修正本次回報的
  首頁崩潰。
- `MovieSearchPagingSource`（搜尋分頁）套用相同去重邏輯，避免相同成因在
  搜尋頁重現（`feature/search` 的 `SearchScreen` 也使用 `itemKey { it.id }`）。
- 不更動 `feature/home`／`feature/search` 的 Compose UI 層 `itemKey` 寫法——
  去重在資料源頭完成，UI 層維持現有以 `id` 作為 key 的慣例即可。

## Capabilities

### New Capabilities

（無）

### Modified Capabilities

- `kmp-movie-data-repository`：「依類型分頁載入電影列表」與「依關鍵字分頁搜尋
  電影」兩個 Requirement 新增跨頁去重保證，`Flow<PagingData<MovieCardResult>>`
  emit 的分頁資料在同一次分頁 session 中 MUST NOT 出現重複 `id`。

## Impact

- `shared/data`（commonMain，跨 Android／iOS 平台共用）：
  - `src/commonMain/kotlin/com/shang/jetpackmoviekmp/data/paging/MovieGenrePagingSource.kt`
    ——新增跨頁已載入 id 追蹤與過濾。
  - `src/commonMain/kotlin/com/shang/jetpackmoviekmp/data/paging/MovieSearchPagingSource.kt`
    ——套用相同去重邏輯。
  - `src/commonTest/kotlin/com/shang/jetpackmoviekmp/data/paging/MovieGenrePagingSourceTest.kt`
    與 `MovieSearchPagingSourceTest.kt`——新增「跨頁重複 id 應被過濾」測試案例。
  - `shared/data` 有 Kover 80% 下限規則，需確認新增程式碼路徑有對應覆蓋。
- 不涉及 `shared/data` public API 簽名變更（`MovieRepository.getMovieListPager`／
  `getMovieSearchPager` 回傳型別不變），故不影響 Android 呼叫端
  （`feature/home`、`feature/search`）與 `shared/app` 的 iOS framework 匯出邊界、
  Swift 消費端相容性。
- 不涉及第三方依賴新增或版本調整，`gradle/libs.versions.toml` 無需變更。

**明確排除（不在本次範圍）**：
- `feature/home`、`feature/search` 的 Compose UI 層 `itemKey` 寫法本身不修改。
- `feature/collect`（`CollectScreen`）、`feature/history`（`HistoryScreen`）的
  `items(..., key = { it.id })` 用法——資料來源為本地資料庫查詢而非外部分頁
  API，排序穩定、不具備本次相同的跨頁重複風險，不在本次範圍內處理。
- iOS 端（`ios-home-movie-list`／`ios-movie-search`）目前未觀察到對應崩潰，
  本次僅在共用的 `shared/data` 層修正成因，不額外調整 iOS UI 層渲染邏輯。
