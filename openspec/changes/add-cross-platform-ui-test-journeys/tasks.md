## 1. 測試資料與組裝邊界

- [ ] 1.1 釐清並實作 Android 與 iOS 共用或等效的 UI-test launch mode，確保 production 預設仍使用正式 Koin 組裝。
- [ ] 1.2 在 `shared/app`（若選定為共用入口）或各平台 test target 建立固定成功 fixture：首頁類型、`Spider-Man` 搜尋、詳情、推薦及兩頁清單。
- [ ] 1.3 實作每次旅程前的收藏、歷史、主題及語言設定重置；補齊受影響 shared 或平台組裝層測試。
- [ ] 1.4 若修改 `shared/app` 的 iOS 匯出邊界，驗證 Kotlin/Swift API 命名與生命週期，並在 macOS 執行對應 iOS simulator 測試。

## 2. Android selector 與 UI 自動化支援

- [ ] 2.1 在 `androidApp` 為頂層導覽與測試啟動模式補齊穩定 selector，並補相關導覽／狀態單元測試。
- [ ] 2.2 在 `feature/home` 為首頁類型 Tab、Pager 與固定電影卡補齊 Compose semantics／`testTag`；補 ViewModel 或狀態邏輯測試。
- [ ] 2.3 在 `feature/search`、`feature/collect`、`feature/history`、`feature/detail`、`feature/setting` 與必要的 `core/ui` 電影卡元件補齊共同語意 selector；補受影響 ViewModel 或狀態邏輯測試。
- [ ] 2.4 建立 Android 旅程執行與截圖／結果保存機制，並確認既有 mobile-mcp／android-cli workflow 可解析 selector。

## 3. iOS selector 與 XCUITest 支援

- [ ] 3.1 在 `iosApp` 為 Tab、首頁類型、電影卡、收藏、搜尋、歷史、設定與開發者資訊補齊和 Android 同名的 `accessibilityIdentifier`。
- [ ] 3.2 建立 iOS UI test target 的固定資料啟動與狀態重置流程，避免連線至正式 TMDB。
- [ ] 3.3 建立 XCUITest 的共同 journey 解析／對應步驟、元素定位、手勢、斷言及截圖保存機制。
- [ ] 3.4 補齊 Swift 測試以驗證測試啟動設定與 selector 對應；於 macOS/Xcode 環境執行 iOS UI tests。

## 4. 共同 journey 文件與實作

- [ ] 4.1 在 `docs/uitests` 建立 UC-01 至 UC-13（排除 UC-14）的 `journey.xml`，記錄已確認的操作與驗收條件。
- [ ] 4.2 建立 UC-15A 首頁分頁與 UC-15B 搜尋分頁的獨立 `journey.xml`，僅驗證第二頁固定電影卡，不驗證 loading footer。
- [ ] 4.3 將 Android 與 iOS 執行結果分別保存，且每筆結果可追溯到 journey、步驟、selector／查詢方式及截圖。
- [ ] 4.4 分別在 Android 與 iOS 執行所有已核准旅程；記錄不適用或受環境限制的結果，不加入錯誤／重試旅程。

## 5. 驗證

- [ ] 5.1 執行 `openspec validate add-cross-platform-ui-test-journeys --type change --strict --no-interactive`。
- [ ] 5.2 執行 `./gradlew ktlintCheck` 與 `./gradlew :androidApp:assembleDebug`。
- [ ] 5.3 若修改 shared 模組，執行受影響模組的 `testAndroidHostTest`；若修改具 Kover 規則的 shared 模組，執行其 `koverVerify`。
- [ ] 5.4 在 macOS/Xcode 環境執行 iOS UI tests、`./gradlew iosFormatCheck` 及 `./gradlew iosLint`，並記錄 Windows 環境無法執行的限制。
