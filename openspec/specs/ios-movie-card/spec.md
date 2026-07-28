# ios-movie-card Specification

## Purpose

定義 iOS 端可跨頁面重用的電影卡片 SwiftUI 元件，包含顯示欄位、收藏按鈕互動與元件放置位置，供首頁與後續的收藏、搜尋、歷史等頁面共用。
## Requirements
### Requirement: 電影卡片元件 SHALL 為可跨頁面重用的獨立 SwiftUI View

電影卡片元件（`MovieCardView`）SHALL 定義於 `iosApp` 內的共用元件位置（非特定於首頁的資料夾），SHALL 僅依賴傳入的電影資料與 callback，不得直接依賴 `HomeViewModel` 或其他特定頁面的 ViewModel。

#### Scenario: 首頁使用電影卡片元件
- **WHEN** `ios-home-screen` 的分頁畫面需要顯示電影清單
- **THEN** SHALL 直接使用 `MovieCardView` 呈現每一筆電影資料，不得重新實作等價的卡片畫面

#### Scenario: 元件不依賴特定頁面的 ViewModel
- **WHEN** 檢查 `MovieCardView` 的初始化參數
- **THEN** SHALL 僅接受電影資料與點擊／收藏 callback（closure），不得直接持有或建構 `HomeViewModel` 或其他頁面專屬型別

### Requirement: 電影卡片 SHALL 顯示海報、標題、上映日期、評分與收藏狀態

電影卡片元件 SHALL 顯示電影海報圖片、標題、上映日期、平均評分，並以可點擊的收藏按鈕呈現目前的收藏狀態。電影海報 SHALL 透過 `ios-async-image-component` 定義的 iOS 共用圖片元件載入，不得直接將 `MovieCardData.movieCardPosterPath` 傳入 SwiftUI `AsyncImage`。收藏按鈕的點擊 SHALL 僅觸發傳入 callback，不得同時觸發卡片的電影點擊 callback。

#### Scenario: 顯示電影基本資訊

- **WHEN** `MovieCardView` 收到一筆電影資料
- **THEN** SHALL 同時顯示該電影的海報、標題、上映日期與平均評分

#### Scenario: 顯示收藏狀態並可切換

- **WHEN** 使用者點擊卡片上的收藏按鈕
- **THEN** SHALL 觸發對應的收藏／取消收藏 callback，並讓呼叫端決定如何更新收藏狀態

#### Scenario: 點擊收藏按鈕不觸發電影卡片點擊

- **WHEN** `MovieCardView` 同時提供 `onMovieTap` 與 `onCollectTap`，使用者點擊收藏按鈕
- **THEN** SHALL 僅執行 `onCollectTap`，不得執行 `onMovieTap`

#### Scenario: 海報使用 iOS 共用圖片元件

- **WHEN** `MovieCardView` 顯示 `movieCardPosterPath`
- **THEN** SHALL 使用 iOS 共用圖片元件處理 host 補齊與 Loading／Success／Error 狀態，不得在卡片內直接重新實作等價邏輯

### Requirement: 電影卡片 SHALL 使用 shared/model 提供的共用 UI 資料模型

`MovieCardView` 的輸入資料型別 SHALL 直接採用 `kmp-movie-card-ui-model` 定義於 `shared/model` 的電影卡片 UI 資料模型（經 SKIE 匯出），不得另外建立重複欄位的 iOS-only 包裝型別。

#### Scenario: 卡片輸入型別檢查
- **WHEN** 檢查 `MovieCardView` 的資料參數型別
- **THEN** SHALL 為 `shared/model` 提供的電影卡片 UI 資料模型（或其集合），不得是另外定義、欄位重複的自訂 struct

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
