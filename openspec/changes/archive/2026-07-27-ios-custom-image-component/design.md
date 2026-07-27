## Context

iOS 端目前在 `iosApp/iosApp/Common/MovieCard/MovieCardView.swift` 直接使用 SwiftUI `AsyncImage`，並將 `MovieCardData.movieCardPosterPath` 當成完整 URL 建立 `URL`。Android 端則透過 Coil `HostInterceptor` 注入 `BaseHostUrlProvider`，在圖片路徑為相對路徑時補上 TMDB 圖片 host 與 `original` size。

SwiftUI `AsyncImage` 的快取與控制能力不足以支撐電影列表常見的重複圖片載入場景；本次採用 Kingfisher 作為 iOS 圖片載入與快取底層。Kingfisher 是成熟的 Swift 圖片載入套件，提供 SwiftUI `KFImage`、記憶體快取、磁碟快取、placeholder、載入狀態 callback 與圖片處理能力，社群採用度高，適合做為 iOS 端對應 Android Coil 的方案。

`BaseHostUrlProvider` 已由 `shared/datastore` 的 `DatastoreBaseHostUrlProvider` 實作，並在 shared Koin module 中完成綁定；iOS 端透過 `shared/app` 匯出的 `KoinHelper` 取得需要的 shared 依賴。圖片使用情境目前只有電影遠端圖片一種，因此 `RemoteAsyncImage` 提供簡化 API，預設在內部集中取得 `BaseHostUrlProvider`，避免每個 feature View 重複傳遞相同依賴。

## Goals / Non-Goals

**Goals:**

- 建立 iOS 專用 SwiftUI 圖片元件 `RemoteAsyncImage`，底層使用 Kingfisher `KFImage`，統一處理 TMDB 相對圖片路徑與完整 URL。
- 將 URL 補 host 規則對齊 Android Coil `HostInterceptor`：base host 空字串時不改寫、`http/https` 完整 URL 不改寫、其他相對路徑組成 `${baseUrl}original${path}`。
- 透過 Kingfisher 提供記憶體與磁碟快取，避免相同 URL 重複下載。
- 讓圖片元件提供 Loading、Success、Error 三種狀態 UI 擴充點。
- 讓 `MovieCardView` 使用新圖片元件顯示海報。
- 在 `shared/app` 新增 iOS 可呼叫的 `BaseHostUrlProvider` accessor，並補 Koin 解析測試。

**Non-Goals:**

- 不修改 Android Coil `HostInterceptor`、`JMAsyncImage` 或 Android UI 行為。
- 不自訂圖片 networking stack，不在本次建立獨立 cache layer。
- 不變更 TMDB configuration 儲存格式與 DataStore schema。
- 不在本次加入 Kingfisher 以外的第三方圖片載入套件。

## Decisions

### Decision: iOS 元件維持 SwiftUI 專用，不建立跨平台 Compose 圖片元件

此需求源於 SwiftUI `AsyncImage` 無法套用 Android Coil interceptor，且原生元件缺少足夠的快取控制。問題面集中在 iOS UI，因此新增 SwiftUI 元件並以 Kingfisher `KFImage` 作為底層圖片載入器，符合目前 iOS app 已使用 SwiftUI View 組件的架構。

替代方案是把圖片 URL 組合邏輯上移到 shared model 或 mapper，但會讓資料層輸出與 UI 圖片 size 綁定，且 Android 已有 interceptor，可避免讓 model 混入平台圖片載入細節。

### Decision: 圖片載入底層採用 Kingfisher

Kingfisher 提供 SwiftUI `KFImage`、記憶體快取、磁碟快取、placeholder、載入結果 callback、retry 與圖片處理能力，社群採用度高，適合作為 iOS 端對應 Android Coil 的圖片方案。本次的自訂元件 SHALL 封裝 Kingfisher，避免 feature View 直接依賴 `KFImage` API。

替代方案包含繼續使用 SwiftUI `AsyncImage` 或使用 Nuke。`AsyncImage` 對快取與載入控制不足；Nuke 主 repo 仍有維護且 `NukeUI` 已併入主 repo，但社群採用度低於 Kingfisher。基於穩定性與團隊可維護性，本次選擇 Kingfisher。

### Decision: `RemoteAsyncImage` 預設自行取得 shared `BaseHostUrlProvider`

