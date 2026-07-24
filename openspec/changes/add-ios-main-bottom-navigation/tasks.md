## 1. iosApp - Main 導覽結構

- [ ] 1.1 建立 iOS 主畫面 tab 定義，集中管理首頁、收藏、搜尋、歷史、設定五個 tab 的識別、標題 key 與 SF Symbol。
- [ ] 1.2 將 `MainView` 改為 SwiftUI 原生 `TabView`，並預設選取首頁 tab。
- [ ] 1.3 為五個 tab 建立 placeholder content，每頁顯示可辨識的 placeholder text。
- [ ] 1.4 確認 `iOSApp` 從 `SplashView` 成功後仍切換到新的 `MainView`，不改動 Splash configuration loading 流程。

## 2. iosApp - 在地化

- [ ] 2.1 在 `Localizable.xcstrings` 新增五個 tab 標題文案：首頁、收藏、搜尋、歷史、設定。
- [ ] 2.2 在 `Localizable.xcstrings` 新增五個 placeholder text 文案，並提供 `en` 與 `zh-Hant` 翻譯。
- [ ] 2.3 檢查 `MainView` 與 placeholder view 不直接寫死顯示用字串，所有 UI 文案皆透過 String Catalog key 取得。

## 3. iosApp - 驗證

- [ ] 3.1 執行 iOS Swift 格式檢查或格式化流程，確認本次 Swift 檔案符合專案 code style。
- [ ] 3.2 執行 iOS App 或 shared iOS 相關編譯驗證，確認 `iosApp` 可消費 `Shared` framework 且 `MainView` 可編譯。
- [ ] 3.3 以模擬器或 SwiftUI preview 人工確認 `MainView` 顯示五個底部 tab，且每個 tab 切換後顯示對應 placeholder text。
