## Why

iOS 端（`iosApp`）目前完全沒有在地化（Localization）基礎設施：Xcode 專案的
`knownRegions` 只註冊了 `en`，沒有任何 `Localizable.strings` 或 String Catalog
檔案。畫面文案（例如 Splash 的「載入中」「準備完成」「重試」與錯誤訊息）都是直接寫死
在 Swift 程式碼裡的中文字串，其中 `SplashViewModel` 甚至把錯誤訊息文字也寫死在
ViewModel 內，跟畫面（View）耦合。

反觀 shared 層與 Android 端已有一套語言設定機制（`LanguageMode` / `DatastoreLanguageProvider`），
TMDB API 呼叫也已經透過共用的 `initKoin()` 正確帶上對應語言參數，iOS 端不需要重造這段
邏輯。真正缺的是「iOS UI 文案本身的多語系能力」。現在導入可以避免未來畫面越做越多、
文案分散各處，屆時要再抽成在地化字串的成本更高。

## What Changes

- 在 iOS 專案導入 Xcode **String Catalog**（`.xcstrings`）作為 UI 字串在地化機制，
  取代目前寫死的中文字串。
- Xcode 專案設定新增 `zh-Hant`（繁體中文）語系，`en` 維持為 development region /
  base language；`knownRegions` 同步更新。
- 抽出目前已知的寫死文案，改為透過 String Catalog 讀取，目前範圍涵蓋
  `SplashView`（「載入中」「準備完成」「重試」等）與 `SplashViewModel` 內的錯誤訊息。
- **BREAKING**（僅限 iOS 內部型別，尚未有其他呼叫端）：`SplashUiState.failure(error:)`
  的語意調整——`SplashViewModel` 不再直接持有／回傳要顯示給使用者的中文字串，
  只回傳失敗狀態；實際顯示給使用者的文案改由 `SplashView` 依 String Catalog 決定，
  真正的技術性錯誤內容（例如底層 exception message）不再直接顯示在畫面上。
- 本次**只**支援「畫面文案跟隨 iOS 系統語言自動顯示對應翻譯」（Xcode 原生行為），
  **不**實作「App 內主動切換語言」功能（不覆蓋系統語言），也不會在此 change 內
  串接 shared 層的 `LanguageMode` / `UserDataRepository`。App 內語言切換功能留待
  後續 change 另行規劃與提案。

## Capabilities

### New Capabilities
- `ios-localization`：iOS 端 UI 文案在地化基礎設施，包含 String Catalog 設定、
  支援語系（`zh-Hant`、`en`）、以及「畫面文案跟隨系統語言顯示」的行為規範。

### Modified Capabilities
- `ios-splash-screen`：「Configuration 拿取失敗時提供重試」需求的顯示行為調整——
  `SplashView` 顯示的錯誤文案不再是任意字串（可能來自底層 exception message 或
  寫死中文），而是固定透過 `ios-localization` 提供的在地化字串顯示；技術性錯誤內容
  不對使用者顯示。

## Impact

- **受影響模組**：`iosApp`（Xcode 專案設定 `project.pbxproj`、`Info.plist`、
  `iosApp/iosApp/Splash/SplashView.swift`、`iosApp/iosApp/Splash/SplashViewModel.swift`、
  `iosApp/iosApp/Splash/SplashUiState.swift`）。
- 不涉及 `shared/*` 任何模組——TMDB API 的 `language` 參數與 `LanguageMode` 設定機制
  維持現狀不變。
- 不涉及 Android 端（`androidApp`、`core/*`）。
- 新增依賴：無（String Catalog 為 Xcode 內建功能，不需額外套件）。
