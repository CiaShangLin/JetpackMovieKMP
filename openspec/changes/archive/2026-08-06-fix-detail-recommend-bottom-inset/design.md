## Context

App 於 `MainActivity` 呼叫 `enableEdgeToEdge`，畫面內容會延伸到系統列（status bar / navigation bar）後方。`MainScreen`（`androidApp/.../MainActivity.kt`）只用 `Modifier.padding(WindowInsets.statusBars.asPaddingValues())` 消化了 `statusBars` inset，全專案沒有任何地方消化 `navigationBars` inset。

主畫面（Home/Search/Collect/History）使用 `JMNavigationSuiteScaffold`（adaptive scaffold）包住 `NavDisplay`；依 Android 官方 edge-to-edge 慣例，adaptive scaffold **不會**把 insets 往下傳給內部畫面內容，且**不應該**對它的 parent 加 `safeDrawingPadding` 之類的 modifier（會 clip 掉、破壞畫面的 edge-to-edge 效果）。`MovieDetailScreen` 是透過 Navigation3 推入的子畫面，不受 `JMNavigationSuiteScaffold` 的底部 Tab Bar 覆蓋，因此完全沒有任何東西幫忙消化 `navigationBars` inset。

`MovieDetailContent`（`feature/detail/.../MovieDetailScreen.kt`）內的 `LazyColumn` 依序渲染 `MovieSummary` → `MovieOverview` → `MovieActors` → `MovieRecommendations`，`MovieRecommendations` 內含推薦電影的 `LazyRow`，剛好是整個 `LazyColumn` 的最後一個 item，因此直接暴露在系統導覽列後方，被遮擋。

## Goals / Non-Goals

**Goals:**
- 讓 `MovieDetailScreen` 推薦電影列表（以及該畫面 `LazyColumn` 的任何最後一項內容）不再被系統導覽列遮擋。
- 修法遵循官方 edge-to-edge 慣例：inset 透過 scrollable 元件的 `contentPadding` 處理。

**Non-Goals:**
- 不調整 `MainActivity`/`MainScreen` 的全域 inset 處理策略。
- 不盤點或修改其他主頁籤（Home、Search、Collect、History）是否有類似風險。
- 不改變頂部 backdrop 大圖（`JMAsyncImage`）的呈現方式；它在 `Column` 最上方、`LazyColumn` 外，與底部 inset 無關。

## Decisions

- **在 `LazyColumn` 加 `contentPadding`，不對其 parent container 加 `Modifier.padding()`**：官方 edge-to-edge 指南明確要求 scrollable 元件的 inset 要透過 `contentPadding` 處理；若對 parent 加 padding 會直接裁掉內容，使其失去「可滑動到系統列後方」的能力，等於破壞 edge-to-edge 效果。
- **只消化 `navigationBars`，不使用 `safeDrawing`**：`statusBars` 已在 `MainScreen` 的上層被消化過，若在這裡改用涵蓋範圍更廣的 `safeDrawing`（含 status bar），會造成頂部重複 padding。因此精準使用 `WindowInsets.navigationBars.asPaddingValues()`。
- **範圍限定在 `feature/detail` 模組，不動 `MainActivity`/`MainScreen`**：目前 `MainScreen` 對 `statusBars` 的 `Modifier.padding()` 已經是套在 `JMNavigationSuiteScaffold` 的 parent 層級，本身已偏離官方建議（adaptive scaffold parent 不應加 `safeDrawingPadding` 類的 modifier）。若為了修這個 bug 再對 `navigationBars` 疊加同樣手法，會把同一個反模式放大到全部畫面，且影響未來可能需要延伸到系統列後方的畫面（例如全螢幕圖片）。因此改為在 `MovieDetailScreen` 局部處理，風險與影響範圍最小。
- **維持既有 Repository / UseCase / Koin 依賴方向不變**：此變更純屬 UI 層 `contentPadding` 調整，不涉及 ViewModel、UseCase 或 Repository。

## Risks / Trade-offs

- **[風險] 其他主頁籤（Home/Search/Collect/History）可能存在相同根因的類似問題** → 本次刻意不處理，待有實際反饋再開新的 change 個別評估（`JMNavigationSuiteScaffold` 是否已透過 Material3 元件自動處理，需個案確認）。
- **[風險] 未來若新增其他 Navigation3 子畫面（無 Tab Bar 覆蓋）也可能忘記處理底部 inset** → 本次不引入共用 modifier/元件封裝這個處理，屬已知技術債，非本次範圍；若日後有第二、第三個畫面出現同樣問題，屆時再評估是否值得抽出共用寫法。
