## MODIFIED Requirements

### Requirement: Configuration UseCase 具備快取退回機制

`shared/domain` 的 `commonMain` SHALL 提供 `GetConfigurationUseCase`，整合 `shared:data` 的 `MovieRepository.getConfiguration()` 與 `UserDataRepository`，回傳型別為 `Flow<AppResult<ConfigurationBean>>`：API 成功時寫入本地快取並回傳 `AppResult.Success`；API 失敗時若本地有快取則回傳快取內容並視為 `AppResult.Success`，皆無快取才回傳 `AppResult.Failure`（原始錯誤轉換後的 `AppError`）。

#### Scenario: API 呼叫成功時寫入快取並回傳成功

- **WHEN** 呼叫 `GetConfigurationUseCase()` 且 `MovieRepository.getConfiguration()` 回傳 `AppResult.Success(configuration)`
- **THEN** `UserDataRepository.setConfiguration(configuration)` 被呼叫，且回傳的 `Flow` emit `AppResult.Success(configuration)`

#### Scenario: API 呼叫失敗但本地有快取時退回快取

- **WHEN** 呼叫 `GetConfigurationUseCase()` 且 `MovieRepository.getConfiguration()` 回傳 `AppResult.Failure(...)`，`UserDataRepository.userData` 目前的 configuration 不為 null
- **THEN** 回傳的 `Flow` emit `AppResult.Success(cachedConfiguration)`

#### Scenario: API 呼叫失敗且本地無快取時回傳原始錯誤

- **WHEN** 呼叫 `GetConfigurationUseCase()` 且 `MovieRepository.getConfiguration()` 回傳 `AppResult.Failure(error)`，`UserDataRepository.userData` 目前的 configuration 為 null
- **THEN** 回傳的 `Flow` emit `AppResult.Failure(error)`
