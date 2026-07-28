# ios-movie-collection Specification

## Purpose
TBD - created by archiving change ios-collect-experience. Update Purpose after archive.
## Requirements
### Requirement: iOS 收藏頁 SHALL 觀察並顯示 shared 本機收藏資料

iOS 收藏 tab SHALL 透過既有 `MovieRepository.getAllMovieCollect()` 觀察 shared 本機資料庫，不得建立重複的 iOS 收藏資料來源；收到資料變動後 SHALL 顯示目前所有收藏電影，且每筆 `MovieCardData.movieCardIsCollect` 為 `true`。

#### Scenario: 進入收藏 tab 時顯示既有收藏

- **WHEN** 使用者切換至收藏 tab，且本機資料庫有一筆以上收藏
- **THEN** 畫面 SHALL 以 `MovieCardView` 格線顯示所有收藏電影，並以電影 id 維持穩定 identity

#### Scenario: 收藏資料變動時同步更新

- **WHEN** shared `getAllMovieCollect()` 發出新增或刪除後的清單
- **THEN** 收藏頁 SHALL 在不重新建立 app 或手動刷新畫面的情況下顯示最新清單

### Requirement: iOS 收藏頁 SHALL 提供明確空狀態

當 shared 收藏清單為空時，iOS 收藏頁 SHALL 顯示在地化空狀態，而非 placeholder、空白頁或錯誤畫面。

#### Scenario: 尚無收藏電影

- **WHEN** 使用者進入收藏 tab 且 `getAllMovieCollect()` 發出空清單
- **THEN** 畫面 SHALL 顯示收藏為空的在地化提示

#### Scenario: 移除最後一筆收藏

- **WHEN** 使用者在收藏頁取消最後一筆收藏，且 shared Flow 隨後發出空清單
- **THEN** 畫面 SHALL 由電影格線切換為收藏空狀態

### Requirement: iOS 收藏操作 SHALL 由注入的 shared Repository 執行

iOS 收藏相關 ViewModel 或操作物件 SHALL 以建構子接收 `MovieRepository`，並依卡片目前收藏狀態呼叫 `insertMovieCollect()` 或 `deleteMovieCollect()`；View 與 `MovieCardView` 不得直接取得 Koin 依賴。

#### Scenario: 將未收藏電影加入收藏

- **WHEN** 收藏操作收到 `movieCardIsCollect` 為 `false` 的電影
- **THEN** SHALL 呼叫 shared Repository 的 `insertMovieCollect()`，後續 shared Flow 發出包含該電影的收藏清單

#### Scenario: 將已收藏電影移除收藏

- **WHEN** 收藏操作收到 `movieCardIsCollect` 為 `true` 的電影
- **THEN** SHALL 呼叫 shared Repository 的 `deleteMovieCollect()`，後續 shared Flow 不再包含該電影
