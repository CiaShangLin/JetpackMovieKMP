## MODIFIED Requirements

### Requirement: commonMain 提供中立的跨層共用型別

`shared/common` 的 `commonMain` MUST 在 `com.shang.jetpackmoviekmp.common` package 提供 `LanguageProvider`、`BaseHostUrlProvider`、`NetworkException` 型別定義；該 package MUST NOT 依賴 `network` 或 `datastore` package 底下的任何型別，確保依賴方向永遠是消費端（`shared:network`、`shared:datastore`）依賴 `shared:common`，而非反向。

#### Scenario: common package 不依賴 network 或 datastore
- **WHEN** 檢查 `shared:common` 模組底下所有檔案的 import
- **THEN** 不包含任何 `com.shang.jetpackmoviekmp.network.*` 或 `com.shang.jetpackmoviekmp.datastore.*` 型別，且 `shared/common/build.gradle.kts` 不依賴 `projects.shared.network` 或 `projects.shared.datastore`

#### Scenario: LanguageProvider 定義位於 common
- **WHEN** 解析 `com.shang.jetpackmoviekmp.common.LanguageProvider`
- **THEN** 該型別存在於 `shared:common` 模組，且僅定義 `getLanguageCode(): String`

#### Scenario: BaseHostUrlProvider 定義位於 common
- **WHEN** 解析 `com.shang.jetpackmoviekmp.common.BaseHostUrlProvider`
- **THEN** 該型別存在於 `shared:common` 模組，且僅定義 `getBaseHostUrl(): String`

#### Scenario: NetworkException 定義位於 common
- **WHEN** 解析 `com.shang.jetpackmoviekmp.common.NetworkException`
- **THEN** 該型別存在於 `shared:common` 模組，包含 `HttpError`、`ConnectionError`、`TimeoutError`、`ParseError`、`UnknownError` 子型別，且 `Throwable.toNetworkException()` extension 行為與模組化前一致

### Requirement: common 提供共用 CoroutineScope 與 CoroutineDispatcher 的 Koin module

`shared/common` 的 `commonMain` MUST 在 `common` package 提供 `commonModule()`（Koin module function），綁定單一 `CoroutineScope` single，以及至少一個帶 qualifier 的 `CoroutineDispatcher` single（`CommonDispatcher.IO`，透過 `named(CommonDispatcher.IO)` 解析）；其他子模組（例如 `shared:datastore`、`shared:data`）的 Koin module MUST NOT 自行重複定義等價的 `CoroutineScope`／`CoroutineDispatcher` binding，MUST 透過 `commonModule()` 提供的 single 取得。

#### Scenario: commonModule 可解析 CoroutineScope
- **WHEN** Koin 使用 `shared:common` 提供的 `commonModule()` 啟動
- **THEN** 可以 resolve 非 null 的 `CoroutineScope`

#### Scenario: datastoreModule 不再自帶 CoroutineScope binding
- **WHEN** 檢查 `shared:datastore` 的 `datastoreModule()` 實作
- **THEN** 其內容不包含 `single<CoroutineScope>` 定義，該 binding 只存在於 `shared:common` 的 `commonModule()`

#### Scenario: commonModule 可依 qualifier 解析 IO CoroutineDispatcher
- **WHEN** Koin 使用 `commonModule()` 啟動，並以 `qualifier = named(CommonDispatcher.IO)` 要求 `CoroutineDispatcher`
- **THEN** 可以 resolve 非 null 的 `CoroutineDispatcher`（正式環境為 `kotlinx.coroutines.Dispatchers.IO`）

#### Scenario: dataModule 不自帶 CoroutineDispatcher binding
- **WHEN** 檢查 `shared:data` 的 `dataModule()` 實作
- **THEN** 其內容不包含 `single<CoroutineDispatcher>` 定義，`MovieRepositoryImpl` 所需的 `ioDispatcher` 透過 `get(qualifier = named(CommonDispatcher.IO))` 向 `shared:common` 的 `commonModule()` 取得
