## ADDED Requirements

### Requirement: iOS 專案支援的語系
`iosApp` SHALL 支援 `zh-Hant`（繁體中文）與 `en`（英文）兩個語系；`en` 為
development region / base language，`zh-Hant` 為額外語系，兩者皆須列於 Xcode
專案的 `knownRegions` 設定中。

#### Scenario: 系統語言為繁體中文
- **WHEN** 使用者裝置系統語言設定為繁體中文（zh-Hant）
- **THEN** iOS App 畫面文案 SHALL 顯示繁體中文翻譯

#### Scenario: 系統語言非上述兩者之一
- **WHEN** 使用者裝置系統語言不是 `zh-Hant` 也不是 `en`
- **THEN** iOS App 畫面文案 SHALL fallback 顯示為 `en`（base language）翻譯

### Requirement: UI 文案透過 String Catalog 集中管理
iOS App 的畫面文案 SHALL 透過 String Catalog（`.xcstrings`）取得，不得以字面
字串常數的形式寫死於 Swift 原始碼中。

#### Scenario: Splash 畫面文案透過 String Catalog 顯示
- **WHEN** `SplashView` 顯示「載入中」「準備完成」「重試」等文案
- **THEN** 該文案內容 SHALL 來自 String Catalog，而非寫死於 Swift 原始碼的字串常數

### Requirement: 本次範圍不支援 App 內語言切換
iOS App SHALL 僅依系統語言設定顯示對應翻譯，不提供任何可覆蓋系統語言的 App 內
切換功能；亦不讀取或訂閱 shared 層 `LanguageMode` 設定來決定 UI 顯示語言。

#### Scenario: 使用者變更系統語言後開啟 App
- **WHEN** 使用者在裝置系統設定變更語言後重新開啟 iOS App
- **THEN** iOS App SHALL 依新的系統語言顯示對應翻譯，且沒有任何 App 內選項可以
  獨立於系統語言另外指定顯示語言
