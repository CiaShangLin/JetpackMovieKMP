# common-kernel Specification

## Purpose
TBD - created by archiving change migrate-common-providers-to-commonmain. Update Purpose after archive.

## Requirements
### Requirement: commonMain 提供中立的跨層共用型別

`shared/commonMain` MUST 在 `com.shang.jetpackmoviekmp.common` package 提供 `LanguageProvider`、`BaseHostUrlProvider`、`NetworkException` 型別定義；該 package MUST NOT 依賴 `network` 或 `datastore` package 底下的任何型別，確保依賴方向永遠是消費端（network、datastore）依賴 `common`，而非反向。

#### Scenario: common package 不依賴 network 或 datastore
- **WHEN** 檢查 `common` package 底下所有檔案的 import
- **THEN** 不包含任何 `com.shang.jetpackmoviekmp.network.*` 或 `com.shang.jetpackmoviekmp.datastore.*` 型別

#### Scenario: LanguageProvider 定義位於 common
- **WHEN** 解析 `com.shang.jetpackmoviekmp.common.LanguageProvider`
- **THEN** 該型別存在且僅定義 `getLanguageCode(): String`

#### Scenario: BaseHostUrlProvider 定義位於 common
- **WHEN** 解析 `com.shang.jetpackmoviekmp.common.BaseHostUrlProvider`
- **THEN** 該型別存在且僅定義 `getBaseHostUrl(): String`

#### Scenario: NetworkException 定義位於 common
- **WHEN** 解析 `com.shang.jetpackmoviekmp.common.NetworkException`
- **THEN** 該型別存在，包含 `HttpError`、`ConnectionError`、`TimeoutError`、`ParseError`、`UnknownError` 子型別，且 `Throwable.toNetworkException()` extension 行為與搬移前一致

### Requirement: network 與 datastore 皆透過 common 介面提供 provider 實作

`network`、`datastore` package MUST 透過 `com.shang.jetpackmoviekmp.common` 的介面型別（而非彼此重新定義的介面）來實作與綁定 `LanguageProvider`、`BaseHostUrlProvider`。

#### Scenario: Koin binding 綁定到 common 介面型別
- **WHEN** `networkModule`、`datastoreModule` 綁定 `LanguageProvider`、`BaseHostUrlProvider`
- **THEN** 綁定的介面型別皆為 `com.shang.jetpackmoviekmp.common.LanguageProvider`／`com.shang.jetpackmoviekmp.common.BaseHostUrlProvider`

#### Scenario: BaseHostUrlProvider 反映 configuration 的圖片 base URL
- **WHEN** `UserPreferenceDataSource` 持久化的 `configuration.images.baseUrl` 有值
- **THEN** datastore-backed `BaseHostUrlProvider.getBaseHostUrl()` 回傳該值（非空字串時補上尾端 `/`）

### Requirement: common 提供共用 CoroutineScope 與 CoroutineDispatcher 的 Koin module

`shared/commonMain` MUST 在 `common` package 提供 `commonModule()`（Koin module function），綁定單一 `CoroutineScope` single，以及至少一個帶 qualifier 的 `CoroutineDispatcher` single（`CommonDispatcher.IO`，透過 `named(CommonDispatcher.IO)` 解析）；其他 module（例如 `datastoreModule`、`dataModule`）MUST NOT 自行重複定義等價的 `CoroutineScope`／`CoroutineDispatcher` binding，MUST 透過 `commonModule()` 提供的 single 取得。

#### Scenario: commonModule 可解析 CoroutineScope
- **WHEN** Koin 使用 `commonModule()` 啟動
- **THEN** 可以 resolve 非 null 的 `CoroutineScope`

#### Scenario: datastoreModule 不再自帶 CoroutineScope binding
- **WHEN** 檢查 `datastoreModule()` 的實作
- **THEN** 其內容不包含 `single<CoroutineScope>` 定義，該 binding 只存在於 `commonModule()`

#### Scenario: commonModule 可依 qualifier 解析 IO CoroutineDispatcher
- **WHEN** Koin 使用 `commonModule()` 啟動，並以 `qualifier = named(CommonDispatcher.IO)` 要求 `CoroutineDispatcher`
- **THEN** 可以 resolve 非 null 的 `CoroutineDispatcher`（正式環境為 `kotlinx.coroutines.Dispatchers.IO`）

#### Scenario: dataModule 不自帶 CoroutineDispatcher binding
- **WHEN** 檢查 `dataModule()` 的實作
- **THEN** 其內容不包含 `single<CoroutineDispatcher>` 定義，`MovieRepositoryImpl` 所需的 `ioDispatcher` 透過 `get(qualifier = named(CommonDispatcher.IO))` 向 `commonModule()` 取得
