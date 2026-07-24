## MODIFIED Requirements

### Requirement: Configuration 拿取失敗時提供重試

當 `SplashUiState` 為 `failure(error:)` 時，`SplashView` SHALL 顯示錯誤文案與一個
重試按鈕；使用者點擊後 SHALL 重新觸發 `IosConfigurationLoader` 的呼叫流程。
`SplashView` 顯示的錯誤文案 SHALL 為透過 `ios-localization` 的 String Catalog
取得的固定在地化文案，不得直接把 `SplashUiState.failure` 內部的原始技術性錯誤
內容（例如底層 exception message）顯示給使用者。

#### Scenario: 使用者點擊重試按鈕
- **WHEN** `SplashUiState` 為 `failure` 且使用者點擊畫面上的重試按鈕
- **THEN** `SplashUiState` SHALL 重置為 `loading`，並重新呼叫
  `IosConfigurationLoader` 取得 configuration

#### Scenario: 顯示失敗時的在地化錯誤文案
- **WHEN** `SplashUiState` 轉為 `failure`
- **THEN** `SplashView` SHALL 顯示來自 String Catalog 的固定在地化錯誤文案，
  而非 `SplashUiState.failure` 內部攜帶的原始技術性錯誤內容
