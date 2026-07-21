# android-app-entry Specification

## Purpose
定義 `androidApp` 進入點（`MainActivity`）重新整合的驗收標準：涵蓋可編譯／可啟動性、啟動畫面（`androidx.core:core-splashscreen`）、Navigation3 導覽骨架，以及 `MainViewModel` 透過 Koin 注入的方式，確保移除舊有 UI 依賴後 App 仍可正常運作。

## Requirements

### Requirement: `androidApp` 進入點 MUST 可正常編譯並啟動

`androidApp` 模組 MUST 在沒有任何 feature module 畫面接回的情況下，仍可透過 `./gradlew :androidApp:assembleDebug` 成功建置，並在裝置／模擬器上啟動 `MainActivity` 而不發生編譯錯誤或執行期 crash。

#### Scenario: Debug 組建成功

- **WHEN** 執行 `./gradlew :androidApp:assembleDebug`
- **THEN** 建置 MUST 成功完成，不得因缺少 `installSplashScreen()`、導覽 API 或 `MainViewModel` 建構失敗而中止

#### Scenario: 冷啟動不 crash

- **WHEN** 使用者在裝置／模擬器上啟動 App
- **THEN** `MainActivity` MUST 成功進入 `onCreate()` 並渲染畫面（Loading／Error／Success 任一狀態），不得拋出未捕捉例外

### Requirement: `MainActivity` MUST 使用 `androidx.core:core-splashscreen` 顯示啟動畫面

`MainActivity` MUST 透過官方 `androidx.core:core-splashscreen` 的 `installSplashScreen()` API 顯示啟動畫面，並在 `MainUiState` 為 `Loading` 時持續顯示，直到設定載入完成。

#### Scenario: 依賴與 theme 設定齊備

- **WHEN** 檢查 `gradle/libs.versions.toml` 與 `androidApp/build.gradle.kts`
- **THEN** MUST 包含 `androidx.core:core-splashscreen` 對應的 catalog alias 與 `implementation` 依賴
- **AND** `AndroidManifest.xml` 的 `MainActivity` `<activity>` 元素 MUST 有 `android:theme` 屬性，指向繼承自 `Theme.SplashScreen` 的 style（例如 `Theme.App.Starting`）
- **AND** 該 style 內引用的所有 resource（例如 `windowSplashScreenBackground` 指向的顏色）MUST 在對應的 `values` resource 檔（例如 `colors.xml`）中已定義，建置時不得出現 resource linking 錯誤

#### Scenario: Splash 隨設定載入狀態收起

- **WHEN** `MainViewModel.configuration` 從 `MainUiState.Loading` 轉為 `MainUiState.Success` 或 `MainUiState.Error`
- **THEN** splash screen MUST 結束顯示，交由對應的 Success／Error 畫面接手

### Requirement: `MainActivity` 主要導覽骨架 MUST 使用 Navigation3

`MainActivity` 的主要導覽骨架 MUST 使用專案既定的 `androidx.navigation3:navigation3-runtime` 與 `androidx.navigation3:navigation3-ui`（`NavBackStack`／`NavDisplay`／entry provider），MUST NOT 依賴 classic `androidx.navigation:navigation-compose`（`NavHostController`／`NavHost`／`rememberNavController`）。

#### Scenario: 不存在 classic Navigation Compose 依賴

- **WHEN** 檢查 `androidApp/build.gradle.kts` 與 `MainActivity.kt` 的 import
- **THEN** MUST NOT 出現 `androidx.navigation:navigation-compose` 依賴或 `androidx.navigation.NavHostController`／`androidx.navigation.compose.NavHost`／`androidx.navigation.compose.rememberNavController` 的 import

#### Scenario: 使用 Navigation3 API 建立導覽骨架

- **WHEN** 檢查 `MainActivity.kt` 的導覽相關實作
- **THEN** MUST 使用 `rememberNavBackStack()` 建立 backstack 並交給 `NavDisplay` 渲染
- **AND** `MainNavItem` 驅動的導覽列點擊事件 MUST 透過操作該 backstack（而非字串路由 `navigate()`）切換畫面

### Requirement: `MainViewModel` MUST 透過 Koin 注入

`MainViewModel` MUST 由 Koin module 提供並透過 Koin API 注入到 `MainActivity`，MUST NOT 使用預設 `by viewModels()`（`SavedStateViewModelFactory`）建立。

#### Scenario: Koin module 提供 MainViewModel

- **WHEN** 檢查 `androidApp` 的 Koin module 定義
- **THEN** MUST 存在提供 `MainViewModel`（含其 `GetConfigurationUseCase`／`UserDataRepository` 依賴）的 module
- **AND** 該 module MUST 在 App 啟動流程中被載入（例如 `JetpackMovieApplication.onCreate()` 呼叫 `loadKoinModules`）

#### Scenario: MainActivity 不使用預設 ViewModelProvider Factory 取得 MainViewModel

- **WHEN** 檢查 `MainActivity.kt` 取得 `MainViewModel` 實例的方式
- **THEN** MUST NOT 使用 `by viewModels()`
- **AND** MUST 使用 Koin 提供的注入方式（例如 `koinViewModel()`）取得實例
