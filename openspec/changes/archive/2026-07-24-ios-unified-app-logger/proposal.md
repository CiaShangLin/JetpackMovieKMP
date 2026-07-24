## Why

iOS 端目前除錯僅依賴 `print`、Xcode console 與 Ktor network logging，缺乏一致的 log 格式與統一開關，正式版容易輸出過多雜訊甚至敏感資訊。此需求記錄於 backlog（來源：`ios-splash-rewrite` 實作中發現的缺口），現在著手補上 iOS 專屬的統一 App Log 機制。

## What Changes

- 在 `iosApp` 新增 `AppLogger` protocol（可替換注入的抽象介面），提供對應常見 log level 的方法（如 debug/info/warning/error）。
- 新增一個預設實作，包裝系統原生 log API，依 `isDebug` 決定是否輸出。
- `isDebug` 的判斷對齊 `iosApp/iosApp/iOSApp.swift` 現有的 `networkLoggingEnabled`（`#if DEBUG` + `JM_DEBUG_NETWORK` 環境變數覆寫）邏輯，不另外發明一套不相干的判斷方式。
- 提供一個可在啟動時替換注入的機制（例如簡單的靜態注入點或輕量 DI），讓測試或未來需求可替換成不同的 Logger 實作。

## Capabilities

### New Capabilities
- `ios-app-logger`：iOS 端統一 App Log 機制，包含可替換注入的 Logger protocol、預設實作、isDebug 判斷邏輯。

### Modified Capabilities
（無現有 capability 的 requirement 變更；本次為全新能力，不修改 `ios-koin-bootstrap`／`ios-koin-bridge` 既有 requirement，僅在其既有 `isDebug` 傳遞路徑之外新增獨立的判斷依據參考。）

## Impact

- `iosApp`：新增 Logger protocol、預設實作與注入點（新增檔案，範圍侷限在 iOS Xcode 專案內）。
- 不涉及 `shared/network`、`shared/data` 等 shared 模組；不整合 Ktor network logging、不整合 shared bridge log（留待後續 change 處理，已記錄於 backlog）。
- 不涉及 Android 端（`androidApp`、`core/*`）：目前 Android 端沒有現成的統一 Logger 封裝可參照，本次為 iOS 專屬設計，非移植既有 Android 實作。
