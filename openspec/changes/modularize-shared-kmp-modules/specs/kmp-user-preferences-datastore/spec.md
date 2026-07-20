## MODIFIED Requirements

### Requirement: Shared 使用者偏好設定 datastore

`shared/datastore` 的 `commonMain` MUST 提供 user preference data source，以 flow 暴露 `UserData`，並持久化 configuration、theme mode、language mode。

#### Scenario: 預設 user data 會被發出

- **WHEN** 尚未持久化任何 user preferences
- **THEN** data source 發出 `UserData.getDefault()`

#### Scenario: theme mode 會被持久化

- **WHEN** 呼叫 `setThemeMode(ThemeMode.DARK)`
- **THEN** 後續 `userData` emission 包含 `themeMode = ThemeMode.DARK`

#### Scenario: language mode 會被持久化

- **WHEN** 呼叫 `setLanguageMode(LanguageMode.ENGLISH)`
- **THEN** 後續 `userData` emission 包含 `languageMode = LanguageMode.ENGLISH`

#### Scenario: configuration 會被持久化

- **WHEN** 呼叫 `setConfiguration(configuration)`
- **THEN** 後續 `userData` emission 包含相同 configuration values

### Requirement: Datastore Koin module

`shared/datastore` MUST 提供 Koin datastore module，可以 resolve user preference data source，並將 `shared:common` 的 `LanguageProvider`、`BaseHostUrlProvider` 介面綁定到 datastore-backed implementation。這些 datastore-backed provider（`DatastoreLanguageProvider`、`DatastoreBaseHostUrlProvider`）MUST 定義在 `shared:datastore` 模組內，且依賴 `shared:common` 的 `commonModule()` 提供的 `CoroutineScope`，因此 datastore module MUST 搭配 `commonModule` 一起安裝才能完整解析；`shared:datastore` MUST NOT 依賴 `shared:network`。

#### Scenario: datastore module 搭配 common module 可解析 user preferences

- **WHEN** Koin 同時使用 `commonModule()` 與 datastore module 啟動
- **THEN** 可以 resolve `UserPreferenceDataSource`

#### Scenario: datastore module 搭配 common module 可解析 language provider

- **WHEN** Koin 同時使用 `commonModule()` 與 datastore module 啟動
- **THEN** `LanguageProvider` resolve 到 `shared:datastore` 模組內的 `DatastoreLanguageProvider`

#### Scenario: datastore module 搭配 common module 可解析 base host url provider

- **WHEN** Koin 同時使用 `commonModule()` 與 datastore module 啟動
- **THEN** `BaseHostUrlProvider` resolve 到 `shared:datastore` 模組內的 `DatastoreBaseHostUrlProvider`，其值反映 `UserPreferenceDataSource` 持久化的 `configuration.images.baseUrl`

#### Scenario: 缺少 common module 時 datastore-backed provider 無法解析

- **WHEN** Koin 只安裝 datastore module、未安裝 `commonModule()`
- **THEN** 解析 datastore-backed `LanguageProvider`／`BaseHostUrlProvider` 會因缺少 `CoroutineScope` binding 而失敗

#### Scenario: shared:datastore 不依賴 shared:network

- **WHEN** 檢查 `shared/datastore/build.gradle.kts` 的 dependencies 區塊
- **THEN** 不包含 `projects.shared.network`，`DatastoreLanguageProvider`／`DatastoreBaseHostUrlProvider` 的實作完全在 `shared:datastore` 模組內部完成，不需要 import 任何 `shared:network` 的型別
