## Why

目前 Android 與 iOS 雖然提供對應的電影瀏覽功能，但沒有共同定義、可重複執行的 UI 自動化測試旅程。直接依賴 TMDB 即時回應及畫面座標會使測試結果不穩定，也無法有效驗證兩端的行為一致性。

## What Changes

- 建立跨 Android 與 iOS 共用的 UI 測試旅程定義，並以既有 `journey.xml` 格式記錄操作與驗收條件。
- 提供可識別的 UI selector 與結構化 journey 文件，使電影卡、搜尋欄、Tab、設定項目及收藏操作可穩定定位與記錄。
- 為 Android 與 iOS 分別建立測試執行轉接；Android 沿用既有自動化工具鏈，iOS 以 XCUITest 對應相同旅程與證據輸出。
- 納入啟動、首頁類型切換、詳情與推薦區塊、收藏、搜尋、歷史、設定、開發者資訊及首頁／搜尋分頁等已確認旅程。
- 排除網路失敗與重試情境；本次測試資料僅需支援成功路徑。

## Capabilities

### New Capabilities

- `cross-platform-ui-test-journeys`：定義 Android 與 iOS 共用的 UI 自動化測試旅程、操作步驟與驗收條件。
- `ui-test-live-data-conditions`：定義使用正式資料來源時的執行前提、不適用條件與結果記錄方式。
- `ui-test-platform-adapters`：為 Android 與 iOS 提供可執行共同旅程的 selector 與平台測試轉接。

### Modified Capabilities

- 無。

## Impact

- **Android／`androidApp`、`feature/home`、`feature/search`、`feature/collect`、`feature/history`、`feature/detail`、`feature/setting`、`core/ui`**：新增測試 selector、journey 執行與結果保存支援；正式使用者流程不變。
- **iOS／`iosApp`**：新增 `accessibilityIdentifier` 與 XCUITest 對應測試；不改變正式 SwiftUI 導覽或商業邏輯。
- **共用資料與組裝邊界**：不新增 fixture 或 Shared 測試組裝 API，維持 production Koin 組裝。
- **測試文件／`docs/uitests`**：新增各旅程的 `journey.xml` 與執行證據目錄規範。
- 不新增第三方依賴；不修改 `shared/domain`、資料庫 schema 或 production TMDB API 契約。
