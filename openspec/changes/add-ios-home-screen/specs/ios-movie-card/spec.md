## ADDED Requirements

### Requirement: 電影卡片元件 SHALL 為可跨頁面重用的獨立 SwiftUI View

電影卡片元件（`MovieCardView`）SHALL 定義於 `iosApp` 內的共用元件位置（非特定於首頁的資料夾），SHALL 僅依賴傳入的電影資料與 callback，不得直接依賴 `HomeViewModel` 或其他特定頁面的 ViewModel。

#### Scenario: 首頁使用電影卡片元件
- **WHEN** `ios-home-screen` 的分頁畫面需要顯示電影清單
- **THEN** SHALL 直接使用 `MovieCardView` 呈現每一筆電影資料，不得重新實作等價的卡片畫面

#### Scenario: 元件不依賴特定頁面的 ViewModel
- **WHEN** 檢查 `MovieCardView` 的初始化參數
- **THEN** SHALL 僅接受電影資料與點擊／收藏 callback（closure），不得直接持有或建構 `HomeViewModel` 或其他頁面專屬型別

### Requirement: 電影卡片 SHALL 顯示海報、標題、上映日期、評分與收藏狀態

電影卡片元件 SHALL 顯示電影海報圖片、標題、上映日期、平均評分，並以可點擊的收藏按鈕呈現目前的收藏狀態。

#### Scenario: 顯示電影基本資訊
- **WHEN** `MovieCardView` 收到一筆電影資料
- **THEN** SHALL 同時顯示該電影的海報、標題、上映日期與平均評分

#### Scenario: 顯示收藏狀態並可切換
- **WHEN** 使用者點擊卡片上的收藏按鈕
- **THEN** SHALL 觸發對應的收藏／取消收藏 callback，並讓呼叫端決定如何更新收藏狀態

### Requirement: 電影卡片 SHALL 使用 shared/model 提供的共用 UI 資料模型

`MovieCardView` 的輸入資料型別 SHALL 直接採用 `kmp-movie-card-ui-model` 定義於 `shared/model` 的電影卡片 UI 資料模型（經 SKIE 匯出），不得另外建立重複欄位的 iOS-only 包裝型別。

#### Scenario: 卡片輸入型別檢查
- **WHEN** 檢查 `MovieCardView` 的資料參數型別
- **THEN** SHALL 為 `shared/model` 提供的電影卡片 UI 資料模型（或其集合），不得是另外定義、欄位重複的自訂 struct
