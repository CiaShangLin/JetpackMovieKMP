## Why

iOS 的收藏 tab 目前只有 placeholder，使用者無法查看或移除已收藏電影；首頁電影卡片的愛心按鈕也尚未連接資料操作，因此按下後沒有任何結果。Shared 資料庫與 `MovieRepository` 已具備收藏讀寫與觀察能力，現在應將這些既有能力安全地接到 iOS SwiftUI。

## What Changes

- 將 iOS 收藏 tab 實作為可即時反映本機收藏資料的電影格線，包含空狀態與取消收藏操作。
- 建立 iOS 收藏畫面的 ViewModel／資料觀察流程，透過既有 `MovieRepository`、SKIE 匯出的 `Flow` 與 suspend API 消費 shared 收藏資料；不重複建立資料庫、DAO 或網路 API。
- 在首頁 `HomeContentView` 將 `MovieCardView.onCollectTap` 接到收藏切換操作，使加入或取消收藏後愛心狀態與收藏 tab 自動同步。
- 補足 iOS 收藏相關在地化文案與必要的 Swift 測試；既有 Android 收藏流程不在此 change 的實作範圍。

## Capabilities

### New Capabilities

- `ios-movie-collection`: iOS 可觀察、顯示及移除本機電影收藏，並提供明確空狀態。

### Modified Capabilities

- `ios-home-movie-list`: 首頁清單中的電影卡片可切換收藏，且狀態會隨 shared 收藏資料同步更新。
- `ios-movie-card`: 收藏按鈕在提供回呼時須觸發該回呼，供首頁與收藏頁執行加入或移除操作。
- `ios-main-bottom-navigation`: 收藏 tab 由 placeholder 升級為可用的收藏畫面。

## Impact

- `iosApp`：新增或調整收藏 ViewModel、`FavoritesView`、首頁卡片回呼接線、主 tab 組裝、String Catalog 與 Swift 測試。
- `shared/app`：預期不需新增資料 API；沿用既有 `KoinHelper.getMovieRepository()` 與 SKIE 的 `Flow`／suspend 匯出。若實作驗證發現 Swift 端無法安全消費既有公開型別，才以最小範圍新增具名橋接 API，並補 shared 測試。
- `shared/data`、`shared/database`：沿用既有 `MovieRepository.getAllMovieCollect()`、`insertMovieCollect()` 與 `deleteMovieCollect()`；不變更 schema、DAO 或依賴。
- `shared/domain`：本次不新增 UseCase，以維持現有 Android 收藏功能直接透過 `MovieRepository` 的既有模式。
- 不新增第三方依賴，也不修改 Android 功能模組。
