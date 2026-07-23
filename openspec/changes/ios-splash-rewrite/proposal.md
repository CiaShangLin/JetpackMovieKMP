## Why

`iosApp` 目前的 `Splash` 目錄僅有骨架檔案（`SplashUiState.swift` 已定義基本 enum，`SplashView.swift`／`SplashViewModel.swift` 皆為空檔），尚未有任何進場動畫與資料拿取邏輯。使用者要開始嘗試建置 iOS 版本，第一步是把啟動畫面補完：顯示進場動畫，並在啟動時呼叫既有的 `GetConfigurationUseCase` 取得 TMDB configuration（含快取退回邏輯，已由 domain 層處理），做為後續畫面（例如首頁圖片組 URL 組裝）依賴的前置資料。主題與語言切換不在本次範圍內，避免範圍發散。

## What Changes

- 補完 `iosApp/iosApp/Splash/SplashView.swift`：加入進場動畫（Logo 淡入 + 輕微縮放）。
- 補完 `iosApp/iosApp/Splash/SplashViewModel.swift`：啟動時呼叫 `GetConfigurationUseCase`，並依成功/失敗更新 `SplashUiState`（`loading` → `success(data:)` 或 `failure(error:)`）；失敗時提供重試（重新呼叫 UseCase）。
- 視情況調整 `iosApp/iosApp/Splash/SplashUiState.swift`（目前骨架已大致符合需求，若討論中發現需要調整再修改）。
- 在 `shared/app` 的 iosMain `KoinHelper` 新增 `getConfigurationUseCase()` accessor，讓 Swift 端能解析 `GetConfigurationUseCase`（沿用既有 `ios-koin-bridge` 具名 accessor 慣例）。
- 調整 `iosApp/iosApp/iOSApp.swift`：加入 root 狀態切換，App 啟動先顯示 `SplashView`，Configuration 拿取成功後切換到既有的 `MainView`。
- **協作模式（非一般 AI 自動實作 change）**：本次 Swift 程式碼由使用者本人親自撰寫，Claude 的角色是逐步討論設計決策並在使用者實作後進行 review／輔助除錯，**不**由 Claude 直接產出完整 Swift 實作。後續 `flow-apply`／`tasks.md` 執行時應遵循此協作模式，任務項目需拆解成可與使用者逐步討論、確認後再實作的粒度。
- **明確排除**：主題（theme）切換、語言（language）切換，兩者都留待未來獨立 change 處理。

## Capabilities

### New Capabilities
- `ios-splash-screen`：定義 iOS 端 Splash 畫面在啟動時顯示進場動畫、呼叫 `GetConfigurationUseCase` 拿取 configuration 並依結果轉換 UI 狀態的行為規格。

### Modified Capabilities
- `ios-koin-bridge`：`KoinHelper` 新增 `getConfigurationUseCase()` accessor，擴充既有「具名 accessor 曝露 Koin 依賴」規格的涵蓋範圍。

## Impact

- `iosApp`（iosApp/Splash/*.swift）：新增進場動畫與資料拿取邏輯的實際 Swift 實作（使用者親自撰寫）。
- `iosApp`（iosApp/iOSApp.swift）：新增 Splash → Main 的 root 狀態切換邏輯。
- `shared/app`（iosMain）：`KoinHelper.kt` 新增一個 accessor 方法。
- 不涉及 `androidApp`、`core/*`、`shared/domain`（`GetConfigurationUseCase` 簽名不變，直接複用）。
- 不新增第三方依賴，不涉及 `gradle/libs.versions.toml` 或 buildSrc 變更。
- 不處理主題／語言切換，不影響 `shared/datastore` 既有的 `ThemeMode`／`LanguageMode` 相關程式碼。
