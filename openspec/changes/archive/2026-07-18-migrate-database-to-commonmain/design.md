## Context

參考 Android 專案（`JetpackMovieCompose/core/database`）使用 Room 搭配 Hilt，將 `AppDatabase`、DAO、entity 放在獨立的 `core:database` Gradle module，並以 `@Module`/`@InstallIn(SingletonComponent::class)` 提供單例。這個 KMP 專案目前的模組化方式不同：`shared/commonMain` 尚未拆出獨立 module，DI 一律使用 Koin，且已經有兩次先例可循——

- `migrate-datastore-to-commonmain`：把 `UserPreferenceDataSource` 遷移到 `shared/commonMain/datastore`，平台差異（檔案路徑）透過 `createUserPreferencesDataStore(...)` 的 expect-like 平台工廠函式處理，最後在 `initKoin(...)` 中安裝 `datastoreModule(dataStore)`。
- `migrate-common-providers-to-commonmain`：新增 `shared/commonMain` 的統一進入點 `initKoin(dataStore, isDebug, appDeclaration)`，`androidApp/JetpackMovieApplication` 與 `shared/iosMain/InitKoinIos.doInitKoinIos` 都透過這個進入點啟動 Koin。

`shared/build.gradle.kts` 已經完成 Room KMP 所需設定：`androidx.room.runtime`／`androidx.room.paging`／`androidx.sqlite.bundled` 依賴、Android／iosArm64／iosSimulatorArm64 三個 target 的 KSP `room-compiler`，以及 `room { schemaDirectory("$projectDir/schemas") }`（`shared/schemas` 目錄已存在）。因為這次遷移目標是 `commonMain`，Hilt 的 `@Module`/`@Provides`/`@Singleton`/`ApplicationContext` 都必須替換成 KMP 相容做法；Room 官方 KMP 支援本身就要求 `RoomDatabase.Builder<T>` 的建立邏輯留在平台層（Android 需要 `Context`，iOS 需要檔案路徑字串），資料庫本體與 DAO 定義則可以完全放在 `commonMain`。

## Goals / Non-Goals

**Goals:**

- 將 `MovieCollectEntity`、`MovieHistoryEntity`、`MovieCollectDao`、`MovieHistoryDao`、`AppDatabase` 遷移到 `shared/commonMain/database`，行為與參考專案等價。
- 比照 `datastore` 遷移的 expect-like 平台工廠模式，讓 `RoomDatabase.Builder<AppDatabase>` 的平台差異（檔案路徑取得方式）不進入共用業務邏輯。
- 新增 Koin `databaseModule(databaseBuilder)`，並整合進既有的 `initKoin(...)` 進入點，讓 Android／iOS 都能解析 `AppDatabase`／DAO。
- 維持與參考專案相同的 table 名稱（`MovieCollectEntity`、`MovieHistoryEntity`）與欄位，避免未來若要遷移既有資料時產生不必要的落差。

**Non-Goals:**

- 不建立獨立的 `core:database` Gradle module（Module 化留待之後的 change，見 proposal 非目標）。
- 不新增 repository／use-case 層——這次不比照既有 MVVM/Repository pattern 往上疊一層，因為目前 `shared` 尚未有消費 `MovieCollectDao`/`MovieHistoryDao` 的 ViewModel 或畫面，疊 repository 層會是沒有呼叫端的空殼抽象。DAO 直接透過 Koin 暴露，等未來實際串接收藏/瀏覽紀錄功能時，再依當時的畫面需求決定 repository 邊界。
- 不處理 Room schema migration（`Migration` class）——這次是全新資料庫（`version = 1`），沒有既有資料需要遷移。
- 不新增 UI 驗證（datastore 遷移曾在 `androidApp` 加一個測試按鈕；這次沒有對應的可視行為好驗證，改以 commonTest／androidHostTest 覆蓋，見 Risks）。

## Decisions

### 1. Database 程式碼保留在 `shared/commonMain/database`

比照 `datastore` 遷移的既有決策：符合目前不建立新 module 的限制，也避免在 shared app 架構還不完整時過早切出 module 邊界。Package 命名為 `com.shang.jetpackmoviekmp.database`，子 package 為 `entity`、`dao`、`di`，與 `datastore` package 的組織方式一致。

### 2. 平台專屬的 `RoomDatabase.Builder` 建立邏輯，比照 `createUserPreferencesDataStore` 模式

Room KMP 官方模式要求平台分別建立 `RoomDatabase.Builder<AppDatabase>`：

