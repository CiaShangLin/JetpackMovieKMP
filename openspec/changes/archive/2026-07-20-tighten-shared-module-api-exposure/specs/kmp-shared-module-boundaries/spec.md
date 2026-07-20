## MODIFIED Requirements

### Requirement: 模組依賴方向由 Gradle project dependency 強制驗證

子模組間的依賴方向 MUST 對應既有分層邊界，且 dependency configuration MUST 反映 public API 暴露程度：只有 public signature 直接需要的 module 或 library 才能使用 `api`；純內部實作依賴 MUST 使用 `implementation`。

`shared:app` MUST 作為 Koin composition root / DI facade。它可以依賴全部底層 `shared:*` 子模組來組裝 Koin，但不得因為組裝需要就把所有底層 module 以 transitive API 暴露給 app 使用者。`shared:app` 的 `api` dependency MUST 只限於其 public API 簽名直接出現的型別。

#### Scenario: data 不以 api 暴露底層實作 module

- **WHEN** 檢查 `shared/data/build.gradle.kts`
- **THEN** `shared:network`、`shared:database`、`shared:datastore` 使用 `implementation`
- **AND** `shared:model` 與 `androidx-paging-common` 因 repository public API 需要而使用 `api`

#### Scenario: app 預設不傳遞暴露底層 module

- **WHEN** 檢查 `shared/app/build.gradle.kts`
- **THEN** 僅供 Koin module 組裝使用的 `shared:*` dependency 使用 `implementation`
- **AND** 只有 `shared:app` public API 簽名直接需要的 dependency 可以使用 `api`
