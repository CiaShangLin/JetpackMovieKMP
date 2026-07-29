## Why

搜尋結果的電影卡目前未提供收藏點擊操作，且搜尋 Pager 未合併最新的收藏資料；因此使用者即使完成收藏或取消收藏，卡片狀態也不會即時反映，與首頁及其他電影清單的行為不一致。

## What Changes

- 為搜尋結果的 `MovieCard` 串接電影詳情導覽與收藏切換事件。
- 在搜尋 ViewModel 透過既有 Repository 寫入或刪除收藏資料。
- 新增 `GetSearchMovieListUseCase`，合併搜尋 Paging 資料與收藏電影 id，讓搜尋卡片的收藏狀態會隨收藏資料異動更新。
- 調整搜尋 feature 由 UseCase 取得 Pager，並補齊對應單元測試。

## Capabilities

### New Capabilities

- 無。

### Modified Capabilities

- `android-search-module`: 搜尋結果必須支援電影點擊、收藏切換及即時收藏狀態更新。
- `kmp-movie-domain-usecases`: domain 層必須提供可標記搜尋 Paging 收藏狀態的 UseCase，並由 Koin 註冊。

## Impact

- 受影響模組：`feature/search`、`shared/domain`、`androidApp`。
- 受影響元件：SearchViewModel、SearchScreen、SearchNavigation、Android Navigation3 entry、Domain Koin module 與搜尋結果單元測試。
- 使用既有 `MovieRepository`、`MovieCard`、收藏資料流與 version catalog 依賴，不新增外部依賴或 API 金鑰設定。
