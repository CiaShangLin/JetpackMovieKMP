## Why

合併 PR #29（iOS 遠端圖片元件）與 PR #30（iOS Design Token）後，首頁在遠端海報尚未載入時，電影卡片會互相重疊並超出雙欄格線。這使首頁無法正常瀏覽，需修復遠端圖片載入狀態下的卡片尺寸契約。

## What Changes

- 讓 `RemoteAsyncImage` 的載入、成功與錯誤狀態都能遵循呼叫端提供的版面尺寸。
- 將電影卡海報區塊的 3:4 比例固定在容器層級，避免遠端圖片或 placeholder 的固有尺寸改變 `LazyVGrid` 欄位量測。
- 優先使用 TMDB configuration 的 HTTPS 圖片 host，避免 iOS App Transport Security 封鎖海報請求。
- 補足卡片在首頁雙欄格線中的版面驗證，涵蓋遠端圖片載入前的狀態。

## Capabilities

### New Capabilities

無。

### Modified Capabilities

- `ios-async-image-component`: 遠端圖片的所有載入狀態必須尊重呼叫端的版面約束。
- `ios-movie-card`: 電影卡海報與卡片內容必須在首頁格線欄寬內穩定排列，不得與相鄰卡片重疊。
- `kmp-user-preferences-datastore`: 圖片 host provider 必須優先提供安全的 HTTPS host。

## Impact

- 受影響模組：`iosApp`、`shared/datastore`。
- 受影響檔案：`Common/Image/RemoteAsyncImage.swift`、`Common/MovieCard/MovieCardView.swift`、`DatastoreBaseHostUrlProvider.kt`，以及其 Swift/Kotlin 測試或 Preview 驗證。
- 不變更 Shared/Kotlin API、TMDB API 或第三方依賴。
