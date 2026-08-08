## Context

`shared/data` 的 `MovieGenrePagingSource`／`MovieSearchPagingSource`
（`commonMain`）各自呼叫 TMDB `discover`／`search` API 依頁碼載入
`MovieCardResult` 清單，直接把單頁 `response.data.results` 原封不動包成
`LoadResult.Page` 回傳，不記錄先前已載入過哪些 `id`。

TMDB 這兩個端點依人氣或相關性排序，排序在多次請求之間可能即時漂移
（新資料寫入、熱度分數更新等）。當使用者持續向下滑動、`Pager` 依序載入
第 N 頁、第 N+1 頁時，若排序在兩次請求之間變動，同一部電影可能同時落在
第 N 頁尾端與第 N+1 頁開頭，導致同一個 `PagingSource` 生命週期內對外
emit 出重複 `id` 的 `MovieCardResult`。

Android 端 `feature/home`／`feature/search` 使用
`LazyPagingItems.itemKey { it.id }` 作為 `LazyVerticalGrid`／`LazyColumn`
的 key，Compose 對 key 唯一性是強制檢查、重複即拋出
`IllegalArgumentException` 崩潰；iOS 端目前尚未觀察到對應崩潰，但成因
同樣存在於共用的 `shared/data` 層。

## Goals / Non-Goals

**Goals:**
- 確保 `MovieGenrePagingSource`／`MovieSearchPagingSource` 在同一個
  `PagingSource` 實例的生命週期內（一次 `Pager` 建立到下一次
  `invalidate`／`refresh` 之間），對外 emit 的 `MovieCardResult` 不含重複
  `id`。
- 修正不得影響既有分頁邊界計算（`prevKey`／`nextKey`／`totalPages`）與
  `MovieRepository` public API 簽名。

**Non-Goals:**
- 不處理 Compose／SwiftUI UI 層的 key 策略（如改用複合 key）——去重在
  資料源頭完成即可，UI 層維持現狀。
- 不處理 `CollectScreen`／`HistoryScreen` 本地資料庫查詢清單（排序穩定，
  非本次成因）。
- 不改變 TMDB API 呼叫方式或分頁策略（例如不改成一次抓多頁做整體去重、
  不引入 cursor-based pagination）。

## Decisions

### 去重範圍：單一 `PagingSource` 實例的生命週期內

**決策**：在 `MovieGenrePagingSource`／`MovieSearchPagingSource` 內各自維護
一個 `MutableSet<Int>`（已載入 `id` 集合）作為 instance 欄位，`load()`
成功取得回應後，先過濾掉集合中已存在的 `id`，再把剩餘項目的 `id` 併入
集合，最後才組成 `LoadResult.Page`。

**為何不做全域／跨 session 去重**：Paging3 的設計是每次 `refresh`
（下拉重新整理、`invalidate()`）都會建立全新的 `PagingSource` 實例
（`PagingSourceFactory` 重新呼叫），因此 instance 欄位天然隨 refresh 重置，
不需要額外清除邏輯，也不會讓「上次 session 看過的電影」永久排除在外
（例如電影可能之後真的被移出清單又重新進來，仍應正常顯示）。

**為何不需要同步鎖（Mutex／synchronized）**：Paging3 保證同一個
`PagingSource` 實例的 `load()` 呼叫是循序執行、不會併發呼叫，維持
instance 內部可變狀態是安全的（`androidx.paging` 官方文件對 `PagingSource`
生命週期的既有假設）。

**考慮過的替代方案**：
- 在 `MovieRepository`／`MovieRepositoryImpl` 層對 `PagingData` 套用
  `.map`／`distinctUntilChangedBy` 做去重——`PagingData` 的 transform 需要
  跨頁狀態才能正確去重，且 Paging3 的 `map` 是逐頁執行、同樣拿不到「先前
  頁面已出現的 id」這種跨頁上下文，複雜度不比在 `PagingSource.load()`
  內直接處理低，故不採用。
- 只在 Android UI 層（`itemKey`）用複合 key（如 `"${id}_$index"`）避開
  崩潰——只是掩蓋症狀，重複資料仍會出現在清單中（使用者會看到同一部
  電影出現兩次），不是本次要修正的行為，故不採用；且不修正資料源頭，
  iOS 未來若做同樣的清單渲染仍會有重複資料問題。

### 保持既有分頁邊界計算邏輯不變

**決策**：`prevKey`／`nextKey`／`totalPages` 的計算依據仍是 API 回應的
`page`／`totalPages` 中繼資料，不受去重後實際回傳筆數影響。即使去重後
某一頁回傳的 `data` 筆數變少（甚至為 0），只要 `nextKey` 非 null，
Paging3 仍會依 `prefetchDistance` 自動觸發下一頁載入，不會誤判為清單
已到底。

**為何不依去重後筆數重新計算頁碼**：TMDB 的頁碼語意來自伺服器端、與
本地過濾筆數無關，若混用兩種語意會讓 `prevKey`／`nextKey` 失去與伺服器
分頁的對應關係，增加除錯與維護成本。

## Risks / Trade-offs

- **[風險] 去重造成單頁視覺筆數不穩定**：某一頁去重後可能只剩少數幾筆
  甚至 0 筆，使用者滑動時偶爾會感覺「這一小段沒有新內容」。
  → **緩解**：這是伺服器排序漂移下的正常結果，且 Paging3 會自動接續載入
  下一頁補足畫面，不需額外處理；若之後有明顯的 UX 落差再另行評估。
- **[風險] `MutableSet<Int>` 隨分頁持續成長，理論上無上限**：極端情況下
  使用者持續滑動載入非常多頁時，記憶體占用會隨已載入電影數量線性成長。
  → **緩解**：TMDB `discover`／`search` 的 `totalPages` 本身有上限
  （TMDB API 限制通常為 500 頁），且集合只存 `Int`，實務上不構成問題；
  不在本次引入額外的 LRU／上限機制，避免過度設計。
- **[風險] 測試需覆蓋跨頁重複情境**：現有 `MovieGenrePagingSourceTest`／
  `MovieSearchPagingSourceTest` 目前皆為單頁情境測試，需新增「連續兩次
  `load()` 呼叫、第二次回應包含第一次已出現的 `id`」的案例，並驗證
  `LoadResult.Page.data` 過濾掉重複項目。
  → 已於 tasks.md 中列為必要任務，且 `shared/data` 有 Kover 80% 下限規則
  把關。
