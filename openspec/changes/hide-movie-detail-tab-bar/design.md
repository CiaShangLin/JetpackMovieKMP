## Context

iOS 主畫面（`MainView.swift`）使用 SwiftUI 原生 `TabView` 呈現底部導覽列，`MainTab.swift` 定義五個固定 tab；首頁、收藏、搜尋、歷史四個 tab 各自持有獨立的 `NavigationStack(path: $path)`，並透過 `.navigationDestination(for: Int32.self)` push 進共用的 `MovieDetailView`。既有 spec `ios-main-bottom-navigation` 明確定義「進入詳情頁時底部導覽列維持可見」，此設計與 Android 版（進入詳情頁時透過切換 Activity 隱藏底部導覽）不一致。專案 `IPHONEOS_DEPLOYMENT_TARGET` 為 18.2，遠高於 SwiftUI `.toolbar(.hidden, for: .tabBar)` 所需的 iOS 16+。

## Goals / Non-Goals

**Goals:**
- 使用者從任一 tab 推入電影詳情頁時，底部 Tab Bar 隱藏；返回列表頁時自動復原。
- 四個 tab（首頁、收藏、搜尋、歷史）共用同一份 `MovieDetailView`，改一處即全數適用。
- 不改變既有 `TabView`／`NavigationStack` 結構與各 tab 的 pop 行為（一次僅 pop 一層、不影響其他 tab 狀態）。

**Non-Goals:**
- 不處理其他 detail-like 頁面（目前不存在，例如演員詳情、圖片瀏覽），不預先抽共用 modifier。
- 不調整 Android 端行為（Android 已透過獨立 Activity 達成隱藏效果）。
- 不變更 `MovieDetailViewModel` 或任何 shared/domain、shared/data 邏輯。

## Decisions

**採用 `.toolbar(.hidden, for: .tabBar)` 加在 `MovieDetailView` 上，而非調整導覽拓樸（如把 `TabView` 移到最上層之外，或改用自訂 TabBar）。**

理由：
- 目前拓樸是「`TabView` 包多個各自獨立的 `NavigationStack`」，這正是此 modifier 設計上支援良好的情境，不像「`NavigationStack` 包 `TabView`」那種巢狀組合在部分系統版本上有已知的 Tab Bar 閃現 bug。
- 專案最低部署版本 18.2，功能可直接使用，無需 UIKit bridge（`hidesBottomBarWhenPushed`）或其他 fallback。
- 影響範圍最小：只需修改 `MovieDetailView` 一個檔案，不牽動 `MainView`、`MainTab` 或四個 tab 根視圖的既有導覽程式碼，符合「修改範圍聚焦在使用者要求的行為」原則。

考慮過的替代方案：
- **調整整體導覽拓樸（全域單一 `NavigationStack` 搭配自訂 TabBar）**：可完全掌控 Tab Bar 顯示邏輯，但需重寫四個 tab 的導覽與状態管理，範圍遠超過本次需求，故不採用。
- **UIKit `hidesBottomBarWhenPushed`**：僅適用於 `UINavigationController` push，本專案 iOS 導覽層為 SwiftUI `NavigationStack`，不適用。

## Risks / Trade-offs

- [風險] `.toolbar(.hidden, for: .tabBar)` 在多層 push（例如電影詳情頁內點擊推薦電影卡再推入下一層詳情頁）時，需確認每一層目的地都正確套用該 modifier，避免中間層 Tab Bar 短暫恢復可見 → 緩解：`MovieDetailView` 本身即為遞迴 push 的目的地（見 `ios-movie-detail` spec 中「點擊推薦電影卡進入下一部詳情頁」情境），modifier 掛在該 View 上會隨每一層 push 一併套用，無需額外處理。
- [風險] 若日後在 `TabView` 與 `NavigationStack` 之間新增其他容器層（例如 sheet 或 overlay），此 modifier 的生效範圍可能改變 → 緩解：範圍外事項，發生時另開 change 重新評估。
