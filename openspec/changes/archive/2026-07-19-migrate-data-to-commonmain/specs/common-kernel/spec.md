## MODIFIED Requirements

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
