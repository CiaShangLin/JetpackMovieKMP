# ios-koin-bridge Specification

## Purpose
定義 `shared/app` 在 iOS 層提供的 Koin 依賴橋接能力，讓 Swift 端能透過具名方法取得
Koin 容器中的共用依賴，並維持由組裝根以建構子注入往下傳遞的消費慣例。
## Requirements
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

### Requirement: KoinHelper 解析行為需有自動化測試驗證
`shared/app` SHALL 提供一支 iOS target 測試，驗證啟動 Koin 後透過 `KoinHelper`
解析依賴能成功回傳實例，不須依賴 Swift 端或 `iosApp` 才能驗證此行為。

#### Scenario: 測試啟動 Koin 後解析成功
- **WHEN** 測試以 `startKoin`（或等效方式）啟動含 `KoinHelper` 所需 module 的 Koin
  容器，接著呼叫 `KoinHelper.userDataRepository()`
- **THEN** 測試斷言回傳值為非 null 的 `UserDataRepository` 實例

### Requirement: 消費端以建構子注入取得的實例
Swift 端消費 `KoinHelper` 解析出的實例時，SHALL 由組裝根（composition root，例如
App 進入點）取得實例後，以建構子參數往下傳遞給需要的物件；消費物件本身 SHALL NOT
在內部直接呼叫 `KoinHelper`。

#### Scenario: 組裝根取得實例並以建構子傳遞
- **WHEN** SwiftUI App 進入點需要建立一個依賴 `UserDataRepository` 的物件
- **THEN** 該物件的初始化方法以參數形式接收 `UserDataRepository`，其值由呼叫端
  透過 `KoinHelper.shared.userDataRepository()` 取得後傳入，而非由該物件內部
  呼叫 `KoinHelper`

