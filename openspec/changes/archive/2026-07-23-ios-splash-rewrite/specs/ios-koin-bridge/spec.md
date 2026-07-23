## MODIFIED Requirements

### Requirement: KoinHelper 具名橋接物件
`shared` 模組的 `iosMain` SHALL 提供一個名為 `KoinHelper` 的 `KoinComponent` object，
以具名（非 reified generic）方法暴露 Koin 容器中的依賴，供 Swift 端呼叫，並隨消費端
需求持續新增對應的具名 accessor（例如 `userDataRepository()`、
`getMovieDetailUseCase()`、`getConfigurationUseCase()`、`getConfigurationLoader()`）。

#### Scenario: Swift 端呼叫具名方法取得依賴
- **WHEN** `iosApp` 已呼叫 `doInitKoinIos` 啟動 Koin 容器之後，Swift 端呼叫
  `KoinHelper.shared.userDataRepository()`
- **THEN** 回傳一個非 null 的 `UserDataRepository` 實例

#### Scenario: Swift 端呼叫 getConfigurationUseCase 取得依賴
- **WHEN** `iosApp` 已呼叫 `doInitKoinIos` 啟動 Koin 容器之後，Swift 端呼叫
  `KoinHelper.shared.getConfigurationUseCase()`
- **THEN** 回傳一個非 null 的 `GetConfigurationUseCase` 實例

#### Scenario: Swift 端呼叫 getConfigurationLoader 取得 iOS 友善橋接物件
- **WHEN** `iosApp` 已呼叫 `doInitKoinIos` 啟動 Koin 容器之後，Swift 端呼叫
  `KoinHelper.shared.getConfigurationLoader()`
- **THEN** 回傳一個非 null 的 `IosConfigurationLoader` 實例
