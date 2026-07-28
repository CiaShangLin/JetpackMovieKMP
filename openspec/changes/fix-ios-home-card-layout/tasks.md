## 1. iosApp 共用圖片元件

- [ ] 1.1 檢查並調整 `RemoteAsyncImage` 的容器與預設 fallback，使 Loading、Success、Error 在父層提供有限尺寸時使用相同邊界。
- [ ] 1.2 擴充 `MovieImageURLResolverTests` 或新增對應 XCTest，驗證圖片狀態與 fallback 尺寸策略的可測試部分。

## 2. iosApp 電影卡與首頁格線

- [ ] 2.1 讓 `MovieCardView` 以固定 3:4 海報容器與欄寬填滿策略呈現 `RemoteAsyncImage`，並限制標題與日期不得撐開卡片。
- [ ] 2.2 新增或更新電影卡的 SwiftUI Preview／測試輔助資料，涵蓋長標題、日期與圖片 Loading 狀態。
- [ ] 2.3 在小型 iPhone 模擬器驗證首頁雙欄清單於圖片載入前、成功後與失敗時均無重疊或水平溢出。

## 3. 驗證

- [ ] 3.1 執行 `./gradlew iosFormatCheck`。
- [ ] 3.2 執行 iOS XCTest，確認既有 URL resolver 與新增圖片版面策略測試通過。
