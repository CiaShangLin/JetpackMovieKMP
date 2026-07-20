## Context

目前 `shared:data` 的 `commonMain.dependencies` 對 `shared:common`、`shared:model`、`shared:network`、`shared:datastore`、`shared:database` 全部使用 `api`。這讓 `shared:data` 的消費端可透過 transitive dependency 看到底層模組型別，和 module boundary 想達成的分層隔離不一致。

實際 public signature 盤點結果：

- `MovieRepository` / `UserDataRepository` 對外簽名使用 `shared:model` 型別、`Flow`、`Result`，其中電影分頁 API 使用 `PagingData<MovieCardResult>`。
- repository interface 沒有對外暴露 `shared:network`、`shared:database`、`shared:datastore` 型別。
- `MovieRepositoryImpl` constructor 使用 `MovieDataSource`、`MovieCollectDao`、`MovieHistoryDao`。
- `UserDataRepositoryImpl` constructor 使用 `UserPreferenceDataSource`。
- `MovieGenrePagingSource` / `MovieSearchPagingSource` constructor 使用 `MovieDataSource`。
- `MovieCardResult.asCollectEntity()` / `asHistoryEntity()` 回傳 database entity。

因此，若上述實作型別維持 public，`shared:network`、`shared:database`、`shared:datastore` 仍屬於 `shared:data` 的 public ABI，不能安全改成 `implementation`。收斂 Gradle dependency 之前，必須同步收斂 Kotlin visibility。

## Goals / Non-Goals

**Goals:**

- 讓 `shared:data` 的底層實作依賴不再透過 transitive API 暴露給上層 module。
- 明確 data 層對外契約為 `MovieRepository`、`UserDataRepository` 與 `dataModule()`。
- 讓實作 class、paging source、底層 mapper extension 成為 `shared:data` 內部細節。
- 明確 `shared:app` 的定位是 DI facade / composition root，依賴各 layer module 時預設使用 `implementation`。

**Non-Goals:**

- 不把 `MovieRepository` / `UserDataRepository` 搬到 `shared:domain`。
- 不改變 UseCase 建構方式；UseCase constructor 是否改 `internal constructor` 留待後續討論。
- 不改變 repository、datasource、DAO、DataStore 的執行期行為。
- 不在本 change 建立新的 feature module 或 screen model。

## Decisions

### 1. `api` 只用於 public ABI 需要的型別

Gradle `api` 代表該 dependency 的型別會成為本 module 對外契約的一部分；`implementation` 代表該 dependency 只供 module 內部使用。未出現在 public signature 的 dependency MUST 使用 `implementation`。

`shared:data` 的 public repository interface 需要 `shared:model` 與 `androidx-paging-common`，因此兩者使用 `api`。`shared:network`、`shared:database`、`shared:datastore` 只供實作與 DI binding 使用，必須收斂為 `implementation`。

### 2. data 層實作型別改為 `internal`

`MovieRepositoryImpl`、`UserDataRepositoryImpl` 是 Koin binding 的具體實作，不是外部建構用 API。將其改為 `internal` 後，外部只能透過 `MovieRepository` / `UserDataRepository` 介面使用，且底層 constructor 型別不會再進入 `shared:data` public ABI。

同理，`MovieGenrePagingSource`、`MovieSearchPagingSource` 是 repository implementation 的內部 paging 細節；database entity mapper extension 只在 data module 內部負責 model/entity 轉換。這些型別與 function 皆應改為 `internal`。

### 3. `shared:app` 是 DI facade，不是底層 API 聚合出口

`shared:app` 的主要目的為對 Android / iOS 暴露 Koin 啟動與組裝入口。它需要依賴各 layer module 來安裝 module，但不代表要把所有底層 module 當成自己的 public API 暴露出去。

因此 `shared:app` 對 `shared:common`、`shared:model`、`shared:network`、`shared:datastore`、`shared:database`、`shared:data`、`shared:domain` 預設使用 `implementation`。只有當 `shared:app` 的 public function signature 直接使用某個 module 的型別時，該 dependency 才能升為 `api`，並需要在設計上說明為何這個型別是 facade 契約的一部分。

### 4. 外部使用者只需要知道介面

實作型別改為 `internal` 不影響 DI 使用方式。Koin binding 仍可在同一 module 內建構 internal implementation：

```kotlin
single<MovieRepository> {
    MovieRepositoryImpl(...)
}
```

外部使用者只依賴 `MovieRepository`、`UserDataRepository`、UseCase 或 `shared:app` 的啟動入口。底層 datasource、DAO、DataStore 實作由各自 DI module 提供，不應成為 feature 層或 app 層的直接依賴。

## Risks / Trade-offs

- 若目前 Android 驗證畫面仍直接 inject `MovieDataSource` / `UserPreferenceDataSource`，收斂 `shared:app` dependency 後可能需要改為透過 repository 或 UseCase 驗證。這是符合最終架構方向的調整。
- 若測試直接建構 `MovieRepositoryImpl` 或 paging source，將 implementation 改為 `internal` 後仍可在同 module 測試中使用；跨 module 測試不得依賴這些實作細節。
- `shared:domain -> shared:data` 目前仍因 UseCase public constructor 使用 repository interface 而可能需要 `api`。本 change 先不處理架構反轉，避免把 visibility 收斂和 repository 歸屬調整混在同一次變更。
