## 1. shared/app

- [x] 1.1 在 `KoinHelper` 新增 iOS 可呼叫的 `BaseHostUrlProvider` accessor，命名需清楚且符合既有 accessor 風格。
- [x] 1.2 更新 `KoinHelperTest`，驗證 `doInitKoinIos`／`initKoin` 後可解析 `BaseHostUrlProvider`。
- [x] 1.3 執行 `./gradlew :shared:app:iosSimulatorArm64Test` 或專案可用的 shared iOS 測試任務，確認 accessor 與既有 iOS bridge 未破壞。

## 2. iosApp

- [x] 2.1 在 iOS Xcode project 加入 Kingfisher Swift Package dependency，並確認只影響 iOS target。
- [x] 2.2 新增 iOS 共用圖片 URL resolver，實作與 Android `HostInterceptor` 對齊的 URL 組合規則。
- [x] 2.3 新增 SwiftUI iOS 圖片元件 `RemoteAsyncImage`，底層封裝 Kingfisher `KFImage`，支援 Loading、Success、Error 三種狀態與呼叫端覆寫內容。
- [x] 2.4 讓 `RemoteAsyncImage` 的預設路徑可透過 `KoinHelper` 取得 shared `BaseHostUrlProvider`，避免每個呼叫端重複注入相同 provider。
- [x] 2.5 將 `MovieCardView` 海報區改用新的 iOS 圖片元件，保留既有卡片版面、點擊與收藏 callback。
- [x] 2.6 補 Swift 單元測試或可在 Xcode test target 執行的測試案例，涵蓋相對路徑、完整 URL、空 base host、Kingfisher 快取路徑與 Loading／Success／Error 狀態映射。
- [x] 2.7 執行 `./gradlew iosFormatCheck iosLint`；若本機缺少 SwiftFormat／SwiftLint，需記錄實際錯誤並改以可用的 Xcode/Swift 編譯或測試任務驗證。

## 3. 驗證

- [x] 3.1 執行 `./gradlew ktlintCheck`，確認 Kotlin 變更符合格式規範。
- [ ] 3.2 手動確認 iOS 電影卡片使用相對 poster path 時會顯示海報，載入中與錯誤狀態有可辨識 fallback。
