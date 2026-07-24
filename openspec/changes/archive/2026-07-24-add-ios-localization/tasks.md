## 1. iosApp：Xcode 專案語系設定

- [x] 1.1 開啟 `iosApp.xcodeproj`，在 Project 設定的 Info 分頁新增 `Chinese, Traditional`
      （`zh-Hant`）語系，確認 `en` 仍為 development region / base language。
- [x] 1.2 確認 `project.pbxproj` 的 `knownRegions` 已同時包含 `en` 與 `zh-Hant`。
- [x] 1.3 視需要於 `Info.plist` 補上 `CFBundleLocalizations`（`en`、`zh-Hant`），
      確保 App 送審／建置時語系資訊完整。（確認不需要：5.1/5.2 模擬器驗證中
      英文切換皆正確辨識，Xcode 依 `knownRegions` 與在地化資源自動產生即可，
      不需手動補此 key。）

## 2. iosApp：建立 String Catalog 並登錄文案

- [x] 2.1 在 `iosApp/iosApp` 底下新增 `Localizable.xcstrings` String Catalog 檔案。
- [x] 2.2 盤點目前寫死文案並登錄為 key（含中文／英文兩個翻譯）：
      - `SplashView.swift:43`「載入中」→ `splash_loading`
      - `SplashView.swift:45`「準備完成」→ `splash_ready`
      - `SplashView.swift:52`「重試」→ `splash_retry_button`
      - Splash 讀取失敗時要顯示的固定錯誤文案 → `splash_error_message`
- [x] 2.3 確認 Xcode 對這些 key 的翻譯狀態顯示為已完成（不是 New / 未翻譯）。

## 3. iosApp：調整 Splash 錯誤狀態設計

- [x] 3.1 依 `design.md` 決策 3，調整 `SplashUiState.failure(error:)` 的語意，
      讓 `error` 不再是要直接顯示給使用者的文字（可保留技術性內容供除錯／log，
      但不做為畫面顯示來源）。（改名為 `failure(debugMessage:)`）
- [x] 3.2 修改 `SplashViewModel.loadConfiguration()`，移除內部寫死的中文
      fallback 錯誤字串（`SplashViewModel.swift:36`、`:40`），改為回傳調整後的
      失敗狀態。
- [x] 3.3 確認 `SplashViewModel.retry()` 行為不受影響，重試流程維持原本邏輯。

## 4. iosApp：SplashView 改用 String Catalog

- [x] 4.1 修改 `SplashView.swift`，把步驟 2.2 盤點的寫死字串改為讀取
      String Catalog（例如透過 `String(localized:)` 或對應的 SwiftUI
      `Text(_:)` 在地化 initializer）。
- [x] 4.2 失敗狀態下顯示的錯誤文案改為固定的在地化字串，不顯示
      `SplashUiState.failure` 內部的原始技術性錯誤內容。
- [x] 4.3 確認重試按鈕文案（「重試」）同樣透過 String Catalog 顯示。

## 5. 驗證

- [x] 5.1 iOS 模擬器語言切換為「繁體中文」，重新啟動 App，確認 Splash 畫面
      三段文案（載入中／準備完成／重試）與失敗錯誤文案皆正確顯示繁體中文。
- [x] 5.2 iOS 模擬器語言切換為「英文」，重複 5.1 驗證步驟，確認正確顯示英文。
- [x] 5.3 手動模擬 `IosConfigurationLoader` 失敗情境（例如中斷網路），確認畫面
      顯示的是固定在地化錯誤文案，而非原始 exception message 或空白內容。
- [x] 5.4 執行 `./gradlew :shared:app:iosSimulatorArm64Test`（或專案既有的 iOS
      測試指令）確認 shared 層未被本次變更影響、既有測試維持綠燈。（BUILD SUCCESSFUL）
- [x] 5.5 執行 `ktlintCheck` 確認未觸碰 Kotlin 檔案格式（本次僅涉及 Swift／
      Xcode 設定，預期無需變動）。（BUILD SUCCESSFUL）
