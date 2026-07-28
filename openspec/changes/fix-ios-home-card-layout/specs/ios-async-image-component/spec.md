## MODIFIED Requirements

### Requirement: RemoteAsyncImage SHALL 提供 Loading、Success、Error 三種狀態呈現

`RemoteAsyncImage` SHALL 將 Kingfisher 圖片載入結果收斂為 Loading、Success、Error 三種狀態，並提供預設 UI 與呼叫端可覆寫的內容區塊。當呼叫端對元件提供有限的版面尺寸時，Loading、Success、Error 狀態 MUST 使用相同的父層邊界，且 fallback 內容不得以固有尺寸擴張父層。

#### Scenario: 圖片載入中顯示 Loading 狀態
- **WHEN** Kingfisher 尚未取得成功或失敗結果，且呼叫端提供固定寬高或比例容器
- **THEN** iOS 圖片元件 SHALL 在該容器邊界內顯示 Loading 內容，且不得改變容器尺寸

#### Scenario: 圖片載入成功顯示 Success 狀態
- **WHEN** Kingfisher 成功取得圖片
- **THEN** iOS 圖片元件 SHALL 顯示 Success 圖片內容，並允許呼叫端套用顯示樣式，且不得改變呼叫端已提供的容器尺寸

#### Scenario: 圖片載入失敗顯示 Error 狀態
- **WHEN** URL 無效、base host 不可用或 Kingfisher 載入失敗
- **THEN** iOS 圖片元件 SHALL 在呼叫端提供的容器邊界內顯示 Error 內容，且不得改變容器尺寸
