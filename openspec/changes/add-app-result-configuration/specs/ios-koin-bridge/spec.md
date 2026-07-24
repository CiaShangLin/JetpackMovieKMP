## MODIFIED Requirements

### Requirement: KoinHelper 具名橋接物件

`shared` 模組的 `iosMain` SHALL 提供一個名為 `KoinHelper` 的 `KoinComponent` object，以具名（非 reified generic）方法暴露 Koin 容器中的依賴，供 Swift 端呼叫，並隨消費端需求持續新增對應的具名 accessor（例如 `userDataRepository()`、`getMovieDetailUseCase()`、`getConfigurationUseCase()`）。`getConfigurationUseCase()` 回傳的 `GetConfigurationUseCase` 呼叫結果為 `Flow<AppResult<ConfigurationBean>>`，Swift 端 SHALL 直接消費此型別，不再透過額外的 iOS 專用 wrapper（例如已移除的 `IosConfigurationLoader`）轉換。

#### Scenario: Swift 端呼叫具名方法取得依賴
- **WHEN** `iosApp` 已呼叫 `doInitKoinIos` 啟動 Koin 容器之後，Swift 端呼叫
  `KoinHelper.shared.userDataRepository()`
- **THEN** 回傳一個非 null 的 `UserDataRepository` 實例

#### Scenario: Swift 端呼叫 getConfigurationUseCase 取得依賴
- **WHEN** `iosApp` 已呼叫 `doInitKoinIos` 啟動 Koin 容器之後，Swift 端呼叫
  `KoinHelper.shared.getConfigurationUseCase()`
- **THEN** 回傳一個非 null 的 `GetConfigurationUseCase` 實例，其 `invoke()` 回傳
  `Flow<AppResult<ConfigurationBean>>`

#### Scenario: KoinHelper 不再提供 getConfigurationLoader
- **WHEN** 檢查 `KoinHelper.kt` 的具名 accessor 清單
- **THEN** 不存在 `getConfigurationLoader()` 方法，Swift 端改為直接呼叫
  `getConfigurationUseCase()` 取得 `Flow<AppResult<ConfigurationBean>>`
