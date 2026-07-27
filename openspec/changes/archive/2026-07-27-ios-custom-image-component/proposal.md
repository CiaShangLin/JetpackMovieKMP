## Why

iOS 端目前在 `MovieCardView` 直接使用 SwiftUI `AsyncImage` 載入 `movieCardPosterPath`，但 TMDB 回傳的圖片路徑通常是相對路徑，無法像 Android Coil `HostInterceptor` 一樣自動補上 `BaseHostUrlProvider` 的 host。這會造成 iOS 電影海報在尚未傳入完整 URL 時載入失敗，也讓圖片載入狀態缺少一致的 Loading／Success／Error 呈現。

## What Changes

- 新增 iOS 專用的共用圖片元件 `RemoteAsyncImage`，封裝 URL 補 host 與 Kingfisher `KFImage` 圖片載入狀態處理。
- 透過 shared `BaseHostUrlProvider`（實際由 `DatastoreBaseHostUrlProvider` 綁定）取得 TMDB 圖片 base URL，將相對圖片路徑轉為可載入的完整 URL。
- 使用 Kingfisher 提供 iOS 圖片記憶體與磁碟快取能力，避免列表滑動或重進頁面時重複下載相同圖片。
- 圖片元件 SHALL 明確支援 Loading、Success、Error 三種狀態，並允許呼叫端提供對應 UI。
- `MovieCardView` SHALL 改用新的 iOS 圖片元件顯示海報，保留既有卡片資料模型與互動 callback。
- `shared/app` SHALL 提供 iOS 可呼叫的 `BaseHostUrlProvider` accessor，讓 `RemoteAsyncImage` 的預設路徑可集中取得 shared provider；完整 URL 不應觸發 host 補齊。

## Capabilities

### New Capabilities

- `ios-async-image-component`: 定義 iOS 專用圖片元件的 URL 組合、依賴注入、載入狀態與錯誤 fallback 行為。

### Modified Capabilities

- `ios-movie-card`: 電影卡片海報 SHALL 使用 iOS 共用圖片元件載入，避免直接使用未補 host 的相對路徑。

## Impact

- 受影響 module：`iosApp`、`shared/app`。
- 受影響 UI：`iosApp/iosApp/Common/MovieCard/MovieCardView.swift` 與新增的 iOS 共用圖片元件。
- 受影響 shared API：`shared/app/src/iosMain/kotlin/com/shang/jetpackmoviekmp/KoinHelper.kt` 需新增 `BaseHostUrlProvider` accessor，並補 `iosTest`。
- 驗證影響：`iosFormatCheck`／`iosLint` 會掃描既有 Swift 檔案；若既有格式或 lint 問題阻塞本次驗證，需以最小範圍修正。
- 依賴：iOS 端新增 Kingfisher Swift Package dependency；不需修改 `gradle/libs.versions.toml`。
- Android：不修改 Android Coil `HostInterceptor` 或 Android Compose 圖片元件行為。
