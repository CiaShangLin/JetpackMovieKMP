# migrate-data-to-commonmain 架構圖

對應 change：`openspec/changes/migrate-data-to-commonmain`。呈現新增的 `data` 層如何整合既有的
`network`／`database`／`datastore` 三層，以及 Koin DI 的組裝關係。

## 元件架構圖

```mermaid
graph TB
    UI["🖥️ UI / 未來的 feature module<br/>（本次未涵蓋）"]

    subgraph DataLayer["📦 data 層（本次新增）"]
        MovieRepo["MovieRepository<br/>（interface）"]
        MovieRepoImpl["MovieRepositoryImpl"]
        UserDataRepo["UserDataRepository<br/>（interface）"]
        UserDataRepoImpl["UserDataRepositoryImpl"]
        GenrePaging["MovieGenrePagingSource"]
        SearchPaging["MovieSearchPagingSource"]
        MovieMapper["MovieMapper<br/>（asCollectEntity／asHistoryEntity）"]
    end

    subgraph ExistingLayers["🗂️ 既有底層（已個別遷移完成）"]
        MovieDataSource["network.datasource<br/>MovieDataSource"]
        CollectDao["database.dao<br/>MovieCollectDao"]
        HistoryDao["database.dao<br/>MovieHistoryDao"]
        PrefDataSource["datastore<br/>UserPreferenceDataSource"]
    end

    subgraph DiLayer["🔌 Koin DI"]
        CommonModule["common.di.commonModule()<br/>CoroutineScope +<br/>CoroutineDispatcher（qualifier: CommonDispatcher.IO）"]
        NetworkModule["network.di.networkModule()"]
        DatabaseModule["database.di.databaseModule()"]
        DatastoreModule["datastore.di.datastoreModule()"]
        DataModule["data.di.dataModule()<br/>（本次新增）"]
        InitKoin["InitKoin.kt<br/>initKoin(...)"]
    end

    UI -->|"注入使用"| MovieRepo
    UI -->|"注入使用"| UserDataRepo

    MovieRepo -.->|"實作"| MovieRepoImpl
    UserDataRepo -.->|"實作"| UserDataRepoImpl

    MovieRepoImpl --> MovieDataSource
    MovieRepoImpl --> CollectDao
    MovieRepoImpl --> HistoryDao
    MovieRepoImpl --> MovieMapper
    MovieRepoImpl --> GenrePaging
    MovieRepoImpl --> SearchPaging
    GenrePaging --> MovieDataSource
    SearchPaging --> MovieDataSource

    UserDataRepoImpl --> PrefDataSource

    CommonModule -->|"提供 ioDispatcher<br/>（named CommonDispatcher.IO）"| DataModule
    NetworkModule -->|"提供 MovieDataSource"| DataModule
    DatabaseModule -->|"提供 MovieCollectDao／MovieHistoryDao"| DataModule
    DatastoreModule -->|"提供 UserPreferenceDataSource"| DataModule
    DataModule -->|"組裝"| MovieRepoImpl
    DataModule -->|"組裝"| UserDataRepoImpl

    InitKoin -->|"安裝"| CommonModule
    InitKoin -->|"安裝"| NetworkModule
    InitKoin -->|"安裝"| DatabaseModule
    InitKoin -->|"安裝"| DatastoreModule
    InitKoin -->|"安裝"| DataModule

    classDef newLayer fill:#90EE90,stroke:#333,stroke-width:2px,color:darkgreen
    classDef existingLayer fill:#87CEEB,stroke:#333,stroke-width:2px,color:darkblue
    classDef diLayer fill:#E6E6FA,stroke:#333,stroke-width:2px,color:darkblue
    classDef ui fill:#FFD700,stroke:#333,stroke-width:2px,color:black

    class MovieRepo,MovieRepoImpl,UserDataRepo,UserDataRepoImpl,GenrePaging,SearchPaging,MovieMapper newLayer
    class MovieDataSource,CollectDao,HistoryDao,PrefDataSource existingLayer
    class CommonModule,NetworkModule,DatabaseModule,DatastoreModule,DataModule,InitKoin diLayer
    class UI ui
```

## 說明

- **`data` 層（綠色）** 是本次變更新增的整合層，統一提供 `MovieRepository`／`UserDataRepository`
  給未來的 UI／feature module 使用，內部聚合三個已個別遷移完成的底層依賴（藍色）。
- **`MovieGenrePagingSource`／`MovieSearchPagingSource`** 只依賴 `MovieDataSource`（network），
  由 `MovieRepositoryImpl` 透過 `Pager` 組裝成 `Flow<PagingData<MovieCardResult>>`。
- **`CommonDispatcher.IO` qualifier**（紫色 `commonModule()`）是本次討論後新增的共用 DI 元件：
  `MovieRepositoryImpl` 的 `ioDispatcher` 建構子參數由 `dataModule()` 透過
  `get(qualifier = named(CommonDispatcher.IO))` 向 `commonModule()` 取得，而非寫死在
  `dataModule()` 內部，讓未來其他 module 也能重用同一個 IO dispatcher 綁定。
- **`InitKoin.kt`** 是兩平台（Android／iOS）共用的 Koin 啟動進入點，`dataModule()` 加入
  `modules(...)` 清單後即完成串接，不需要新增任何函式參數（`dataModule()` 沒有平台專屬邏輯）。
- 本次僅遷移到 `shared/commonMain`，不建立獨立 Gradle module（例如 `core:data`），因此圖中沒有
  獨立的 module 邊界，`data` 層與既有三層都位於同一個 `shared` module 之內。
