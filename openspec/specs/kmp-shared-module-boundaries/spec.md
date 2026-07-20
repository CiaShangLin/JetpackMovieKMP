## ADDED Requirements

### Requirement: shared 資料夾底下每一層皆為獨立 KMP Gradle module
`shared/` 資料夾 MUST 只作為容器目錄，不得擁有自己的 `build.gradle.kts`；其下 MUST 存在 8 個獨立的 KMP Gradle module：`shared:common`、`shared:model`、`shared:network`、`shared:datastore`、`shared:database`、`shared:data`、`shared:domain`、`shared:app`，各自對應原本 `:shared` 單一 module 內同名 package 的內容。

#### Scenario: shared 資料夾沒有可建置的根 module
- **WHEN** 檢查 `settings.gradle.kts` 的 `include(...)` 清單
- **THEN** 不存在 `include(":shared")`，只存在 `include(":shared:common")`、`include(":shared:model")`、`include(":shared:network")`、`include(":shared:datastore")`、`include(":shared:database")`、`include(":shared:data")`、`include(":shared:domain")`、`include(":shared:app")`

#### Scenario: 每個子模組各自擁有 build script
- **WHEN** 檢查 `shared/common`、`shared/model`、`shared/network`、`shared/datastore`、`shared/database`、`shared/data`、`shared/domain`、`shared/app` 目錄
- **THEN** 每個目錄下都存在各自的 `build.gradle.kts`，且 `shared/` 根目錄下不存在 `build.gradle.kts`

### Requirement: 模組依賴方向由 Gradle project dependency 強制驗證
子模組間的依賴方向 MUST 對應現有依賴圖：`shared:network`、`shared:datastore` MUST 只依賴 `shared:common`、`shared:model`；`shared:database` MUST 只依賴 `shared:common`、`shared:model`；`shared:data` MUST 依賴 `shared:common`、`shared:model`、`shared:network`、`shared:datastore`、`shared:database`；`shared:domain` MUST 依賴 `shared:common`、`shared:model`、`shared:data`；`shared:app` MUST 依賴全部 8 個子模組（含自身以外的 7 個）。`shared:common`、`shared:model` MUST NOT 依賴任何其他 `shared:*` 子模組。

dependency configuration MUST 反映 public API 暴露程度：只有 public signature 直接需要的 module 或 library 才能使用 `api`；純內部實作依賴 MUST 使用 `implementation`。`shared:app` MUST 作為 Koin composition root / DI facade。它可以依賴全部底層 `shared:*` 子模組來組裝 Koin，但不得因為組裝需要就把所有底層 module 以 transitive API 暴露給 app 使用者。`shared:app` 的 `api` dependency MUST 只限於其 public API 簽名直接出現的型別。

#### Scenario: network 與 datastore 互不依賴
- **WHEN** 檢查 `shared/network/build.gradle.kts` 與 `shared/datastore/build.gradle.kts` 的 dependencies 區塊
- **THEN** `shared:network` 的 dependencies 不包含 `projects.shared.datastore`，`shared:datastore` 的 dependencies 不包含 `projects.shared.network`

#### Scenario: common 與 model 沒有向上依賴
- **WHEN** 檢查 `shared/common/build.gradle.kts` 與 `shared/model/build.gradle.kts` 的 dependencies 區塊
- **THEN** 兩者皆不依賴任何其他 `shared:*` 子模組

#### Scenario: domain 依賴 data 而非反向
- **WHEN** 檢查 `shared/domain/build.gradle.kts` 與 `shared/data/build.gradle.kts` 的 dependencies 區塊
- **THEN** `shared:domain` 依賴 `projects.shared.data`，`shared:data` 不依賴 `projects.shared.domain`

#### Scenario: 循環依賴會導致 Gradle 建置失敗
- **WHEN** 任兩個 `shared:*` 子模組被錯誤地宣告為互相依賴
- **THEN** Gradle sync／build MUST 因 circular project dependency 而失敗，而非被靜默忽略

#### Scenario: data 不以 api 暴露底層實作 module
- **WHEN** 檢查 `shared/data/build.gradle.kts`
- **THEN** `shared:network`、`shared:database`、`shared:datastore` 使用 `implementation`
- **AND** `shared:model` 與 `androidx-paging-common` 因 repository public API 需要而使用 `api`

#### Scenario: app 預設不傳遞暴露底層 module
- **WHEN** 檢查 `shared/app/build.gradle.kts`
- **THEN** 僅供 Koin module 組裝使用的 `shared:*` dependency 使用 `implementation`
- **AND** 只有 `shared:app` public API 簽名直接需要的 dependency 可以使用 `api`

### Requirement: shared:app 是 androidApp 與 iosApp 依賴 shared 邏輯的唯一進入點
`shared:app` MUST 收斂原本位於 `shared` 根目錄的組裝根邏輯：跨平台 `initKoin(...)` 進入點與 iOS framework 匯出設定（`baseName = "Shared"`）。`androidApp` 與 iOS bridge 專案設定 MUST 只依賴 `:shared:app`，不得直接依賴 `shared:common`、`shared:network` 等底層子模組。

#### Scenario: androidApp 只依賴 shared:app
- **WHEN** 檢查 `androidApp/build.gradle.kts` 的 dependencies 區塊
- **THEN** 對 shared 相關程式碼的依賴只有 `projects.shared.app`，不存在對其他 `shared:*` 子模組的直接依賴

#### Scenario: iOS framework 由 shared:app 匯出
- **WHEN** 建置 iOS framework
- **THEN** `baseName = "Shared"` 的 framework 匯出設定位於 `shared/app/build.gradle.kts`，而非 `shared/` 根目錄或其他子模組

### Requirement: Provider 實作依其實際依賴歸屬對應模組
若某個 provider／實作類別依賴特定子模組提供的型別，該類別 MUST 定義在被依賴的子模組內，不得為了命名方便而放在依賴方（消費端）子模組，避免造成模組間雙向依賴。

#### Scenario: datastore-backed provider 定義於 shared:datastore
- **WHEN** 檢查 `DatastoreBaseHostUrlProvider`、`DatastoreLanguageProvider` 的定義位置
- **THEN** 兩者皆定義在 `shared:datastore` 模組內，而非 `shared:network`

#### Scenario: shared:network 不再依賴 shared:datastore
- **WHEN** 檢查 `shared/network/build.gradle.kts` 的 dependencies
- **THEN** 不包含 `projects.shared.datastore`
