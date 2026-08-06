## Why

Android 電影卡片標題目前僅設定 `maxLines = 2`，未設定 `minLines`，當標題只有 1 行文字時卡片標題區塊會比 2 行標題矮，導致同一格線（Grid）中卡片高度參差不齊。iOS 端 `MovieCardView` 已透過 `.lineLimit(2, reservesSpace: true)` 讓標題固定保留 2 行垂直空間，卡片高度穩定。此變更讓 Android 對齊 iOS 既有做法，消除雙平台視覺行為落差。

## What Changes

- 調整 `core/ui` 的 `MovieCard.kt` 中 `MovieTitle` composable，為標題 `Text` 加上 `minLines = 2`（與既有 `maxLines = 2` 並存），使標題區塊不論實際文字行數皆固定佔用 2 行垂直空間。
- 新增 `android-movie-card` capability spec，記錄 Android 電影卡片標題「固定佔用 2 行空間」的行為規格，供後續維護與跨平台一致性比對依據。

## Capabilities

### New Capabilities
- `android-movie-card`：定義 Android 電影卡片標題文字固定佔用 2 行垂直空間的顯示規則，涵蓋標題文字不足 2 行時的留白行為。

### Modified Capabilities
（無；`android-ui-module` 現有 requirement 僅涉及模組結構/依賴方向/namespace，不涉及 `MovieCard` 元件的顯示行為，故不需修改既有 delta。）

## Impact

- **`core/ui`**（Android-only，`core/ui/src/main/kotlin/com/shang/jetpackmoviekmp/core/ui/MovieCard.kt`）：`MovieTitle` composable 新增 `minLines = 2`，為本次變更的唯一程式碼修改點。
- **間接受影響畫面**（不修改程式碼，僅視覺行為受影響）：所有透過 `core/ui` 的 `MovieCard` 呈現電影清單的 Android 畫面，包含首頁 Grid、搜尋結果、收藏清單、瀏覽歷史。這些畫面皆透過共用元件間接生效，不需個別調整。
- **不在本次範圍**：iOS 端（`iosApp`）已有對應行為，不需修改；`shared/*` 模組不涉及 UI 樣式，不受影響；不調整 `MovieCard` 的其他樣式屬性（字型、顏色、padding）。
