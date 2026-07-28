## 1. iosApp 共用圖片元件

- [x] 1.1 簡化 `RemoteAsyncImage`：保留 Kingfisher `placeholder` 作為 Loading、在 Error 時直接替換圖片內容，並移除不必要的外層 `ZStack` 與成功狀態追蹤。
- [x] 1.2 擴充 `MovieImageURLResolverTests` 或新增對應 XCTest，驗證圖片狀態與 fallback 尺寸策略的可測試部分。

## 2. shared/datastore 圖片 Host

- [x] 2.1 優先使用 TMDB configuration 的 `secureBaseUrl`，在其空白時才回退 `baseUrl`，避免 iOS ATS 封鎖 HTTP 圖片請求。
- [x] 2.2 新增 `DatastoreBaseHostUrlProviderTest`，驗證 HTTPS host 優先與 base URL 回退行為。

## 3. iosApp 電影卡與首頁格線

- [x] 3.1 讓 `MovieCardView` 以固定 3:4 海報容器與欄寬填滿策略呈現 `RemoteAsyncImage`，並限制標題與日期不得撐開卡片。
- [ ] 3.2 新增或更新電影卡的 SwiftUI Preview／測試輔助資料，涵蓋長標題、日期與圖片 Loading 狀態。
- [ ] 3.3 在小型 iPhone 模擬器驗證首頁雙欄清單於圖片載入前、成功後與失敗時均無重疊或水平溢出。

## 4. 驗證

- [x] 4.1 執行 `./gradlew iosFormatCheck`。
- [ ] 4.2 執行 iOS XCTest，確認既有 URL resolver 與新增圖片版面策略測試通過。
