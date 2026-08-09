## ADDED Requirements

### Requirement: 共同 UI 測試旅程文件
系統 SHALL 在 `docs/uitests` 為每個已核准旅程建立一份平台中立的 `journey.xml`，其中包含可執行操作及可驗證的驗收條件。

#### Scenario: 建立核心旅程文件
- **WHEN** 實作 UI 自動化測試
- **THEN** 系統 MUST 建立啟動首頁、首頁點擊與滑動類型切換、首頁詳情與推薦、詳情收藏、搜尋、搜尋詳情、收藏移除、歷史顯示與清除、主題、語言、開發者資訊、首頁分頁及搜尋分頁旅程

### Requirement: 啟動與首頁類型切換旅程
系統 SHALL 將啟動首頁、以點擊切換首頁類型及以滑動切換首頁類型記錄為彼此獨立的旅程。

#### Scenario: 啟動完成
- **WHEN** 使用者啟動 App 且 Splash 完成
- **THEN** 旅程 MUST 驗證底部導覽列已出現，且不等待電影卡載入

#### Scenario: 點擊切換類型
- **WHEN** 使用者在首頁點擊另一個電影類型 Tab
- **THEN** 旅程 MUST 驗證目標 Tab 已成為選取狀態，且不驗證清單內容

#### Scenario: 滑動切換類型
- **WHEN** 使用者在首頁電影區水平滑動至相鄰頁
- **THEN** 旅程 MUST 驗證對應 Tab 的選取狀態已同步更新，且不驗證清單內容

### Requirement: 電影詳情與收藏旅程
系統 SHALL 將電影詳情、推薦區塊及收藏跨頁一致性記錄為可獨立執行的旅程。

#### Scenario: 從首頁顯示詳情與推薦
- **WHEN** 使用者點擊首頁中的固定電影卡並向下滑動詳情內容
- **THEN** 旅程 MUST 驗證詳情標題、收藏按鈕、推薦電影區塊及至少一張推薦卡出現

#### Scenario: 從詳情收藏電影
- **WHEN** 使用者在固定電影詳情點擊收藏按鈕並切換至收藏頁
- **THEN** 旅程 MUST 驗證收藏圖示狀態已切換且該電影存在於收藏頁

### Requirement: 搜尋旅程
系統 SHALL 以固定英文關鍵字 `Spider-Man` 定義搜尋與搜尋結果詳情旅程。

#### Scenario: 搜尋固定關鍵字
- **WHEN** 使用者輸入並送出 `Spider-Man`
- **THEN** 旅程 MUST 驗證指定電影卡出現在搜尋結果，且不進入詳情頁

#### Scenario: 從搜尋結果顯示詳情
- **WHEN** 使用者完成 `Spider-Man` 搜尋並點擊指定結果卡
- **THEN** 旅程 MUST 驗證該電影詳情標題與收藏按鈕出現

### Requirement: 收藏與歷史旅程
系統 SHALL 將取消收藏、顯示歷史及清除歷史記錄為各自獨立的旅程。

#### Scenario: 從收藏頁移除電影
- **WHEN** 使用者在已有固定電影的收藏頁取消收藏
- **THEN** 旅程 MUST 驗證該卡片已移除；若為最後一筆，MUST 驗證空狀態

#### Scenario: 顯示瀏覽歷史
- **WHEN** 使用者從首頁開啟固定電影詳情後切換至歷史頁
- **THEN** 旅程 MUST 驗證該電影卡存在於歷史記錄

#### Scenario: 清除瀏覽歷史
- **WHEN** 使用者在有瀏覽歷史時執行清除
- **THEN** 旅程 MUST 驗證歷史頁顯示空狀態

### Requirement: 設定旅程
系統 SHALL 定義主題、語言及開發者資訊的 UI 測試旅程。

#### Scenario: 切換並還原主題
- **WHEN** 使用者選擇深色主題後再選擇系統預設
- **THEN** 旅程 MUST 依序驗證深色設定值與外觀，以及系統預設設定值

#### Scenario: 切換並還原語言設定
- **WHEN** 使用者選擇英文後再選擇系統預設
- **THEN** 旅程 MUST 驗證設定列值依序更新，且不要求畫面文字變更

#### Scenario: 顯示開發者資訊
- **WHEN** 使用者開啟後關閉開發者資訊
- **THEN** 旅程 MUST 驗證 App 名稱、開發者名稱與技術棧文字，並驗證回到設定頁

### Requirement: 分頁旅程
系統 SHALL 分別定義首頁及搜尋結果的第二頁載入旅程。

#### Scenario: 首頁載入第二頁
- **WHEN** 使用者將首頁初始類型清單捲動到底
- **THEN** 旅程 MUST 驗證第二頁固定電影卡出現，且不驗證 loading footer

#### Scenario: 搜尋載入第二頁
- **WHEN** 使用者完成 `Spider-Man` 搜尋後將結果清單捲動到底
- **THEN** 旅程 MUST 驗證第二頁固定電影卡出現，且不驗證 loading footer
