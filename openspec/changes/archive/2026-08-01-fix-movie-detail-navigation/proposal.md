## Why

Android 版點擊電影卡片後未跳轉到電影詳細頁，畫面呈現空白。根因是 `MainActivity.kt` 的 `mainEntry()`（實際被 `SuccessScreen` 使用的 `NavDisplay` entryProvider）的 `when` 分支缺少 `MovieDetailKey` case，任何導向詳情頁的 `NavKey` 都落入 `else -> PlaceholderScreen()` 的空白畫面；正確處理 `MovieDetailKey` 的 `DetailNavDisplay()` 是合併衝突修復（`c619d61`）遺留下來、從未被呼叫的死程式碼。此行為違反 `android-app-entry` spec 中已定義的「Android App MUST 支援電影詳情 Navigation3 目的地」與「detail 顯示時 MUST 隱藏主 Navigation Suite」需求，屬於實作未達成既有規格的缺陷，須立即修復。

## What Changes

- 在 `mainEntry()` 的 `when` 分支加入 `is MovieDetailKey -> movieDetailEntry(...)`，讓 `NavDisplay` 正確渲染 `feature:detail` 的電影詳情頁，不再落入 `PlaceholderScreen`。
- 確認首頁、搜尋、收藏、觀看紀錄與 detail 頁內推薦電影卡的 `onMovieClick` 回呼，皆會把對應 `MovieDetailKey(movieId)` 加入既有 `NavBackStack`。
- 串接 detail 頁返回行為（畫面返回鍵／系統返回），僅移除 back stack 最後一個 `MovieDetailKey`，正確回到前一個 detail 或原入口頁。
- 當 back stack 最後一個 key 為 `MovieDetailKey` 時，`MainActivity` 隱藏 `JMNavigationSuiteScaffold`（bottom navigation／navigation rail／drawer）；返回 root destination 後恢復。
- 移除 `MainActivity.kt` 中未被使用的死程式碼 `DetailNavDisplay()`，避免與實際生效的 `mainEntry()` 混淆，造成未來再次修復不完整。

## Capabilities

### New Capabilities

（無）

### Modified Capabilities

- `android-app-entry`：「Android App MUST 支援電影詳情 Navigation3 目的地」需求既有內容維持不變（本次為修復實作使其符合此需求），但新增一則情境明確要求 `NavDisplay` 實際使用中的 entryProvider（`mainEntry()`）本身必須直接處理 `MovieDetailKey`，MUST NOT 僅存在於未被呼叫的替代 entryProvider 中——避免本次「規格已定義、卻因程式碼分岔成死程式碼而未生效」的問題再次發生。

## Impact

- `androidApp`：`MainActivity.kt`（`mainEntry()` 的 `when` 分支、Navigation Suite 顯示／隱藏邏輯、移除死程式碼 `DetailNavDisplay()`）
- `feature/detail`：確認 `DetailNavigation.kt` 的 `MovieDetailKey`／`movieDetailEntry()` 介面維持不變，僅由 `mainEntry()` 正確呼叫
- `core/ui`：確認 `MovieCard` 的 `onMovieClick` 回呼串接至各 feature entry（`homeEntry`、`searchEntry`、`collectEntry`、`historyEntry`）無誤
- 不涉及新增依賴，不動 `buildSrc`（`DependenciesVersions`／`Dependencies`／`DependenciesProvider`）