- **Android**：`Room.databaseBuilder(context, dbFile.absolutePath)`，其中 `dbFile = context.getDatabasePath(AppDatabase.DB_NAME)`（沿用 Android 慣用的 app-owned database 目錄，行為對齊 `getDatabasePath`）。
- **iOS**：`Room.databaseBuilder<AppDatabase>(name = documentDirectoryPath + "/" + AppDatabase.DB_NAME)`，document 目錄的取得方式與 `UserPreferencesDataStoreFactory.ios.kt` 相同（`NSFileManager.defaultManager.URLForDirectory(NSDocumentDirectory, ...)`），確保 app 重啟後仍讀得到同一份資料庫檔案。

commonMain 提供 `fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase`，統一設定 `setDriver(BundledSQLiteDriver())`（對應 `shared/build.gradle.kts` 既有的 `androidx.sqlite.bundled` 依賴）與 `setQueryCoroutineContext(Dispatchers.IO)` 後呼叫 `build()`。這個切法讓「平台如何找到資料庫檔案」與「資料庫怎麼被建立/設定」分離，與 datastore 的 `producePath` 切法概念一致。

**考慮過的替代方案**：讓 `commonMain` 定義 `expect fun getDatabaseBuilder(...): RoomDatabase.Builder<AppDatabase>` 並用 `actual` 在各平台實作。因為 Android 與 iOS 建構子簽名不同（Android 需要 `Context`，iOS 不需要），`expect`/`actual` 對這種參數不對稱的情境不如「平台各自定義一個同名 top-level 函式、由呼叫端各自傳參」自然——這正是 `createUserPreferencesDataStore` 現有的解法（`androidMain` 版本吃 `Context`、`iosMain` 版本不吃參數），這次沿用相同慣例而非導入 `expect`/`actual`。

### 3. DI 使用 Koin，`databaseModule` 吃建立 `RoomDatabase.Builder` 的 lambda

```kotlin
fun databaseModule(databaseBuilder: () -> RoomDatabase.Builder<AppDatabase>) = module {
    single { getRoomDatabase(databaseBuilder()) }
    single { get<AppDatabase>().createMovieCollectDao() }
    single { get<AppDatabase>().createMovieHistoryDao() }
}
```

`databaseModule` 的形狀比照 `datastoreModule(dataStore: DataStore<Preferences>)`——由呼叫端（平台）先準備好建立平台專屬物件的方式，再交給 module 函式，module 函式本身保持平台無關。與 `datastoreModule` 不同的是，這裡吃的是「建立 builder 的 lambda」而非「已經建好的 builder」：`getDatabaseBuilder(...)` 內部會呼叫 `context.getDatabasePath(...)` 之類的檔案系統操作，包成 lambda 才能讓這個操作跟著 `single{}` 一起延後到第一次真正 resolve `AppDatabase` 時才執行（見決策 4 的說明）。

### 4. 擴充 `initKoin(...)` 簽名，新增 `databaseBuilder` 參數

```kotlin
fun initKoin(
    dataStore: DataStore<Preferences>,
    databaseBuilder: () -> RoomDatabase.Builder<AppDatabase>,
    isDebug: Boolean,
    appDeclaration: KoinAppDeclaration,
) {
    startKoin {
        appDeclaration()
        modules(
            commonModule(),
            datastoreModule(dataStore),
            databaseModule(databaseBuilder),
            networkModule(isDebug = isDebug, provideDefaultLanguageProvider = false),
        )
    }
}
```

`androidApp/JetpackMovieApplication` 改傳入 `{ getDatabaseBuilder(this) }`；`shared/iosMain/InitKoinIos.doInitKoinIos` 改傳入 `{ getDatabaseBuilder() }`。這是**簽名變更**（新增必填參數），兩個呼叫端都需要同步更新，屬於本次變更範圍內的預期修改，不算對外 BREAKING（`initKoin`/`doInitKoinIos` 都是 app 啟動內部使用，沒有對外穩定 API 承諾）。

`databaseBuilder` 刻意宣告為 `() -> RoomDatabase.Builder<AppDatabase>`（lambda）而非直接傳入已建好的 builder：驗收階段的 code review 發現 `getDatabaseBuilder(context)` 內部呼叫 `context.getDatabasePath(...)`（檔案系統呼叫）本來會在 `Application.onCreate()` 當下就同步執行，即使 `AppDatabase` 從未被實際 resolve 也一樣，這跟同一個 `initKoin` 呼叫式裡 `createUserPreferencesDataStore` 的既有做法（把路徑解析包進 `producePath: () -> String` lambda、真正讀寫時才觸發）不一致。改成 lambda 後，`databaseModule` 的 `single { getRoomDatabase(databaseBuilder()) }` 只有在第一次真正 resolve `AppDatabase` 時才會呼叫 `databaseBuilder()`，維持 Koin `single{}` 原本的 lazy 語意，也讓 database 與 datastore 兩者的啟動時機慣例一致。

