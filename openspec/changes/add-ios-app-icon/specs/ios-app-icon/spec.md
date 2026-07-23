## ADDED Requirements

### Requirement: iOS App 使用 AppIcon asset catalog
`iosApp` SHALL 使用 `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset` 作為主要 App Icon 來源，且 target build setting SHALL 指向 `AppIcon`。

#### Scenario: Target 指向 AppIcon
- **WHEN** 檢查 `iosApp/iosApp.xcodeproj/project.pbxproj`
- **THEN** Debug 與 Release 設定 SHALL 包含 `ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon`

### Requirement: AppIcon 提供 1024x1024 圖片
`AppIcon.appiconset` SHALL 提供一張可供 iOS/iPadOS single-size app icon 使用的 `1024x1024` PNG 圖片。

#### Scenario: 1024 圖片存在於 appiconset
- **WHEN** 檢查 `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset`
- **THEN** 該目錄 SHALL 包含 `app-icon-1024.png`

#### Scenario: Contents.json 指向 1024 圖片
- **WHEN** 檢查 `AppIcon.appiconset/Contents.json`
- **THEN** `images` SHALL 包含 `platform` 為 `ios`、`idiom` 為 `universal`、`size` 為 `1024x1024` 且 `filename` 為 `app-icon-1024.png` 的條目

### Requirement: iOS App Icon 沿用 Android 參考 logo
產出的 iOS App Icon SHALL 以參考專案 Android launcher logo 為視覺來源，保持與 Android App 一致的品牌識別。

#### Scenario: 使用 Android adaptive icon 素材
- **WHEN** 產生 `app-icon-1024.png`
- **THEN** 產出流程 SHALL 以 Android `ic_launcher_foreground.png` 搭配 `ic_launcher_background` 色彩作為主要來源

### Requirement: iOS App Icon 可編譯
`iosApp` SHALL 能在包含 App Icon asset 後通過 iOS target 的編譯驗證。

#### Scenario: iOS target build 成功
- **WHEN** 執行 iOS App 的 Xcode build 或等效 Gradle/Xcode 驗證流程
- **THEN** build SHALL 成功，且不得出現 App Icon asset catalog 缺圖或格式錯誤
