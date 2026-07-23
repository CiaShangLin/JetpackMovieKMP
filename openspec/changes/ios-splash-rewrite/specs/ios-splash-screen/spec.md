## ADDED Requirements

### Requirement: Splash 啟動時拿取 configuration
`iosApp` 的 `SplashViewModel` SHALL 在畫面啟動時呼叫 `GetConfigurationUseCase`，
並依 `Flow<Result<ConfigurationBean>>` 的結果將 `SplashUiState` 轉換為對應狀態。

#### Scenario: Configuration 拿取成功
- **WHEN** `GetConfigurationUseCase` 的 Flow 迭代出可轉型為 `ConfigurationBean`
  的結果
- **THEN** `SplashUiState` SHALL 轉為 `success(data:)`，其中 `data` 為該
  `ConfigurationBean`

#### Scenario: Configuration 拿取失敗
- **WHEN** `GetConfigurationUseCase` 的 Flow 迭代出無法轉型為 `ConfigurationBean`
  的結果（型別已被 Swift 端 type erase）
- **THEN** `SplashUiState` SHALL 轉為 `failure(error:)`

### Requirement: Splash 顯示進場動畫
`SplashView` SHALL 在畫面顯示時播放進場動畫；具體動畫效果與時長由實作階段與使用者
逐步討論後決定，本要求只約束「必須有進場動畫」這個行為存在。

#### Scenario: 畫面出現時播放動畫
- **WHEN** `SplashView` 被顯示
- **THEN** 畫面 SHALL 播放一段進場動畫，而非直接以無動畫狀態顯示內容

### Requirement: Configuration 拿取失敗時提供重試

當 `SplashUiState` 為 `failure(error:)` 時，`SplashView` SHALL 顯示錯誤文案與一個
重試按鈕；使用者點擊後 SHALL 重新觸發 `GetConfigurationUseCase` 的呼叫流程。

#### Scenario: 使用者點擊重試按鈕
- **WHEN** `SplashUiState` 為 `failure` 且使用者點擊畫面上的重試按鈕
- **THEN** `SplashUiState` SHALL 重置為 `loading`，並重新呼叫
  `GetConfigurationUseCase` 取得 configuration

### Requirement: Splash 成功後導轉至既有主畫面

Configuration 拿取成功後，`iosApp` SHALL 從 `SplashView` 切換到既有的 `MainView`，
不停留在 Splash 畫面。

#### Scenario: 成功後切換到 MainView
- **WHEN** `SplashUiState` 轉為 `success(data:)`
- **THEN** App 的 root 畫面 SHALL 由 `SplashView` 切換為 `MainView`

### Requirement: Splash 資料層透過 KoinHelper 取得 UseCase
`SplashViewModel` 取得 `GetConfigurationUseCase` 實例的方式 SHALL 遵循
`ios-koin-bridge` 規格既有的組裝根取得、建構子注入慣例，不得在 `SplashViewModel`
內部直接呼叫 `KoinHelper`。

#### Scenario: 組裝根取得 UseCase 並注入 SplashViewModel
- **WHEN** 建立 `SplashViewModel` 實例
- **THEN** 其初始化方法以參數形式接收 `GetConfigurationUseCase`，該值由呼叫端
  透過 `KoinHelper.shared.getConfigurationUseCase()` 取得後傳入
