## Context

`iosApp` 已有 `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset`，且 Xcode target 的 Debug / Release build setting 已設定 `ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon`。目前 `AppIcon.appiconset` 只有 `app-icon-1024.png` 與 iOS universal single-size 結構，符合 Xcode 新專案可由單張高解析圖產生 iOS/iPadOS icon 變體的做法。

參考 Android 專案提供的素材位於：

- `/Users/tsaishanglin/AndroidStudioProjects/JetpackMovieCompose/app/src/main/res/mipmap-mdpi`
- `/Users/tsaishanglin/AndroidStudioProjects/JetpackMovieCompose/app/src/main/res/mipmap-hdpi`
- `/Users/tsaishanglin/AndroidStudioProjects/JetpackMovieCompose/app/src/main/res/mipmap-xhdpi`
- `/Users/tsaishanglin/AndroidStudioProjects/JetpackMovieCompose/app/src/main/res/mipmap-xxhdpi`
- `/Users/tsaishanglin/AndroidStudioProjects/JetpackMovieCompose/app/src/main/res/mipmap-xxxhdpi`
- `/Users/tsaishanglin/AndroidStudioProjects/JetpackMovieCompose/app/src/main/res/mipmap-anydpi-v26`

其中 adaptive icon 使用 `ic_launcher_background`（目前為 `#FFFFFF`）與 `ic_launcher_foreground.png` 組成；最大 foreground PNG 為 xxxhdpi `432x432`。

## Goals / Non-Goals

**Goals:**

- 讓 iOS App 在 Home Screen、Settings、Search 與 App Store icon 欄位使用專案 logo。
- 沿用 Android 參考素材產出 iOS `1024x1024` PNG。
- 維持 Xcode asset catalog 既有 `AppIcon` 命名與 target build setting。
- 用編譯驗證確認 asset catalog 可被 iOS target 使用。

**Non-Goals:**

- 不新增 alternate app icon。
- 不更動 iOS Splash 畫面或 runtime UI。
- 不調整 Android icon。
- 不新增第三方圖片處理依賴到專案原始碼。

## Decisions

### 採用 iOS single-size AppIcon

決策：維持 `AppIcon.appiconset` 的 single-size universal 結構，提供 `1024x1024` 的 `app-icon-1024.png`，由 Xcode 產生各 iOS/iPadOS 使用情境所需變體。

理由：這符合目前 Xcode asset catalog 的既有結構，也避免手動維護多個尺寸造成錯誤。Apple 文件指出 iOS/iPadOS 可從單張 `1024x1024` 圖產生 icon variations；若未來需要針對小尺寸做不同細節，再改成 All Sizes。

替代方案：手動產生所有 iPhone / iPad icon 尺寸並列入 `Contents.json`。此方案較繁瑣，且目前沒有需要針對不同尺寸客製細節的需求。

### 用 Android adaptive icon 組合產出 iOS icon

決策：以 `mipmap-xxxhdpi/ic_launcher_foreground.png` 搭配 `values/ic_launcher_background.xml` 的背景色產出 `1024x1024` 方形 PNG；必要時用既有 `ic_launcher.png` / `ic_launcher_round.png` 作為視覺參考。

理由：Android adaptive icon 已表達品牌 logo 的前景與背景設定，比直接放大 `192x192` legacy icon 更接近原始設計。iOS icon 需為方形素材，系統會套用圓角遮罩，產出的 PNG 不應預先裁成圓形。

替代方案：直接使用 `mipmap-xxxhdpi/ic_launcher.png` 放大至 `1024x1024`。此方案來源解析度較低，且可能包含 Android legacy icon 的既有縮放結果。

### 不修改 MVVM / KMP 架構

決策：此變更只影響 `iosApp` asset catalog，不涉及 MVVM、MVI、Repository、Use Case、Koin 或 shared module 邏輯。

理由：App Icon 是 build-time asset 設定，不屬於執行期 UI 或 business logic。偏離既有架構模式沒有必要。

## Risks / Trade-offs

- Android 來源 foreground 最大只有 `432x432`，放大到 `1024x1024` 可能有銳利度不足風險 → 實作時需實際檢查產出圖；若品質不足，應改找更高解析度原始 logo。
- iOS App Store icon 不應包含不符合上架規範的透明背景或預先圓角 → 產出 `app-icon-1024.png` 時使用不透明背景，並保留方形邊界。
- Xcode 版本差異可能對 single-size / dark / tinted icon 欄位有不同顯示方式 → 以目前專案既有 `Contents.json` 與 build setting 為準，驗證時執行 iOS target build。
