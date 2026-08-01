## ADDED Requirements

### Requirement: iOS 設定頁 SHALL 直接透過 KoinHelper 取得 shared 依賴

`SettingView` SHALL 在 `init()` 中直接呼叫 `KoinHelper.shared.userDataRepository()` 取得 `UserDataRepository`，並以此建立 `SettingViewModel`；`MainView`／`MainTab` SHALL NOT 為此 feature 新增依賴轉送參數。

#### Scenario: SettingView 直接解析依賴

- **WHEN** iOS Koin 已透過 `doInitKoinIos` 初始化後，使用者切換至設定 tab
- **THEN** `SettingView` SHALL 能建立出可用的 `SettingViewModel`，且不需要 `MainView`／`MainTab` 傳入任何 shared 依賴參數

### Requirement: iOS 設定頁 SHALL 反映目前的主題模式與語言模式

`SettingViewModel` SHALL 透過 `UserDataRepository.userData` 觀察使用者偏好設定，並以可觀察的狀態提供給 `SettingView`。畫面 SHALL 顯示目前 `ThemeMode`（LIGHT／DARK／SYSTEM）與 `LanguageMode`（SYSTEM_DEFAULT／TRADITIONAL_CHINESE／ENGLISH）對應的在地化文案。

#### Scenario: 顯示目前主題與語言設定

- **WHEN** `userData` 發出含特定 `themeMode`／`languageMode` 的資料
- **THEN** 設定頁 SHALL 顯示對應該 `themeMode` 的主題文案與對應該 `languageMode` 的語言文案

### Requirement: 使用者 SHALL 能透過設定頁變更主題模式，且套用範圍為整個 App

設定頁 SHALL 提供主題選擇（LIGHT／DARK／SYSTEM 三個選項）。使用者選擇後，`SettingViewModel` SHALL 呼叫 `UserDataRepository.setThemeMode()` 持久化選擇。App 進入點（`IosApp`）SHALL 觀察同一個 `userData` Flow，並將對應的 `preferredColorScheme` 套用於 `WindowGroup` 內容，使主題變更同時影響 Splash 與所有分頁，而非僅設定頁本身。

#### Scenario: 選擇新主題後持久化並套用全 App

- **WHEN** 使用者在主題選擇 UI 點擊一個與目前不同的主題選項
- **THEN** `SettingViewModel` SHALL 呼叫 `setThemeMode()` 一次並傳入使用者選擇的 `ThemeMode`
- **AND** App 的 `preferredColorScheme` SHALL 在下一次 `userData` emission 後更新為對應主題（`SYSTEM` 對應交還系統決定，不強制指定 colorScheme）

### Requirement: 使用者 SHALL 能透過設定頁變更語言模式，但範圍僅限內容語言

設定頁 SHALL 提供語言選擇（SYSTEM_DEFAULT／TRADITIONAL_CHINESE／ENGLISH 三個選項）。使用者選擇後，`SettingViewModel` SHALL 呼叫 `UserDataRepository.setLanguageMode()` 持久化選擇。此設定 SHALL 僅影響 TMDB 內容語言（透過既有 `DatastoreLanguageProvider`），SHALL NOT 覆蓋 App UI 顯示語言或影響 `Localizable.xcstrings` 的文案語言選擇；`SYSTEM_DEFAULT` 選項 SHALL 沿用既有 `currentSystemLanguageCode()` 系統語言偵測行為，不需 iOS 端額外實作偵測邏輯。

#### Scenario: 選擇新語言後持久化且不影響 UI 顯示語言

- **WHEN** 使用者在語言選擇 UI 點擊一個與目前不同的語言選項
- **THEN** `SettingViewModel` SHALL 呼叫 `setLanguageMode()` 一次並傳入使用者選擇的 `LanguageMode`
- **AND** App 的 UI 文案顯示語言 SHALL NOT 因此改變，仍完全依系統語言決定

#### Scenario: 選擇 SYSTEM_DEFAULT 時內容語言隨系統語言變化

- **WHEN** 使用者選擇 `SYSTEM_DEFAULT`，且裝置系統語言之後被使用者變更
- **THEN** TMDB 內容語言 SHALL 於下次請求時反映新的系統語言（透過既有 `currentSystemLanguageCode()` 行為），不需要本次新增任何偵測程式碼

### Requirement: 設定頁 SHALL 提供開發者資訊

設定頁 SHALL 提供開發者資訊項目，點擊後顯示內容，內容 SHALL 包含 App 名稱「JetpackMovieKMP」、開發者資訊與可點擊的 GitHub 連結。

#### Scenario: 顯示開發者資訊

- **WHEN** 使用者點擊設定頁的開發者資訊項目
- **THEN** SHALL 顯示開發者資訊內容，且內容 SHALL 顯示「JetpackMovieKMP」
- **AND** GitHub 連結 SHALL 可點擊並開啟對應網址

### Requirement: 設定頁文案 SHALL 透過 String Catalog 管理

設定頁新增的所有文案 SHALL 透過 `Localizable.xcstrings` 取得，並提供 `en` 與 `zh-Hant` 兩組翻譯，不得以字面字串常數寫死於 Swift 原始碼中。

#### Scenario: 設定頁文案透過 String Catalog 顯示

- **WHEN** 設定頁顯示主題、語言、開發者資訊相關文案
- **THEN** 該文案內容 SHALL 來自 `Localizable.xcstrings`，且 `en`／`zh-Hant` 兩個語系 SHALL 都有對應翻譯
