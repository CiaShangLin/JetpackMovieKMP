## ADDED Requirements

### Requirement: 搜尋結果支援詳情導覽與收藏切換

系統 SHALL 讓搜尋結果中的每張 `MovieCard` 使用既有詳情導覽 callback，並提供收藏切換 callback。使用者點擊收藏時，系統 MUST 依該電影目前收藏狀態新增或刪除既有收藏資料；收藏資料異動後，已載入搜尋結果的卡片 MUST 顯示最新收藏狀態。

#### Scenario: 點擊搜尋結果電影

- **WHEN** 使用者點擊搜尋結果中的電影卡片內容
- **THEN** 系統 SHALL 將該電影的 `MovieCardData` 傳給上層既有的詳情導覽 callback

#### Scenario: 收藏未收藏的搜尋結果電影

- **WHEN** 使用者點擊一部 `isCollect` 為 false 的搜尋結果電影的收藏操作
- **THEN** 系統 SHALL 將該電影新增至收藏資料，且搜尋卡片在收藏資料流更新後顯示為已收藏

#### Scenario: 取消收藏已收藏的搜尋結果電影

- **WHEN** 使用者點擊一部 `isCollect` 為 true 的搜尋結果電影的收藏操作
- **THEN** 系統 SHALL 從收藏資料移除該電影，且搜尋卡片在收藏資料流更新後顯示為未收藏
