## MODIFIED Requirements

### Requirement: `MainActivity` MUST 使用 `androidx.core:core-splashscreen` 顯示啟動畫面

`MainActivity` MUST 透過官方 `androidx.core:core-splashscreen` 的 `installSplashScreen()` API 顯示啟動畫面，並在 `MainViewModel.configuration`（型別為 `shared/common` 的 `UiState<ConfigurationBean>`）為 `UiState.Loading` 時持續顯示，直到設定載入完成。

#### Scenario: 依賴與 theme 設定齊備

- **WHEN** 檢查 `gradle/libs.versions.toml` 與 `androidApp/build.gradle.kts`
- **THEN** MUST 包含 `androidx.core:core-splashscreen` 對應的 catalog alias 與 `implementation` 依賴
- **AND** `AndroidManifest.xml` 的 `MainActivity` `<activity>` 元素 MUST 有 `android:theme` 屬性，指向繼承自 `Theme.SplashScreen` 的 style（例如 `Theme.App.Starting`）
- **AND** 該 style 內引用的所有 resource（例如 `windowSplashScreenBackground` 指向的顏色）MUST 在對應的 `values` resource 檔（例如 `colors.xml`）中已定義，建置時不得出現 resource linking 錯誤

#### Scenario: Splash 隨設定載入狀態收起

- **WHEN** `MainViewModel.configuration` 從 `UiState.Loading` 轉為 `UiState.Success` 或 `UiState.Error`
- **THEN** splash screen MUST 結束顯示，交由對應的 Success／Error 畫面接手
