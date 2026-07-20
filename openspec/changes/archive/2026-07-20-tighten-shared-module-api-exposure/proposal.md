## Why

`shared:data` 在模組化後目前用 `api` 依賴 `shared:network`、`shared:database`、`shared:datastore`。這讓上層 module 可以暫時穩定編譯，但也會把 data 層的底層實作依賴透過 transitive API 暴露給 `shared:domain`、`shared:app` 與 app 端使用者。

架構上，外部使用者應只依賴 `MovieRepository`、`UserDataRepository`、UseCase 或 `shared:app` 提供的 DI facade，不應直接知道 `MovieDataSource`、DAO、database entity、`UserPreferenceDataSource` 等底層實作型別。若這些型別仍出現在 public ABI，Gradle 依賴就必須維持 `api`，無法真正收斂模組邊界。

## What Changes

- 收斂 `shared:data` 對底層 module 的依賴暴露：`shared:network`、`shared:database`、`shared:datastore` 由 `api` 改為 `implementation`。
- 保留 `shared:data` 對外契約需要的 `api`：`shared:model` 與 `androidx-paging-common`。
- 將 `shared:data` 內只供 module 內部與 Koin binding 建構使用的實作型別改為 `internal`，避免底層型別出現在 `shared:data` public ABI。
- 明確定義 `shared:app` 是 Koin composition root / DI facade。`shared:app` 依賴各 layer module 時預設使用 `implementation`，只有 public API 簽名必須出現的型別才允許對應 dependency 使用 `api`。
- 正式 feature 層不直接 inject `MovieDataSource`、`UserPreferenceDataSource` 等底層 datasource，而是透過 repository、UseCase 或後續 screen model 使用。

## Capabilities

### Modified Capabilities

- `kmp-shared-module-boundaries`：補充 `api` / `implementation` 暴露規則，並明確 `shared:app` 作為 DI facade 時的 dependency policy。
- `kmp-movie-data-repository`：補充 `shared:data` 對外契約只包含 repository interface 與 `dataModule()`，實作 class、paging source、底層 entity mapper 為內部實作細節。

## Impact

- 受影響 module：`shared:data`、`shared:domain`、`shared:app`。
- 受影響 build script：`shared/data/build.gradle.kts`、可能包含 `shared/app/build.gradle.kts`。
- 受影響 Kotlin visibility：`MovieRepositoryImpl`、`UserDataRepositoryImpl`、`MovieGenrePagingSource`、`MovieSearchPagingSource`、database entity mapper extension。
- 本 change 不搬移 repository interface 到 `shared:domain`，也不強制改 UseCase constructor visibility；`shared:domain -> shared:data` 的 dependency 收斂留待後續架構反轉或 constructor visibility 決策。
