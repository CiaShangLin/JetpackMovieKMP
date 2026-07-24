## 1. iosApp - isDebug 判斷邏輯共用化

- [x] 1.1 新增 `iosApp/iosApp/Logging/AppDebugFlag.swift`，將 `iOSApp.swift` 內 `networkLoggingEnabled` 的 `#if DEBUG` + `JM_DEBUG_NETWORK` 判斷邏輯抽出為共用型別（例如 `enum AppDebugFlag { static var isDebugLoggingEnabled: Bool { ... } }`）
- [x] 1.2 修改 `iosApp/iosApp/iOSApp.swift`，讓 `IosApp.init()` 改呼叫 `AppDebugFlag.isDebugLoggingEnabled` 傳給 `doInitKoinIos(isDebug:)`，移除原本的 `networkLoggingEnabled`
- [x] 1.3 手動驗證：分別在 Debug/Release scheme、以及設定/不設定 `JM_DEBUG_NETWORK` 環境變數的組合下，確認傳給 `doInitKoinIos` 的 `isDebug` 行為與抽出前一致（比對 log 輸出或中斷點確認）

## 2. iosApp - AppLogger protocol 與預設實作

- [x] 2.1 新增 `iosApp/iosApp/Logging/AppLogger.swift`，定義 `AppLogger` protocol（`debug`/`info`/`warning`/`error` 四個方法，帶入 `message` 與 `category`）
- [x] 2.2 新增 `iosApp/iosApp/Logging/OSAppLogger.swift`，實作 `AppLogger`，內部包裝 `os.Logger`，依 `isDebug` 決定：`true` 時輸出全部四個 level；`false` 時僅輸出 warning/error
- [x] 2.3 新增 `iosApp/iosApp/Logging/AppLog.swift`，提供全域可替換的注入點（`enum AppLog { static var logger: AppLogger = OSAppLogger(isDebug: AppDebugFlag.isDebugLoggingEnabled) }`）

## 3. iosApp - 驗證與程式碼風格

- [x] 3.1 手動驗證：透過 `AppLog.logger` 呼叫四種 level，於 `isDebug = true` 與 `isDebug = false` 兩種情況下分別確認 Xcode console／Console.app 的實際輸出結果符合 spec 定義（debug/info 在 `isDebug = false` 時不得出現）
- [x] 3.2 手動驗證：將 `AppLog.logger` 替換為一個簡單的測試替身（例如記錄呼叫次數的假實作），確認替換後呼叫端不需修改即可改由新實作處理
- [x] 3.3 執行 `./gradlew iosFormatCheck iosLint`（或對應的 `iosFormat`／`iosCodeStyleCheck`），確保新增的 Swift 檔案符合專案 SwiftFormat／SwiftLint 規則
