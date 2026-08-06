## Why

電影詳情頁（`MovieDetailScreen`）最下方的推薦電影列表（`LazyRow`）被系統導覽列（navigation bar）遮擋，使用者無法完整看到或點擊最後一排卡片。根因是 App 已啟用 edge-to-edge，但畫面內容完全沒有消化 `navigationBars` inset，而推薦電影區塊恰好是頁面內 `LazyColumn` 的最後一個 item，因此直接暴露在系統導覽列後方。

## What Changes

- 在 `MovieDetailContent`（`feature/detail`）的 `LazyColumn` 加上 `contentPadding`，消化 `WindowInsets.navigationBars`，讓最後一個 item（推薦電影區塊）不再被系統導覽列遮擋。
- 修正方式遵循官方 edge-to-edge 慣例：inset 透過 scrollable 元件的 `contentPadding` 處理，不對 `LazyColumn` 的 parent container 使用 `Modifier.padding()`（避免裁掉內容、喪失可滑動到系統列後方的能力）。
- 不調整 `MainActivity`/`MainScreen` 的全域層級 inset 處理，範圍限定在 `feature/detail` 模組。

## Capabilities

### New Capabilities

（無）

### Modified Capabilities

- `android-movie-detail-module`：新增「detail 畫面內容底部需正確消化 `navigationBars` inset，確保推薦電影區塊不被系統導覽列遮擋」的需求與驗收情境。

## Impact

- `feature/detail`（Android-only，`androidMain` source set）：修改 `MovieDetailScreen.kt` 中 `MovieDetailContent` 的 `LazyColumn`，加上 `contentPadding` 消化 `WindowInsets.navigationBars`。
- 不影響 `shared/*`、`core/*`、`androidApp`、`iosApp` 等其他模組。
- 不在本次範圍：`MainActivity`/`MainScreen` 全域 inset 處理，以及其他主頁籤（Home、Search、Collect、History）是否存在類似風險——這些頁面即使可能有相同根因，也留待之後有實際反饋再開新的 change 處理。
