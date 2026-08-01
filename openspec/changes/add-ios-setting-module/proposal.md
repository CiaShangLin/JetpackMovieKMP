## Why

iOS 端的設定分頁（`iosApp/iosApp/Setting/SettingView.swift`）目前僅是一個顯示 `main_setting_placeholder` 文字的佔位畫面，尚未提供任何實際功能。Android 端的 `feature:setting` 已完整實作主題模式、語言模式（僅影響 TMDB 內容語言）與開發者資訊功能，且 shared 層（`shared/model`、`shared/datastore`、`shared/data`）已提供跨平台的 `UserData`／`UserDataRepository`／`DatastoreLanguageProvider`，iOS 可直接複用，不需新增任何 shared Kotlin API。現在要補上 iOS 對應的 Swift UI 與行為，讓兩平台的設定功能對齊。

## What Changes

- 新增 iOS `SettingViewModel`（`@Observable @MainActor`），透過 `KoinHelper.shared.userDataRepository()` 取得 shared `UserDataRepository`，訂閱 `userData` Flow 並呼叫 `setThemeMode()`／`setLanguageMode()`。
- 將 `SettingView.swift` 由 placeholder 改為實際設定清單畫面：主題設定、語言設定、開發者資訊三個項目，樣式沿用既有 iOS feature（如 History）畫面的做法。
- 新增主題選擇 UI（LIGHT／DARK／SYSTEM），選擇後透過 `SettingViewModel` 持久化，並在 App 根層級（`MainView`／App 進入點）套用 `.preferredColorScheme`，讓主題套用範圍是整個 App 而非僅設定頁本身。
- 新增語言選擇 UI（SYSTEM_DEFAULT／TRADITIONAL_CHINESE／ENGLISH）。此語言設定 **僅影響 TMDB 內容語言**（透過既有 `DatastoreLanguageProvider` + `SystemLanguage.ios.kt`），不覆蓋 App UI 顯示語言；`SYSTEM_DEFAULT` 選項行為 SHALL 與現有系統語言偵測（`currentSystemLanguageCode()`）一致。此範圍刻意與 `ios-localization` 現有規格（僅依系統語言顯示 UI 文案，不提供 App 內語言切換）保持相容，不修改該規格。
- 新增開發者資訊畫面／Sheet，內容比照 Android（App 名稱「JetpackMovieKMP」、開發者資訊、可點擊 GitHub 連結），文案新增至 `Localizable.xcstrings`（`en` + `zh-Hant`）。
- 依現有 iOS feature 慣例（`ios-movie-history` 已確立的模式），`SettingView` 直接透過 `KoinHelper` 取得 `UserDataRepository`，不由 `MainView`／`MainTab` 逐一轉送依賴。

## Capabilities

### New Capabilities

- `ios-setting-module`：iOS 設定分頁的主題模式／語言模式（僅內容語言）檢視與變更、開發者資訊顯示，以及對應的 `KoinHelper` 取用與主題套用範圍規範。

### Modified Capabilities

（無；`ios-localization`、`ios-koin-bridge`、`kmp-user-preferences-datastore` 等既有 capability 的 requirement 不變，`ios-setting-module` 僅消費既有能力。）

## Impact

- 受影響模組：`iosApp`（`iosApp/iosApp/Setting/`、`iosApp/iosApp/Main/MainView.swift`、`iosApp/iosApp/Localizable.xcstrings`）。
- 不影響：`shared/*`（model／datastore／data／domain／app 皆已具備所需能力，本次不新增 shared Kotlin API 或 expect/actual）、`androidApp`、`core/*`。
- 不新增任何第三方依賴，`buildSrc` 無需變動。
- 本次為手動實作（使用者於 Xcode／Swift 端自行撰寫程式碼），tasks.md 需以清楚、可逐步照做的步驟拆解。
