## Why

iOS 端目前缺少完整的 App Icon 設定，會讓 App 在模擬器、實機與發佈流程中顯示預設或不完整圖示。這次需求要沿用既有 Android App logo 素材，讓 iOS App 也具備一致的品牌識別。

## What Changes

- 將參考專案 `JetpackMovieCompose` 的 mipmap logo 素材轉換為 iOS `AppIcon.appiconset` 可使用的 PNG 圖片。
- 補齊 `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/Contents.json` 需要的 icon 條目與對應檔案。
- 確認 iOS target 使用 `ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon`，避免額外修改 `Info.plist` 或 target 設定。
- 驗證 Xcode asset catalog 能被 iOS App 編譯使用。

## Capabilities

### New Capabilities

- `ios-app-icon`: 定義 iOS App Icon 的來源素材、asset catalog 結構與編譯驗收要求。

### Modified Capabilities

- 無。

## Impact

- 受影響 module：`iosApp`
- 受影響檔案：
  - `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/`
  - `iosApp/iosApp.xcodeproj/project.pbxproj`（僅確認既有設定；預期不需修改）
- 不新增 Gradle、Swift Package 或 CocoaPods 依賴。
- 不影響 `shared/*`、`androidApp`、`core/*` 模組。
