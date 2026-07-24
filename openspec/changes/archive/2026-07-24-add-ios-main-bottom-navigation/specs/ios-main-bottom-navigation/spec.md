## ADDED Requirements

### Requirement: iOS 主畫面使用原生底部導覽

`iosApp` 的 `MainView` SHALL 使用 SwiftUI 原生 `TabView` 呈現底部導覽列，作為 Splash 成功後的主要畫面入口。

#### Scenario: Splash 成功後顯示主畫面底部導覽
- **WHEN** `SplashView` 載入 configuration 成功並切換到 `MainView`
- **THEN** `MainView` SHALL 顯示 SwiftUI 原生底部導覽列

### Requirement: 底部導覽包含五個固定頁面

iOS 主畫面底部導覽 SHALL 包含五個固定 tab，依序為首頁、收藏、搜尋、歷史、設定。

#### Scenario: 使用者進入 MainView
- **WHEN** 使用者看到 `MainView`
- **THEN** 底部導覽 SHALL 顯示首頁、收藏、搜尋、歷史、設定五個 tab

#### Scenario: 預設選取首頁
- **WHEN** `MainView` 首次顯示
- **THEN** 預設選取的 tab SHALL 為首頁

### Requirement: 每個 tab 顯示 placeholder text

本階段五個 tab 的內容 SHALL 顯示可辨識的 placeholder text，不串接電影列表、收藏、搜尋、歷史或設定資料流。

#### Scenario: 切換到首頁 tab
- **WHEN** 使用者選取首頁 tab
- **THEN** 畫面內容 SHALL 顯示首頁 placeholder text

#### Scenario: 切換到收藏 tab
- **WHEN** 使用者選取收藏 tab
- **THEN** 畫面內容 SHALL 顯示收藏 placeholder text

#### Scenario: 切換到搜尋 tab
- **WHEN** 使用者選取搜尋 tab
- **THEN** 畫面內容 SHALL 顯示搜尋 placeholder text

#### Scenario: 切換到歷史 tab
- **WHEN** 使用者選取歷史 tab
- **THEN** 畫面內容 SHALL 顯示歷史 placeholder text

#### Scenario: 切換到設定 tab
- **WHEN** 使用者選取設定 tab
- **THEN** 畫面內容 SHALL 顯示設定 placeholder text

### Requirement: 導覽與 placeholder 文案使用 String Catalog

底部導覽 tab 標題與 placeholder text SHALL 透過 `Localizable.xcstrings` 取得，不得以顯示用字面字串寫死在 Swift view 中。

#### Scenario: 系統語言為繁體中文
- **WHEN** 使用者裝置系統語言為 `zh-Hant`
- **THEN** 底部導覽與 placeholder text SHALL 顯示繁體中文文案

#### Scenario: 系統語言為英文
- **WHEN** 使用者裝置系統語言為 `en`
- **THEN** 底部導覽與 placeholder text SHALL 顯示英文文案
