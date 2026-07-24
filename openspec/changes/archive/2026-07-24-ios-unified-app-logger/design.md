## Context

`iosApp` 目前沒有任何統一的 App Log 機制，除錯完全依賴 `print` 與 Xcode console。唯一與「依 debug/release 動態開關」相關的既有邏輯，是 `iosApp/iosApp/iOSApp.swift` 中的 `networkLoggingEnabled`：

```swift
private extension IosApp {
    static var networkLoggingEnabled: Bool {
        let value = ProcessInfo.processInfo.environment["JM_DEBUG_NETWORK"]?.lowercased()
        if ["1", "true", "yes"].contains(value) { return true }
        if ["0", "false", "no"].contains(value) { return false }
        #if DEBUG
            return true
        #else
            return false
        #endif
    }
}
```

這段邏輯目前只用來決定傳給 `InitKoinIosKt.doInitKoinIos(isDebug:)` 的值（進而控制 shared 端 Ktor network log level）。Android 端則是用 `ApplicationInfo.FLAG_DEBUGGABLE` 判斷 `isDebug`，一路傳入 `initKoinAndroid` → `InitKoin.kt` → `networkModule`。兩平台的 `isDebug` 判斷依據本來就不同（Android 用系統旗標，iOS 用編譯旗標 + 環境變數覆寫），沒有共用的跨平台判斷機制，這次新增的 iOS Logger 應該對齊「iOS 平台既有」的這條判斷邏輯，而不是嘗試比照 Android 的判斷方式。

`iosApp` 目前也沒有 XCTest target，因此本次無法比照 `shared/*` 模組寫自動化單元測試，僅能靠手動驗證（Xcode console 觀察輸出）。

## Goals / Non-Goals

**Goals:**
- 提供一個可替換注入的 `AppLogger` protocol，涵蓋常見 log level（debug/info/warning/error）。
- 提供一個預設實作，包裝系統原生 `os.Logger`（Apple 統一日誌系統），依 `isDebug` 決定輸出行為。
- `isDebug` 判斷邏輯與 `iOSApp.swift` 既有的 `networkLoggingEnabled` 共用同一份判斷來源，避免同樣的 `#if DEBUG` + 環境變數解析邏輯在兩處重複維護。
- 提供一個簡單的全域注入點，讓呼叫端可以替換成不同實作（例如未來的測試替身或遠端 log 上報）。

**Non-Goals:**
- 不整合 shared bridge（Kotlin/Native）端的 log 訊息，也不修改 `shared/network` 的 Ktor logging plugin。
- 不做遠端 log 上報、不做 log 落地檔案。
- 不涉及 Android 端；不嘗試建立跨平台共用的 Logger 抽象。
- 不新增 XCTest target；沒有自動化單元測試覆蓋（若團隊之後要補，屬於另一個 change）。

## Decisions

### 1. Protocol 設計：`AppLogger`

```swift
protocol AppLogger {
    func debug(_ category: String, _ message: @autoclosure () -> String)
    func info(_ category: String, _ message: @autoclosure () -> String)
    func warning(_ category: String, _ message: @autoclosure () -> String)
    func error(_ category: String, _ message: @autoclosure () -> String)
}
```

- 方法對應 debug/info/warning/error 四個 level，對齊 backlog 提到「一致格式輸出」的需求；不額外做到 Android 常見的 verbose/assert 等更細分 level，避免過度設計。
- `message` 用 `@autoclosure` 延遲字串組裝，`isDebug = false` 時可略過昂貴的字串插值運算。
- 參數順序為 `(category, message)`，對齊 Android `Log.d(TAG, message)` 的慣例，讓熟悉 Android 開發的成員能沿用相同的呼叫直覺。
- `category` 讓呼叫端標註來源（例如 `"network"`、`"lifecycle"`），對應 `os.Logger` 原生的 category 概念，方便未來篩選。

**替代方案考量**：曾考慮直接用 Kotlin/Native 端定義 `expect`/`actual` 的共用 Logger 介面，但使用者已明確要求「先只做 Swift 端原生封裝」，且目前沒有共用需求，強行做 expect/actual 會超出本次範圍，故不採用。

