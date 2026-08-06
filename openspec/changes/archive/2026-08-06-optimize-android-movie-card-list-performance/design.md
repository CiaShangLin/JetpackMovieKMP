## Context

四個 Android 清單畫面目前的清單渲染方式：

- `feature/home/.../HomeScreen.kt` 的 `HomeScreenPager`、`feature/search/.../SearchScreen.kt` 的 `SearchResultScreen`：透過 `androidx.paging.compose.LazyPagingItems` 以 `items(movieList.itemCount) { index -> movieList[index] }` 取值，純 index-based，沒有替 item 標示穩定 key，也沒有用 Paging Compose 提供的 `itemKey`/`itemContentType`。
- `feature/collect/.../CollectScreen.kt`、`feature/history/.../HistoryScreen.kt`：一般 `List<MovieCardResult>` 搭配 `items(list) { movie -> ... }`，同樣沒有帶 `key`。
- 收藏狀態目前由 `shared/domain` 的 `GetHomeMovieListUseCase` / `GetSearchMovieListUseCase` 以 `pagerFlow.combine(collectIdsFlow) { pagingData, ids -> pagingData.map { it.copy(isCollect = ids.contains(it.id)) } }` 併入分頁資料流；`MovieCardResult`/`MovieCardData` 皆為 data class，任一收藏狀態改變都會讓整份目前分頁資料重新包一次，仰賴 data class 結構相等讓 Compose 跳過未變化 item 的重組。這段邏輯是 `commonMain`，iOS 的 `HomeMoviePagingDataPresenter`／`SearchMoviePagingDataPresenter` 共用同一份 UseCase。
- 圖片載入：`core/ui/coil/HostInterceptor.kt` 對所有非完整 URL 的相對路徑一律組成 `"${baseUrl}original$path"`，即不分清單縮圖或詳情頁大圖，一律請求 TMDB 最大原始解析度圖片；`core/ui/MovieCard.kt` 的 `MovieCover` 自行建立 `ImageRequest.Builder(...).data(model).build()` 後交給 `core/designsystem` 的 `JMAsyncImage` 顯示。
- `MovieCard` 最外層 `Box` 疊了 `shadow` → `clip` → `background` → `border` 四層 modifier，每個清單項目都需執行四次對應的 draw phase 計算。

## Goals / Non-Goals

**Goals:**
- 四個清單畫面的 item 皆以電影 id 作為穩定 key，讓 Compose／Paging 在項目增減、重新排序時能正確辨識項目身分。
- 補上測試證明：清單加上穩定 key 後，使用者對特定電影卡片按下收藏／取消收藏，UI 呈現的收藏狀態變化仍對應到該部電影，不會因清單重組而套用到錯誤的項目。
- 清單縮圖依顯示情境請求較小的 TMDB 圖片尺寸，降低捲動時的網路流量與解碼成本；不縮小既有大圖情境（如詳情頁 backdrop）的圖片品質。
- 精簡 `MovieCard` 的 modifier 疊層，降低每個 item 的 draw phase 成本，且視覺輸出與改動前一致。

**Non-Goals:**
- 不調整 `shared/domain` 的 `GetHomeMovieListUseCase` / `GetSearchMovieListUseCase` 收藏狀態 `combine` 邏輯本身（跨平台共用，留待後續視實測效能再開新 change）。
- 不調整 `PagingConfig`（`pageSize` / `prefetchDistance` / `enablePlaceholders`）。
- 不變更 iOS 端任何程式碼或行為。
- 不變更 `MovieCard` 對使用者可見的視覺樣式（顏色、陰影、圓角、邊框寬度等最終呈現效果）。

## Decisions

### 1. Paging 清單改用 `itemKey`/`itemContentType`，一般清單的 `items` 加 `key` lambda

`androidx.paging.compose` 提供 `LazyPagingItems<T>.itemKey(key: (T) -> Any)` 與 `itemContentType(...)` 這兩個 extension，專為搭配 `LazyGridScope`/`LazyListScope` 的 `items(count, key, contentType) { }` 設計，語意上比自行用 index 當 key 更貼合 Paging 的分頁增減語意。`HomeScreenPager`、`SearchResultScreen` 改用：

```kotlin
items(
    count = movieList.itemCount,
    key = movieList.itemKey { it.id },
    contentType = movieList.itemContentType { "MovieCard" },
) { index -> ... }
```

`CollectSuccessScreen`、`HistorySuccessScreen` 的一般 `items(list) { }` 改為 `items(list, key = { it.id }) { }`。

**為何不維持 index-based**：index 在清單插入/移除項目時語意會位移到別的資料上，Compose 沒有穩定 key 時只能依位置比對，捲動狀態與動畫在項目增減時容易對錯位置；穩定 key 讓 Compose 能正確追蹤「同一部電影」跨重組的身分。

**考慮過的替代方案**：只加 `key` 不加 `contentType`。`contentType` 主要幫助 Lazy layout 在 item 被回收/複用時判斷版面是否相容；本清單所有 item 版面一致（都是 `MovieCard`），加上後亦無額外成本，一併補上。

