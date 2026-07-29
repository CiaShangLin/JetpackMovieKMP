## Why

iOS 的歷史 tab 目前僅顯示 placeholder，雖然 shared 層已經持久化觀看紀錄，且 Android
已有完整的歷史功能。這使兩個平台在核心瀏覽體驗上不一致，使用者無法在 iOS 端查看、
管理或調整歷史電影的收藏狀態。

## What Changes

- 將 iOS `HistoryView` 由 placeholder 改為訂閱 shared `GetHistoryMovieListUseCase` 的可用歷史頁。
- 以既有 `MovieCardView` 的 SwiftUI 格線顯示觀看紀錄，並提供在地化空狀態。
- 提供切換電影收藏狀態與清空全部觀看紀錄的操作；資料異動後由 shared Flow 更新畫面。
- 由 `KoinHelper` 暴露歷史 UseCase，並讓需要 shared 依賴的 SwiftUI View 直接建立其 ViewModel，避免 Main 對各 tab 逐一轉送依賴。
- 更新底部導覽規格，使歷史 tab 不再是 placeholder；iOS UI 單元測試不納入本次範圍。

## Capabilities

### New Capabilities

- `ios-movie-history`: iOS 觀看歷史的觀察、顯示、收藏切換與清空功能。

### Modified Capabilities

- `ios-main-bottom-navigation`: 歷史 tab 改顯示可用的觀看歷史頁，不再顯示 placeholder。

## Impact

- 受影響模組：`iosApp`、`shared/app`。
- 受影響既有契約：`shared/domain` 的 `GetHistoryMovieListUseCase`、`shared/data` 的 `MovieRepository`、`ios-movie-card`、`ios-localization` 與 `ios-main-bottom-navigation` 規格。
- 不新增第三方依賴、不變更資料庫 schema，也不修改 Android `feature/history` 或 shared domain/data 的既有行為。