### 2. 預設實作：包裝 `os.Logger`

新增 `OSAppLogger`，內部持有 `os.Logger` 實例，依 `isDebug` 決定：
- `isDebug == true`：所有 level 都輸出（debug 用 `.debug`，info 用 `.info`，warning/error 用 `.error`／`.fault` 對應層級）。
- `isDebug == false`：只輸出 warning/error，避免正式版產生過多雜訊；也不輸出可能包含敏感資訊的 debug/info 內容。

**替代方案考量**：曾考慮直接包 `print`，但 `os.Logger` 是 Apple 現行建議的統一日誌 API，支援 category、隱私標記（`%{public}@`／`%{private}@`）與 Console.app 篩選，成本差異不大，優先選用 `os.Logger`。

### 3. `isDebug` 判斷邏輯共用化

將 `iOSApp.swift` 內 `networkLoggingEnabled` 的判斷邏輯抽出為獨立的共用工具（例如 `AppDebugFlag.isDebugLoggingEnabled`），讓：
- `IosApp.init()` 呼叫 `doInitKoinIos(isDebug:)` 時使用它。
- 新的 `OSAppLogger` 預設實作初始化時也使用它。

兩處共用同一份 `#if DEBUG` + `JM_DEBUG_NETWORK` 環境變數解析邏輯，避免未來其中一處改了判斷方式、另一處忘記同步更新。

**替代方案考量**：曾考慮讓 Logger 自己各自判斷一份新的 `isDebug`，但這樣會出現兩份可能不同步的判斷依據（例如未來只改了 Logger 的環境變數 key），故決定共用同一個來源。

### 4. 注入機制：全域可替換的靜態存取點

```swift
enum AppLog {
    static var logger: AppLogger = OSAppLogger(isDebug: AppDebugFlag.isDebugLoggingEnabled)
}
```

呼叫端使用 `AppLog.logger.debug(...)`；需要替換實作時直接指定 `AppLog.logger = ...`。

**替代方案考量**：專案目前沒有在 `iosApp`（純 Swift 端）使用任何 DI 容器，Koin 只組裝 shared/Kotlin 端的依賴，不涵蓋 Swift 原生型別。若為此新增一個 Swift DI 框架，成本與本次範圍不成比例，因此採用最小成本的全域靜態注入點，符合「protocol 可替換注入」的需求但不引入新框架。

### 5. 檔案放置位置

新增 `iosApp/iosApp/Logging/` 目錄：
- `AppLogger.swift`：protocol 定義
- `OSAppLogger.swift`：預設實作
- `AppLog.swift`：全域注入點（`enum AppLog`）
- `AppDebugFlag.swift`：`isDebug` 共用判斷邏輯（從 `iOSApp.swift` 抽出）

不放在 `shared/*` 任何模組，因為沒有跨平台共用需求，也不需要 expect/actual 結構。

## Risks / Trade-offs

- **[風險] 全域靜態注入點屬於簡易手法，缺乏正式 DI 容器的生命週期管理** → 目前 `iosApp` 規模小、Swift 端沒有既有 DI 框架，全域存取點足以滿足「可替換」需求；若未來 `iosApp` 導入正式 Swift DI，可再遷移。
- **[風險] 沒有 XCTest target，無法自動化驗證 Logger 行為（例如 isDebug=false 時是否真的不輸出 debug/info）** → 本次僅能手動在 Xcode console 驗證；若團隊之後認為需要自動化測試覆蓋，應另開 change 補上 iosApp 的 XCTest target。
- **[風險] 抽出 `AppDebugFlag` 會修改 `iOSApp.swift` 既有程式碼** → 屬於單純的邏輯搬移（extract），行為不變（環境變數 key、`#if DEBUG` 判斷順序維持一致），不影響現有 `ios-koin-bootstrap` spec 對 `isDebug` 傳遞的 requirement。

## Open Questions

- `category` 參數是否要有預設值（例如 `category: String = "app"`）讓呼叫端可省略？留待實作階段依實際呼叫情境決定，不影響本次 design 的整體方向。
