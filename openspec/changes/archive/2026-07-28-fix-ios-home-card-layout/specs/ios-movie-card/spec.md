## ADDED Requirements

### Requirement: 電影卡片 SHALL 在首頁格線欄寬內維持穩定尺寸

`MovieCardView` 放置於首頁 `LazyVGrid` 時，SHALL 填滿格線分配的欄寬。海報區塊 MUST 維持 3:4 比例，且圖片的 Loading、Success、Error 狀態不得改變卡片寬高、與相鄰卡片重疊，或超出列表的水平內容範圍。

#### Scenario: 海報載入中顯示雙欄首頁
- **WHEN** 小型 iPhone 尺寸下首頁以雙欄格線顯示多張尚在載入的電影卡片
- **THEN** 每張卡片 SHALL 位於各自欄位內，卡片之間保留格線 spacing，且不得重疊

#### Scenario: 海報載入完成後保持格線排列
- **WHEN** 雙欄首頁中的任一海報由 Loading 轉為 Success 或 Error
- **THEN** 該卡片 SHALL 保持原欄位寬度與 3:4 海報高度，不得推開或覆蓋相鄰卡片

#### Scenario: 長文字資料顯示於窄欄
- **WHEN** 電影標題或上映日期的固有寬度超過首頁欄寬
- **THEN** 文字 SHALL 在卡片邊界內換行、縮放或截斷，且不得擴張卡片欄寬
