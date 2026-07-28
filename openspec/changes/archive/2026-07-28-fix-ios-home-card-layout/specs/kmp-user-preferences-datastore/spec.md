## MODIFIED Requirements

### Requirement: Datastore Koin module

`shared/datastore` MUST 提供 Koin datastore module，可以 resolve user preference data source，並將 `shared:common` 的 `LanguageProvider`、`BaseHostUrlProvider` 介面綁定到 datastore-backed implementation。這些 datastore-backed provider（`DatastoreLanguageProvider`、`DatastoreBaseHostUrlProvider`）MUST 定義在 `shared:datastore` 模組內，且依賴 `shared:common` 的 `commonModule()` 提供的 `CoroutineScope`，因此 datastore module MUST 搭配 `commonModule` 一起安裝才能完整解析；`shared:datastore` MUST NOT 依賴 `shared:network`。

#### Scenario: datastore module 搭配 common module 可解析 user preferences
- **WHEN** Koin 同時使用 `commonModule()` 與 datastore module 啟動
- **THEN** 可以 resolve `UserPreferenceDataSource`

#### Scenario: datastore module 搭配 common module 可解析 language provider
- **WHEN** Koin 同時使用 `commonModule()` 與 datastore module 啟動
- **THEN** `LanguageProvider` resolve 到 `shared:datastore` 模組內的 `DatastoreLanguageProvider`

#### Scenario: datastore module 優先提供安全的 base host url provider
- **WHEN** Koin 同時使用 `commonModule()` 與 datastore module 啟動，且 configuration 同時包含 HTTP `baseUrl` 與 HTTPS `secureBaseUrl`
- **THEN** `BaseHostUrlProvider` resolve 到 `DatastoreBaseHostUrlProvider`，並回傳 HTTPS `secureBaseUrl`

#### Scenario: secure base host 缺失時升級 fallback host
- **WHEN** configuration 的 `secureBaseUrl` 為空字串且 `baseUrl` 為 HTTP URL
- **THEN** `DatastoreBaseHostUrlProvider` MUST 回傳相同 host 與 path 的 HTTPS URL

#### Scenario: 缺少 common module 時 datastore-backed provider 無法解析
- **WHEN** Koin 只安裝 datastore module、未安裝 `commonModule()`
- **THEN** 解析 datastore-backed `LanguageProvider`／`BaseHostUrlProvider` 會因缺少 `CoroutineScope` binding 而失敗

#### Scenario: shared:datastore 不依賴 shared:network
- **WHEN** 檢查 `shared/datastore/build.gradle.kts` 的 dependencies 區塊
- **THEN** 不包含 `projects.shared.network`，`DatastoreLanguageProvider`／`DatastoreBaseHostUrlProvider` 的實作完全在 `shared:datastore` 模組內部完成，不需要 import 任何 `shared:network` 的型別
