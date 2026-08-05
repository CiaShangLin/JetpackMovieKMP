## MODIFIED Requirements

### Requirement: 已支援電影卡片的 tab SHALL 提供推入電影詳情頁的導覽

首頁、搜尋、收藏、歷史四個 tab 的根內容 SHALL 各自以 `NavigationStack` 承載，並以 `.navigationDestination(for: Int32.self)` 定義電影詳情頁目的地。既有 `MovieCardView.onMovieTap` callback SHALL 接上對應 tab 的 `NavigationPath`，以電影的 `movieCardId`（`Int32`）推入電影詳情頁；四個 tab SHALL 使用一致的 path element 型別，且此導覽目的地 SHALL 直接掛在各 tab 實際渲染的根 View（`HomeContentView`／`SearchView`／`FavoritesView`／`HistoryView`）上，不得只存在於未被實際渲染路徑使用的 wrapper 或替代 View 中。

#### Scenario: 從首頁電影卡進入詳情頁

- **WHEN** 使用者在首頁 tab 點擊一張電影卡
- **THEN** 首頁的 `NavigationStack` SHALL 推入以該電影 `movieCardId` 建立的電影詳情頁

#### Scenario: 從搜尋結果電影卡進入詳情頁

- **WHEN** 使用者在搜尋 tab 點擊一張電影卡
- **THEN** 搜尋 tab 的 `NavigationStack` SHALL 推入以該電影 `movieCardId` 建立的電影詳情頁

#### Scenario: 從收藏電影卡進入詳情頁

- **WHEN** 使用者在收藏 tab 點擊一張電影卡
- **THEN** 收藏 tab 的 `NavigationStack` SHALL 推入以該電影 `movieCardId` 建立的電影詳情頁

#### Scenario: 從歷史電影卡進入詳情頁

- **WHEN** 使用者在歷史 tab 點擊一張電影卡
- **THEN** 歷史 tab 的 `NavigationStack` SHALL 推入以該電影 `movieCardId` 建立的電影詳情頁

#### Scenario: 進入詳情頁時底部導覽列隱藏

- **WHEN** 任一 tab 推入電影詳情頁
- **THEN** 底部 Tab Bar SHALL 隱藏，與 Android 版本行為一致
- **AND** 返回操作 SHALL 一次僅 pop 一層，不影響其他 tab 各自的 `NavigationStack` 狀態

#### Scenario: 返回列表頁時底部導覽列復原

- **WHEN** 使用者從電影詳情頁 pop 回該 tab 的根內容畫面
- **THEN** 底部 Tab Bar SHALL 重新顯示