`shared/app` 新增 `KoinHelper.getBaseHostUrlProvider()`，讓 Swift 可取得 shared `BaseHostUrlProvider`。由於目前遠端圖片只有 TMDB 圖片 host 一種需求，`RemoteAsyncImage(path:)` 預設可在內部建立 resolver，並由 resolver 透過 `KoinHelper.shared.getBaseHostUrlProvider()` 取得 provider。這讓 `MovieCardView` 等消費端只需傳入圖片 path，避免每層 View 重複傳遞相同 provider。

為了維持測試與客製彈性，完整 initializer 仍允許傳入自訂 URL resolver；resolver 也可在測試中傳入假的 `BaseHostUrlProvider`。`MovieImageURLResolver` 只有在輸入不是 `http://` 或 `https://` 完整 URL 時才會讀取 provider，完整 URL 會直接保留，不觸發 host 補齊。

替代方案是由 Home 組裝根一路把 `BaseHostUrlProvider` 傳到每個卡片，但目前使用情境單一，會增加不必要的參數傳遞與樣板程式碼。

### Decision: URL 組合規則對齊 Android `HostInterceptor`

iOS 元件 SHALL 使用相同判斷：

- 原始輸入以 `http://` 或 `https://` 開頭時保留原始輸入。
- 其他輸入視為 TMDB 相對路徑；若 base host 不為空字串，組成 `${baseUrl}original${path}`；若 base host 為空字串，則不產生錯誤拼接 URL，交由元件顯示 Error fallback。

這能維持 Android 與 iOS 顯示同尺寸資源。未來若要支援 `w500` 等尺寸，應另開需求定義 size policy，而不是在本次需求中分歧。

### Decision: 圖片狀態 API 以 Loading、Success、Error 為主要契約

Kingfisher `KFImage` 可提供載入中 placeholder、成功圖片內容與失敗 fallback。元件對外 SHALL 收斂成 Loading、Success、Error。Loading 與 Error 使用預設佔位 UI，並允許呼叫端覆寫；Success 則輸出可套用 `resizable`、`aspectRatio` 等修飾的 image content。

替代方案是直接讓各 View 使用 `KFImage` 並自行處理 placeholder/error，但這會把狀態判斷與 URL 補 host 邏輯重複散落在每個消費元件。

### Decision: 保持現有 MVVM / Koin / shared Repository 模式

本需求不改動 Repository、UseCase 或資料流；圖片 URL 補 host 是 UI 載入層問題。iOS 端仍由 `KoinHelper` 匯出 shared 依賴，Swift ViewModel/View 以建構子參數接收依賴，沒有引入新的狀態管理架構。

## Risks / Trade-offs

- [Risk] `DatastoreBaseHostUrlProvider` 在 configuration 尚未載入前可能回傳空字串，造成相對路徑第一次載入失敗。→ Mitigation：元件在 base host 空字串或無效 URL 時呈現 Error 狀態；既有 Splash configuration 流程應負責在主畫面前完成設定。
- [Risk] 使用 `original` 圖片尺寸可能比 iOS 列表實際需求更大。→ Mitigation：本次先對齊 Android 行為；後續若有效能問題，再以獨立 change 定義跨平台圖片 size policy。
- [Risk] 新增 Kingfisher 會讓 iOS App 多一個第三方依賴。→ Mitigation：只在 iOS target 引入，並以共用圖片元件封裝 `KFImage`，避免依賴外擴到各 feature View。
- [Risk] Kingfisher cache key 預設依 URL absolute string，若未來圖片 URL 拼接策略改變，快取命中率會受影響。→ Mitigation：本次集中使用單一 URL resolver，確保相同圖片路徑產生穩定 URL。
- [Risk] 這次不涉及資料庫 schema。→ Mitigation：不需要 Room migration。

## Migration Plan

1. 在 `shared/app` 新增 `KoinHelper` accessor，讓 Swift 可取得 `BaseHostUrlProvider`。
2. 在 iOS Xcode project 加入 Kingfisher Swift Package dependency。
3. 在 `iosApp` 新增 `RemoteAsyncImage` 與 URL resolver，底層封裝 `KFImage`，預設可自行取得 shared `BaseHostUrlProvider`。
4. 將 `MovieCardView` 的海報區改用新圖片元件。
5. 補上 `shared/app` iOS Koin 解析測試與 Swift URL resolver 狀態／URL 組合測試。

Rollback 時可移除 Kingfisher dependency，讓 `MovieCardView` 暫時改回 SwiftUI `AsyncImage`，並保留 `KoinHelper` accessor；該 accessor 不改變既有行為。

## Open Questions

- 圖片尺寸是否長期維持 `original`，或後續要定義 iOS 列表專用尺寸（例如 `w500`）？
- Error 狀態是否需要全 app 統一圖示／文案，或由各消費端自行提供？
