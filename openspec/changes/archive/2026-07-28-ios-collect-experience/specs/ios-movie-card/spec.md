## MODIFIED Requirements

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
