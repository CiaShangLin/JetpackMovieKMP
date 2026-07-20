## MODIFIED Requirements

### Requirement: 電影資料 Repository

`shared/data` 的 `commonMain` SHALL 對外提供 `MovieRepository` 作為電影資料存取介面。`MovieRepository` 的 public API SHALL 只暴露 model 型別、`Flow` / `Result` 與 paging public 型別，不得暴露 `shared:network`、`shared:database`、`shared:datastore` 的 datasource、DAO、entity 或 DataStore 實作型別。

`MovieRepositoryImpl`、paging source 與 model/entity mapper SHALL 視為 `shared:data` 內部實作細節，不得作為跨 module public API 使用。

#### Scenario: MovieRepository public API 不暴露底層型別

- **WHEN** 檢查 `MovieRepository` 的 public function signature
- **THEN** signature 不包含 `MovieDataSource`、DAO、database entity 或 DataStore datasource 型別
- **AND** 消費端只需依賴 repository interface 即可使用電影資料功能

#### Scenario: MovieRepository 實作由 dataModule 建立

- **WHEN** 安裝 `dataModule()` 並向 Koin container resolve `MovieRepository`
- **THEN** Koin 在 `shared:data` module 內部建構 repository implementation
- **AND** 呼叫端不需要也不能依賴 `MovieRepositoryImpl` 的具體 constructor

### Requirement: 使用者偏好設定 Repository

`shared/data` 的 `commonMain` SHALL 對外提供 `UserDataRepository` 作為使用者偏好設定存取介面。`UserDataRepository` 的 public API SHALL 只暴露 model 型別與 `Flow`，不得暴露 `UserPreferenceDataSource` 或 DataStore 實作型別。

`UserDataRepositoryImpl` SHALL 視為 `shared:data` 內部實作細節，由 `dataModule()` 綁定為 `UserDataRepository`。

#### Scenario: UserDataRepository public API 不暴露 datastore 型別

- **WHEN** 檢查 `UserDataRepository` 的 public property 與 function signature
- **THEN** signature 不包含 `UserPreferenceDataSource`、`DataStore` 或 `Preferences`
- **AND** 消費端只需依賴 `UserDataRepository` 介面即可讀寫使用者偏好設定