### 2. 收藏狀態正確性：只在 Android 端補測試驗證，不動 `shared/domain` combine 邏輯

因為 `MovieCardResult`/`MovieCardData` 是 data class，`combine` 產生的新 `PagingData` 若某 item 內容沒變，值相等使 Compose 結構相等判斷跳過該 item 重組；理論上「收藏標記對錯 item」目前不是已知會重現的 bug，而是 backlog 記錄下來的潛在風險。決定範圍收斂為：

- 加上 Android host test，模擬「清單含多部電影、其中一部觸發收藏切換」，驗證只有目標電影的 `movieCardIsCollect` 改變，其餘電影資料不受影響。
- 不動 `shared/domain` 的 `combine` 實作。若之後量測到 `combine` 造成的整份 diff 有實際效能影響，留給新的 change 處理（該改動會同時影響 iOS `HomeMoviePagingDataPresenter`／`SearchMoviePagingDataPresenter`，屬於跨平台變更，不適合跟本次 Android-only 的效能調整綁在一起）。

### 3. TMDB 圖片尺寸：由呼叫端透過 Coil request 夾帶尺寸提示，`HostInterceptor` 依提示組 URL，未提供時維持 `original`

`HostInterceptor` 目前直接把 `"original"` 寫死在組 URL 的字串中。改為：`HostInterceptor` 讀取當前 Coil `ImageRequest` 是否夾帶「目標尺寸」資訊（透過 Coil 的 request extras/tag 機制），有提供則用該尺寸的 TMDB path segment（例如清單縮圖用 `w342`），沒有提供則沿用現有的 `original`，維持向後相容。

`MovieCard.kt` 的 `MovieCover` 本來就自行呼叫 `ImageRequest.Builder(LocalContext.current).data(model).build()` 後才交給 `JMAsyncImage` 顯示，因此只需在這裡建構 `ImageRequest` 時額外夾帶「清單縮圖」尺寸提示即可；`core/designsystem` 的 `JMAsyncImage` 簽章不需變動（它本來就接受任意 Coil `model`，包含預先建好的 `ImageRequest`）。

`feature/detail` 的 backdrop 大圖（`JMAsyncImage(model = movie.backdropPath, ...)`）直接傳字串 model，不經過 `MovieCover`，不會夾帶尺寸提示，因此維持目前的 `original` 大圖行為，不受影響。

**考慮過的替代方案**：
- 在 `JMAsyncImage` 新增 `imageSize` 參數並往下傳。但 `JMAsyncImage` 目前把 `model` 完全交給 Coil，呼叫端已經能透過自建 `ImageRequest` 夾帶額外資訊，新增參數屬於不必要的簽章變動，故不採用。
- 直接在 `MovieCover` 端組好帶尺寸的完整 URL、跳過 `HostInterceptor` 的重寫。這會讓「組 TMDB 圖片 URL」的邏輯分散在兩處（`HostInterceptor` 與 `MovieCover`），未來要調整 host 或尺寸規則時容易漏改一處，故不採用；維持 `HostInterceptor` 作為組 URL 的唯一入口。

### 4. `MovieCard` 的 `Box` modifier 疊層精簡

`shadow` + `clip` + `background` + `border` 四層 modifier 改為用單一 `drawWithCache`（或等效的合併繪製方式）一次完成陰影、裁切、背景與邊框的繪製，減少每個 item 的 draw phase 呼叫次數。視覺輸出（陰影強度、圓角、邊框顏色與寬度）需與改動前逐一比對一致，避免精簡過程中不小心改變外觀。

## Risks / Trade-offs

- **[Risk]** `itemKey`/一般 `items` 的 `key` 若不是整數而是可能重複或為 null 的欄位，會在 debug 模式丟出重複 key 的 crash → **Mitigation**：一律使用 `movieCardId`/`id`（Int，資料庫主鍵，保證同一清單內不重複）。
- **[Risk]** `HostInterceptor` 改為讀取 request 提示後，若尺寸提示的 key/取值方式與 Coil 版本 API 不相容 → **Mitigation**：實作時以現有專案的 Coil 版本（`gradle/libs.versions.toml`）為準核對 API，並補單元測試覆蓋「有提示」與「無提示（維持 original）」兩種情境，避免現有呼叫端（detail backdrop）行為出現非預期改變。
- **[Risk]** 清單縮圖改用較小 TMDB 尺寸後，若使用者裝置為高密度螢幕、卡片顯示尺寸偏大，可能出現圖片略糊 → **Mitigation**：選用比卡片實際顯示寬度略大一級的 TMDB 尺寸（如 `w342`），而非最小尺寸；驗收時人工比對縮圖清晰度。
- **[Trade-off]** `MovieCard` modifier 合併後可讀性略降（單一 `drawWithCache` 內同時處理多種繪製），以效能換取程式碼直觀度；透過保留清楚的內部命名與適量註解緩解。
