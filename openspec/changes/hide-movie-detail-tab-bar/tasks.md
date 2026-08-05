## 1. iosApp 實作

- [ ] 1.1 在 `MovieDetailView.swift` 加上 `.toolbar(.hidden, for: .tabBar)`，使推入電影詳情頁時底部 Tab Bar 隱藏
- [ ] 1.2 確認遞迴推入下一部電影詳情頁（點擊推薦電影卡）時，Tab Bar 仍維持隱藏，無中間層短暫恢復可見的情形

## 2. iosApp 驗證

- [ ] 2.1 於 iOS Simulator 手動驗證四個 tab（首頁、收藏、搜尋、歷史）推入電影詳情頁時 Tab Bar 皆隱藏，pop 回列表頁時 Tab Bar 皆復原
- [ ] 2.2 執行 `iosFormat`、`iosFormatCheck`、`iosLint`（需本機安裝 `swiftformat`、`swiftlint`；此變更僅涉及 SwiftUI View modifier，無新增可獨立測試的邏輯，不額外新增 Swift unit test）

## 3. 最終驗證

- [ ] 3.1 執行 `openspec validate hide-movie-detail-tab-bar --type change --strict --no-interactive`
