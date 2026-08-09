## ADDED Requirements

### Requirement: 正式資料來源的執行條件
系統 SHALL 使用正式資料來源執行 UI journey，並記錄資料不足或環境限制造成的不適用結果。

#### Scenario: 維持 production 組裝
- **WHEN** Android 或 iOS UI 測試啟動 App
- **THEN** 系統 MUST 維持 production Koin 組裝，且不得要求 fixture 或額外的 Shared 測試 API

### Requirement: 不適用結果的範圍
當正式資料來源未提供搜尋結果、下一頁或必要內容時，UI journey MUST 記錄原因、步驟與截圖為不適用，且不將其視為錯誤重試情境。

#### Scenario: 資料不足
- **WHEN** journey 所需的搜尋結果、下一頁或內容未出現
- **THEN** 執行結果 MUST 以不適用記錄原因、步驟與截圖
