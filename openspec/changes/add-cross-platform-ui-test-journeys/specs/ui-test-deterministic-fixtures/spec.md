## ADDED Requirements

### Requirement: 固定成功情境資料
系統 SHALL 為 UI 測試提供不呼叫正式 TMDB 的固定成功資料。

#### Scenario: 啟用測試資料
- **WHEN** Android 或 iOS UI 測試以測試模式啟動 App
- **THEN** 系統 MUST 提供固定首頁類型、`Spider-Man` 搜尋結果、詳情、推薦電影及至少兩頁清單資料

### Requirement: 每個旅程的資料重置
系統 SHALL 在每個 UI 測試旅程開始前重置可變動的使用者資料。

#### Scenario: 重置跨旅程狀態
- **WHEN** 新的 UI 測試旅程啟動
- **THEN** 系統 MUST 將收藏、瀏覽歷史、主題及語言設定還原為已定義初始狀態

### Requirement: 僅支援成功路徑
固定資料模式 MUST 僅保證已核准的成功情境，且不提供錯誤、重試或正式網路連線的驗收旅程。

#### Scenario: 執行已核准旅程
- **WHEN** 執行本 change 定義的 UI 旅程
- **THEN** 系統 MUST 不以網路失敗或重試畫面作為驗收條件
