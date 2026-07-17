## MODIFIED Requirements

### Requirement: Datastore Koin module

`shared` MUST 提供 Koin datastore module，可以 resolve user preference data source，並將 `common` 的 `LanguageProvider`、`BaseHostUrlProvider` 介面綁定到 datastore-backed implementation。這些 datastore-backed provider 依賴 `common` 的 `commonModule()` 提供的 `CoroutineScope`，因此 datastore module MUST 搭配 `commonModule` 一起安裝才能完整解析。

#### Scenario: datastore module 搭配 common module 可解析 user preferences

- **WHEN** Koin 同時使用 `commonModule()` 與 datastore module 啟動
- **THEN** 可以 resolve `UserPreferenceDataSource`

#### Scenario: datastore module 搭配 common module 可解析 language provider

- **WHEN** Koin 同時使用 `commonModule()` 與 datastore module 啟動
- **THEN** `LanguageProvider` resolve 到 datastore-backed provider

#### Scenario: datastore module 搭配 common module 可解析 base host url provider

- **WHEN** Koin 同時使用 `commonModule()` 與 datastore module 啟動
- **THEN** `BaseHostUrlProvider` resolve 到 datastore-backed provider，其值反映 `UserPreferenceDataSource` 持久化的 `configuration.images.baseUrl`

#### Scenario: 缺少 common module 時 datastore-backed provider 無法解析

- **WHEN** Koin 只安裝 datastore module、未安裝 `commonModule()`
- **THEN** 解析 datastore-backed `LanguageProvider`／`BaseHostUrlProvider` 會因缺少 `CoroutineScope` binding 而失敗
