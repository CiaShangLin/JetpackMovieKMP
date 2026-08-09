## ADDED Requirements

### Requirement: 跨平台穩定 selector
系統 SHALL 為共同 UI 目標提供相同語意名稱的穩定 selector。

#### Scenario: 定位共同控制項
- **WHEN** 任一平台執行 journey 的導覽、類型、電影卡、收藏、搜尋或設定操作
- **THEN** Android MUST 以 Compose semantics 或 `testTag`，iOS MUST 以 `accessibilityIdentifier` 定位相同語意目標

### Requirement: 平台原生旅程執行
系統 SHALL 以平台原生工具執行相同的 journey 操作與驗收意圖。

#### Scenario: 執行 Android 旅程
- **WHEN** Android UI 測試執行 journey
- **THEN** 系統 MUST 使用既有 Android 自動化工具鏈並保存每個步驟的結果與截圖

#### Scenario: 執行 iOS 旅程
- **WHEN** iOS UI 測試執行 journey
- **THEN** 系統 MUST 使用 XCUITest 定位元素、執行操作並保存每個步驟的結果與截圖

### Requirement: 平台分離的測試證據
系統 SHALL 將 Android 與 iOS 的執行證據分別儲存，且可追溯至同一 journey 名稱與步驟。

#### Scenario: 產生執行紀錄
- **WHEN** 任一平台完成或失敗於 journey 步驟
- **THEN** 系統 MUST 記錄平台、步驟、狀態、定位方式與對應截圖路徑
