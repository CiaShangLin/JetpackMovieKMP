## Why

目前 iOS 版電影詳情頁與 Android 版行為不一致：Android 進入詳情頁時底部導覽列會隱藏（透過切換到另一個 Activity 達成），而 iOS 目前明確保留底部 Tab Bar 可見（見既有 spec `ios-main-bottom-navigation` 中「進入詳情頁時底部導覽列維持可見」的 scenario）。為了讓 iOS 與 Android 提供一致的使用者體驗，且詳情頁本身已佔滿螢幕內容，需要在使用者進入電影詳情頁時隱藏底部 Tab Bar，返回列表頁時自動復原。

## What Changes

- 在 `MovieDetailView` 加上 `.toolbar(.hidden, for: .tabBar)`（SwiftUI 原生 modifier，iOS 16+ 支援；專案最低部署版本為 iOS 18.2，無相容性疑慮）。
- 四個 tab（首頁、收藏、搜尋、歷史）皆共用同一份 `MovieDetailView`，此變更為單一實作點，push 進電影詳情頁時 Tab Bar 隱藏，pop 回列表頁時自動復原。
- 不調整 `MainView`/`MainTab` 的 `TabView` 結構，也不調整各 tab 既有的 `NavigationStack` 拓樸；純粹是在被 push 的目的地畫面加上顯示層級的 modifier。

## Capabilities

### New Capabilities

（無）

### Modified Capabilities

- `ios-main-bottom-navigation`：「已支援電影卡片的 tab SHALL 提供推入電影詳情頁的導覽」需求中，「進入詳情頁時底部導覽列維持可見」的 scenario 行為反轉為「進入詳情頁時底部導覽列隱藏」，其餘 pop 行為（一次僅 pop 一層、不影響其他 tab 狀態）不變。

## Impact

- **受影響模組／平台**：`iosApp`（iOS，SwiftUI），不涉及任何 `shared/*` KMP 模組或 Android 端。
- **受影響檔案**：`iosApp/iosApp/MovieDetail`（或實際 `MovieDetailView` 所在目錄）內的 `MovieDetailView.swift`，僅新增一個 view modifier。
- **不受影響**：`MainView.swift`、`MainTab.swift`、`HomeView.swift`/`FavoritesView.swift`/`SearchView.swift`/`HistoryView.swift` 的 `NavigationStack` 結構與 `.navigationDestination` 定義皆不變動。
- **依賴／第三方套件**：無新增或調整，`.toolbar(.hidden, for: .tabBar)` 為 SwiftUI 原生 API。
- **範圍外**：其他 detail-like 頁面（如演員詳情、圖片瀏覽）目前不存在，本次不預先抽共用 modifier，亦不涉及 Android 端行為（Android 已透過獨立 Activity 達成隱藏效果，無需變更）。
