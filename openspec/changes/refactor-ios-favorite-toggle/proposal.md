## Why

iOS 端「收藏／取消收藏電影」的操作邏輯目前分散重複寫在 5 個 ViewModel 裡（`FavoritesViewModel`、`HomeContentViewModel`、`SearchViewModel`、`HistoryViewModel`、`MovieDetailViewModel`），每處都各自重寫一次防連點 guard、insert-or-delete 判斷與錯誤處理，且方法命名不一致（`toggleMovieCollectStatus` vs `toggleMovieCollect`）。這也連帶造成測試缺口：只有 `SearchViewModel` 額外自建了可 mock 的小 protocol，其餘 4 個 ViewModel 的收藏切換邏輯完全沒有單元測試覆蓋。現在收斂這段邏輯，可同時消除重複、統一命名，並讓所有相關 ViewModel 都能補上測試。

## What Changes

- 新增共用類別 `MovieCollectToggler`（連同既有的 `MovieCollectionToggling` protocol 與 `MovieRepositoryCollectionAdapter`，由 `SearchViewModel.swift` 搬移至新檔案 `iosApp/iosApp/Common/MovieCollectToggler.swift`），封裝防連點 guard、insert-or-delete 判斷、呼叫 `MovieRepository` 與錯誤處理。
- `FavoritesViewModel`、`HomeContentViewModel`、`SearchViewModel`、`HistoryViewModel`、`MovieDetailViewModel` 改為持有 `MovieCollectToggler` 實例，各自的 toggle 方法收斂為一行委派呼叫。
- 移除 `MovieCollectAction` enum（判斷邏輯已收斂進 `MovieCollectToggler`）。
- **BREAKING**（僅限 iOS 內部呼叫端，非對外 API）：`HistoryViewModel.toggleMovieCollect(data:)` 改名為 `toggleMovieCollectStatus(data:)`，`HistoryView.swift` 呼叫端同步更新。
- 補上 `FavoritesViewModel`、`HomeContentViewModel`、`HistoryViewModel`、`MovieDetailViewModel` 的收藏切換單元測試（沿用搬移後的 `FakeMovieCollectionToggling`）；`MovieCollectActionTests.swift` 改寫為 `MovieCollectTogglerTests.swift`。
- 不新增 Kotlin UseCase，不修改 `shared/domain`、`shared/data`；不處理 `MovieDetailBean → MovieCardResult` 轉換遺失 `isCollect` 的既有問題，`MovieDetailViewModel` 的 `observeCollectStatus()` 訂閱機制維持不變。

## Capabilities

### New Capabilities

（無）本次不引入新能力，純粹重構既有收藏操作的程式碼結構。

### Modified Capabilities

- `ios-movie-collection`：「iOS 收藏操作 SHALL 由注入的 shared Repository 執行」這條requirement 的實作方式改變——收藏相關 ViewModel 不再各自直接持有 `MovieRepository` 並自行判斷 insert/delete，而是委派給共用的 `MovieCollectToggler`（內部依賴 `MovieCollectionToggling` 這個窄介面包裝 `MovieRepository`）。insert/delete 的觸發條件（依卡片目前收藏狀態）與最終行為不變，需更新 spec 描述以反映新的委派架構。

## Impact

- **平台／模組**：僅影響 iOS 端（`iosApp` Xcode 專案），不影響 `shared/*`、`androidApp`、`core/*`、`feature/*` 任何 Gradle module。
- **新增檔案**：
  - `iosApp/iosApp/Common/MovieCollectToggler.swift`（新的 `MovieCollectToggler` 類別 + 搬移過來的 `MovieCollectionToggling` protocol、`MovieRepositoryCollectionAdapter`）
  - `iosApp/iosAppTests/Common/MovieCollectTogglerTests.swift`
  - `iosApp/iosAppTests/Common/FakeMovieCollectionToggling.swift`（由 `iosApp/iosAppTests/Search/FakeMovieCollectionToggling.swift` 搬移至共用測試位置）
- **修改檔案**：
  - `iosApp/iosApp/Favorites/FavoritesViewModel.swift`（移除 `MovieCollectAction` enum、改用 `MovieCollectToggler`）
  - `iosApp/iosApp/Home/page/HomeContentViewModel.swift`
  - `iosApp/iosApp/Search/SearchViewModel.swift`（移除搬移出去的 protocol/adapter 定義）
  - `iosApp/iosApp/History/HistoryViewModel.swift`（含方法改名）
  - `iosApp/iosApp/History/HistoryView.swift`（同步方法呼叫）
  - `iosApp/iosApp/Detail/MovieDetailViewModel.swift`
- **移除檔案**：`iosApp/iosAppTests/Favorites/MovieCollectActionTests.swift`（內容改寫進 `MovieCollectTogglerTests.swift`）
- **測試補強**：`FavoritesViewModelTests`、`HomeContentViewModelTests`、`HistoryViewModelTests`、`MovieDetailViewModelTests`（若尚不存在則新增）需補上收藏切換相關測試案例。
- **不受影響**：`shared/domain`、`shared/data`、`shared/model`、`androidApp`、`core/*`、`feature/collect`（Android 收藏邏輯不變）。
- **相容性**：純 iOS 內部重構，不涉及 shared public API 或跨語言邊界，不影響 iOS 對外匯出的 `Shared` framework 介面。
