## MODIFIED Requirements

### Requirement: 收藏頁 MUST 反映本機收藏資料並提供空狀態

`CollectViewModel` MUST 透過既有 `MovieRepository.getAllMovieCollect()` 訂閱收藏電影，並以可觀察的 UI state 提供給 `CollectScreen`。清單有資料時，畫面 MUST 以既有 `MovieCard` 與 grid 依 shared 回傳的最新收藏優先順序顯示每一筆收藏；清單為空時 MUST 顯示收藏頁專用的空狀態圖示與文案。

#### Scenario: 有收藏資料時顯示最新收藏優先的電影
- **WHEN** `getAllMovieCollect()` 發出至少一筆已依收藏時間由新到舊排序的 `MovieCardResult`
- **THEN** 收藏頁 MUST 顯示收藏標題與每筆對應的 `MovieCard`，且不得在 Android UI 層重排 shared 回傳順序

#### Scenario: 尚無收藏資料時顯示空狀態
- **WHEN** `getAllMovieCollect()` 發出空清單
- **THEN** 收藏頁 MUST 顯示空狀態圖示與「目前沒有收藏」的在地化文案，且不得顯示空白 grid
