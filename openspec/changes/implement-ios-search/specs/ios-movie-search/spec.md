## ADDED Requirements

### Requirement: iOS 搜尋頁可提交電影關鍵字

系統 SHALL 在既有 iOS Search tab 以 `NavigationStack` 內的 SwiftUI `.searchable` 提供系統原生搜尋欄與提交動作。使用者提交時系統 MUST trim 關鍵字，且僅在結果非空白時建立搜尋工作；輸入中的文字不得直接執行搜尋 API。尚未提交過有效關鍵字時，系統 MUST 顯示搜尋引導內容且不得建立搜尋工作；提交空白字串時，系統 MUST 保留最後一次有效搜尋的結果與 Presenter。

#### Scenario: 提交有效關鍵字

- **WHEN** 使用者輸入並提交「Dune」
- **THEN** 系統顯示該關鍵字的首次搜尋載入狀態，並開始載入結果

#### Scenario: 提交空白關鍵字

- **WHEN** 使用者提交只包含空白字元的輸入
- **THEN** 系統不建立搜尋請求，並保留最後一次有效搜尋的結果

#### Scenario: 清除系統搜尋欄

- **WHEN** 使用者使用 `.searchable` 的系統清除動作將已提交 query 清空
- **THEN** 系統只清除搜尋欄文字，並保留最後一次有效搜尋的 Presenter 與結果

### Requirement: 搜尋結果支援分頁載入與狀態回饋

系統 MUST 透過 iOS 專用 Presenter 消費搜尋分頁資料，並向 SwiftUI 提供目前結果快照、refresh 與 append 狀態。結果卡片接近列表末端時，系統 SHALL 存取對應 index 以觸發 Paging 預取。系統 MUST 顯示首次載入、首次錯誤、更多資料載入、更多資料錯誤與無更多資料的適當狀態。

#### Scenario: 首次搜尋載入成功

- **WHEN** 有效關鍵字的第一頁資料載入完成
- **THEN** 系統顯示電影卡片 grid，且每張卡片資料來自 Presenter snapshot

#### Scenario: 載入下一頁失敗

- **WHEN** 使用者捲動至末端且 append 載入失敗
- **THEN** 系統在清單尾端顯示錯誤與可重試動作，且不移除既有結果

#### Scenario: 已載入所有頁面

- **WHEN** append 載入完成且沒有下一頁
- **THEN** 系統在清單尾端顯示本地化的無更多資料提示

#### Scenario: 搜尋沒有符合結果

- **WHEN** 有效關鍵字的 refresh 載入完成且 snapshot 沒有電影
- **THEN** 系統顯示與初始引導不同的本地化無結果狀態

### Requirement: 搜尋 query 的 Presenter 生命週期必須隔離

系統 MUST 為每個已提交的非空白 query 建立一個獨立的 Presenter。提交任一有效 query（包含不同或相同 query）前，系統 MUST 取消舊 Presenter 的觀察工作並呼叫 `clear()`，使舊的 paging stream 不得更新新 query 的畫面。清除系統搜尋欄不得釋放目前 Presenter；畫面 ViewModel 釋放時則 MUST 取消觀察並清理目前 Presenter。

#### Scenario: 改為搜尋新關鍵字

- **WHEN** 使用者已搜尋「Dune」後再提交「Avatar」
- **THEN** 系統釋放 Dune 的 Presenter 並只顯示 Avatar 的載入與結果狀態

### Requirement: 搜尋結果可 refresh、retry 與切換收藏

系統 MUST 將錯誤 retry 委派給目前 Presenter 的 `retry()`，以重試原本失敗的頁碼。使用者重新提交有效 query 或下拉 refresh 時，系統 MUST 釋放目前 Presenter、以相同 query 建立新的 Presenter，並由第 1 頁重新載入。使用者切換電影卡片收藏時，系統 MUST 依目前 `isCollect` 狀態新增或刪除收藏，且後續分頁快照 SHALL 反映更新後的收藏狀態。

#### Scenario: 首次搜尋錯誤後重試

- **WHEN** 首次搜尋顯示錯誤且使用者點擊重試
- **THEN** 系統呼叫目前 Presenter 的 retry，並重新顯示對應 query 的載入結果

#### Scenario: 下拉重新整理

- **WHEN** 使用者在搜尋結果 grid 下拉重新整理
- **THEN** 系統保留相同 query、釋放目前 Presenter，並由第 1 頁重新載入

#### Scenario: 重新提交相同關鍵字

- **WHEN** 使用者對目前已搜尋的相同關鍵字再次按下鍵盤 Search
- **THEN** 系統釋放目前 Presenter，並從第 1 頁重新載入該關鍵字的結果

#### Scenario: 收藏搜尋結果

- **WHEN** 使用者點擊尚未收藏電影的收藏控制項
- **THEN** 系統新增該電影收藏，且後續顯示的卡片標記為已收藏