### 5. Table／欄位命名沿用參考專案

`MovieCollectEntity`、`MovieHistoryEntity` 的 `@Entity(tableName = ...)` 與 `@ColumnInfo(name = ...)` 全部照抄參考專案。`AppDatabase.DB_NAME` 改用專案自己的命名（例如 `"JetpackMovieKmpDatabase"`），避免與參考專案的資料庫檔案名稱混淆——兩者是不同 app，不會有資料互通的情境。

## Risks / Trade-offs

- **Room migration 策略**：這是全新資料庫，`version = 1`，目前不需要 `Migration`。風險是之後 schema 一旦變動（例如新增欄位），必須另立 change 補上 `Migration` 或改採 `fallbackToDestructiveMigration()`（會清空既有收藏/瀏覽紀錄）——這次不預先決定策略，留待實際需要時依當下需求判斷，並在 design 中明確記錄「非目標」避免範圍蔓延。
- **平台驗證風險（實作後確認比預期更廣）**：原先預期只有 iOS 模擬器無法在 Windows 執行。實作後發現 Android host test（`:shared:testAndroidHostTest`，純 JVM 執行，非模擬器/真機）**也**無法驗證真正的 Room 讀寫行為——`androidx.sqlite:sqlite-bundled-android` 的 native library（`sqliteJni`）是為真實 Android ABI 打包，JVM host test 的 `java.library.path` 找不到對應原生函式庫，任何實際開啟 SQLite 連線的操作都會拋出 `UnsatisfiedLinkError`；同時 host test 的 `android.content.Context` 也不是可用實作（呼叫 `context.getDatabasePath(...)` 等方法會拋出「not mocked」例外），這台機器未安裝 Robolectric（且本次 proposal 明確排除新增依賴）。緩解方式：拆成兩層——(1) Koin resolve-only 測試（`DatabaseModuleResolutionTest`，只驗證 DI 型別能否解析，不觸發實際查詢，`assertNotNull` 不會踩到上述限制）與 entity mapping 測試（`asExtendedModel()`，純 Kotlin 邏輯）留在 `commonTest`，在 Android host test 上有真實覆蓋；(2) 需要真正開啟連線做 insert/query/delete 的完整行為測試（`MovieCollectDaoTest`、`MovieHistoryDaoTest`、`DatabaseModuleTest` 的整合案例）改放 `shared/src/iosTest`——iOS/Kotlin Native 的 `Room.databaseBuilder(name)` 不需要 Context，原生 SQLite 也不受此限制，但同樣因為 Windows 無法執行 `iosTest`，這些測試目前只能保證原始碼正確（比照官方 Room KMP API 撰寫），未經實際執行驗證。詳細記錄見 tasks.md 7.4。
- **無 UI 可視驗證**：`datastore` 遷移曾用一個 debug button 證明 Android wiring 有效。這次 `MovieCollectDao`/`MovieHistoryDao` 沒有對應消費端，無法用同樣方式驗證。緩解方式如上一項風險所述，透過測試分層盡量涵蓋。
- **`initKoin` 簽名變更影響既有呼叫端**：新增必填參數會讓 `androidApp/JetpackMovieApplication` 與 `shared/iosMain/InitKoinIos.doInitKoinIos` 兩處呼叫點同時需要更新；`shared/commonTest` 內既有呼叫 `initKoin(...)` 的測試（若有）也需要同步調整參數。任務清單需明確列出這兩個呼叫點與既有測試的更新項目，避免遺漏導致編譯失敗。

## Migration Plan

1. 在 `shared/commonMain` 新增 `database/entity`、`database/dao`，遷移 entity 與 DAO 定義。
2. 新增 `database/AppDatabase`（`@Database`）與 commonMain 的 `getRoomDatabase(builder)`。
3. 新增 `androidMain`／`iosMain` 的 `getDatabaseBuilder(...)` 平台工廠函式。
4. 新增 `database/di/DatabaseModule.kt` 的 `databaseModule(databaseBuilder)`。
5. 擴充 `initKoin(...)` 簽名並更新 `androidApp/JetpackMovieApplication`、`shared/iosMain/InitKoinIos.doInitKoinIos` 兩個呼叫點。
6. 新增測試（entity/DAO 讀寫、Koin module resolve）並執行 shared/Android 驗證。

## Open Questions

- `AppDatabase.DB_NAME` 的具體字串留待實作時定案（目前傾向 `"JetpackMovieKmpDatabase"`），不影響本次設計決策。
