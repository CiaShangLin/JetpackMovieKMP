## MODIFIED Requirements

### Requirement: Splash 啟動時拿取 configuration

`iosApp` 的 `SplashViewModel` SHALL 在畫面啟動時呼叫 `GetConfigurationUseCase`，並依 `Flow<AppResult<ConfigurationBean>>` 的結果將 `SplashUiState` 轉換為對應狀態。

#### Scenario: Configuration 拿取成功
- **WHEN** `GetConfigurationUseCase` 回傳的 `Flow` 迭代出 `AppResult.Success(configuration)`
- **THEN** `SplashUiState` SHALL 轉為 `success(data:)`，其中 `data` 為該
  `ConfigurationBean`

#### Scenario: Configuration 拿取失敗
- **WHEN** `GetConfigurationUseCase` 回傳的 `Flow` 迭代出 `AppResult.Failure(error)`
- **THEN** `SplashUiState` SHALL 轉為 `failure(debugMessage:)`

### Requirement: Configuration 拿取失敗時提供重試

當 `SplashUiState` 為 `failure(debugMessage:)` 時，`SplashView` SHALL 顯示錯誤文案與一個
重試按鈕；使用者點擊後 SHALL 重新觸發 `GetConfigurationUseCase` 的呼叫流程。
`SplashView` 顯示的錯誤文案 SHALL 為透過 `ios-localization` 的 String Catalog
取得的固定在地化文案，不得直接把 `SplashUiState.failure` 內部的原始技術性錯誤
內容（例如底層 exception message）顯示給使用者。

#### Scenario: 使用者點擊重試按鈕
- **WHEN** `SplashUiState` 為 `failure` 且使用者點擊畫面上的重試按鈕
- **THEN** `SplashUiState` SHALL 重置為 `loading`，並重新呼叫
  `GetConfigurationUseCase` 取得 configuration

#### Scenario: 顯示失敗時的在地化錯誤文案
- **WHEN** `SplashUiState` 轉為 `failure`
- **THEN** `SplashView` SHALL 顯示來自 String Catalog 的固定在地化錯誤文案，
  而非 `SplashUiState.failure` 內部攜帶的原始技術性錯誤內容

### Requirement: Splash 資料層透過 KoinHelper 取得 iOS configuration loader

`SplashViewModel` 取得 `GetConfigurationUseCase` 實例的方式 SHALL 遵循
`ios-koin-bridge` 規格既有的組裝根取得、建構子注入慣例，不得在 `SplashViewModel`
內部直接呼叫 `KoinHelper`。

#### Scenario: 組裝根取得 UseCase 並注入 SplashViewModel
- **WHEN** 建立 `SplashViewModel` 實例
- **THEN** 其初始化方法以參數形式接收 `GetConfigurationUseCase`，該值由呼叫端
  透過 `KoinHelper.shared.getConfigurationUseCase()` 取得後傳入
