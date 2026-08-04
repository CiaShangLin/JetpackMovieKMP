## MODIFIED Requirements

### Requirement: iOS 收藏頁 SHALL 觀察並顯示 shared 本機收藏資料

iOS 收藏 tab SHALL 透過既有 `MovieRepository.getAllMovieCollect()` 觀察 shared 本機資料庫，不得建立重複的 iOS 收藏資料來源或在 SwiftUI 層重新排序。收到資料變動後 SHALL 以 shared 回傳的最新收藏優先順序顯示目前所有收藏電影，且每筆 `MovieCardData.movieCardIsCollect` 為 `true`。

#### Scenario: 進入收藏 tab 時顯示既有收藏
- **WHEN** 使用者切換至收藏 tab，且本機資料庫有一筆以上收藏
- **THEN** 畫面 SHALL 以 `MovieCardView` 格線依最新收藏優先順序顯示所有收藏電影，並以電影 id 維持穩定 identity

#### Scenario: 收藏資料變動時同步更新
- **WHEN** shared `getAllMovieCollect()` 發出新增或刪除後的清單
- **THEN** 收藏頁 SHALL 在不重新建立 app 或手動刷新畫面的情況下顯示 shared 提供的最新順序

## ADDED Requirements

### Requirement: iOS 收藏頁 SHALL 顯示在地化頁面標題

iOS 收藏 tab SHALL 在收藏清單與空狀態上方顯示在地化的收藏頁標題，並沿用 iOS 歷史頁既有的標頭視覺結構。

#### Scenario: 顯示收藏頁標題
- **WHEN** 使用者進入收藏 tab，無論 shared 收藏清單為空或有資料
- **THEN** 畫面 SHALL 在內容上方顯示在地化的收藏頁標題與分隔線
