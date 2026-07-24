## Why

iOS App 目前從 Splash 進入後的 `MainView` 仍是暫時性的文字畫面，缺少與 Android 主畫面一致的主要導覽結構。現在需要先建立首頁骨架，讓後續首頁、收藏、搜尋、歷史與設定功能能依固定入口逐步導入。

## What Changes

- 將 iOS `MainView` 規劃為 SwiftUI 原生 `TabView` 主畫面。
- 新增五個底部導覽項目：首頁、收藏、搜尋、歷史、設定。
- 第一階段五個頁面皆顯示 placeholder text，不在本次串接首頁電影列表或其他實際功能資料流。
- 導覽列與 placeholder 文案需透過 `Localizable.xcstrings` 管理，支援既有 `en` 與 `zh-Hant`。
- 不新增第三方依賴，不調整 shared/domain/data/network API。

## Capabilities

### New Capabilities

- `ios-main-bottom-navigation`: 定義 iOS 主畫面底部導覽列、五個 tab 入口與 placeholder 頁面行為。

### Modified Capabilities

- 無。

## Impact

- 受影響 module：`iosApp`
- 主要檔案範圍：`iosApp/iosApp/Main/*`、`iosApp/iosApp/Home/*`、`iosApp/iosApp/Localizable.xcstrings`
- 不影響 module：`shared/*`、`androidApp`、`core/*`
- 不新增依賴；不需修改 `gradle/libs.versions.toml` 或 Xcode package / framework 設定。
