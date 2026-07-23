## 1. iosApp

- [x] 1.1 檢查參考素材 `/Users/tsaishanglin/AndroidStudioProjects/JetpackMovieCompose/app/src/main/res/mipmap-*` 與 `mipmap-anydpi-v26`，確認可用的 foreground、legacy icon 與背景色來源。
- [x] 1.2 以 Android adaptive icon 的 foreground 圖與背景色產出 `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png`，圖片尺寸需為 `1024x1024`。
- [x] 1.3 更新或確認 `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/Contents.json` 包含 iOS universal `1024x1024` 且 filename 指向 `app-icon-1024.png` 的條目。
- [x] 1.4 確認 `iosApp/iosApp.xcodeproj/project.pbxproj` 的 Debug / Release build setting 均使用 `ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon`。
- [x] 1.5 檢查產出的 `app-icon-1024.png` 不透明、未預先裁切圓角，且視覺上與 Android launcher logo 一致。
- [x] 1.6 執行 iOS target build 驗證 asset catalog 可編譯，若本機 simulator destination 不固定，先列出可用 destination 後選擇可用裝置執行。
