## Why

`JetpackMovieCompose/core/database` 使用 Room 儲存使用者的電影收藏（`MovieCollectEntity`）與瀏覽紀錄（`MovieHistoryEntity`），並透過 Hilt 提供 `AppDatabase`／DAO。目前 KMP 專案的 `shared/commonMain` 完全沒有本地資料庫層——`MovieCardResult` 雖然已經有 `isCollect`、`timestamp` 欄位，但沒有任何機制可以持久化收藏與瀏覽紀錄。`shared/build.gradle.kts` 已經配置好 Room KMP 所需的依賴（`androidx.room.runtime`、`androidx.room.paging`、`androidx.sqlite.bundled`、KSP for Android/iosArm64/iosSimulatorArm64、`room { schemaDirectory(...) }`），但尚未有任何程式碼使用。

這次變更會把 Room database 遷移到 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/database`，沿用先前 `datastore` 遷移（`migrate-datastore-to-commonmain`）建立的模式：commonMain 定義 entity／DAO／`@Database` 與 Koin module，平台專屬的 `RoomDatabase.Builder` 建立邏輯放在 `androidMain`／`iosMain`，並比照 `initKoin` 的既有串接方式接上 Android／iOS 啟動流程。這次僅遷移到 `commonMain`，不建立獨立的 Gradle module（例如 `core:database`）；Module 化留待之後的 change 處理。

## What Changes

- 在 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/` 下新增 `database` package，包含 `entity`、`dao`、`di` 子 package。
- 將參考專案的 Room 結構遷移到 KMP：
  - `MovieCollectEntity`、`MovieHistoryEntity`（`@Entity`，欄位與 `@ColumnInfo` 對應保持一致）。
  - `MovieCollectDao`、`MovieHistoryDao`（含 `Flow` 查詢、`suspend` 寫入/刪除，行為與參考專案等價）。
  - `AppDatabase`（`@Database`，`entities = [MovieCollectEntity::class, MovieHistoryEntity::class]`，`version = 1`）。
- 比照 `createUserPreferencesDataStore` 的 expect/actual 平台工廠模式，新增建立 `RoomDatabase.Builder<AppDatabase>` 的平台專屬函式：
  - `androidMain`：以 `Context.getDatabasePath(...)` 取得資料庫檔案路徑。
  - `iosMain`：以穩定的 app document 目錄路徑建立資料庫檔案。
  - commonMain 提供共用的 `getRoomDatabase(builder)`，統一設定 `BundledSQLiteDriver` 與 query coroutine context 後 `build()`。
- 新增 Koin `databaseModule(databaseBuilder: RoomDatabase.Builder<AppDatabase>)`，提供 `AppDatabase`、`MovieCollectDao`、`MovieHistoryDao`。
- 擴充共用 `initKoin(...)` 進入點，新增 `databaseBuilder` 參數並安裝 `databaseModule(databaseBuilder)`；同步更新 `androidApp/JetpackMovieApplication` 與 `iosMain/InitKoinIos.doInitKoinIos`，讓兩個平台都傳入各自建立的 `RoomDatabase.Builder`。
- 新增聚焦測試，涵蓋 entity/DAO 行為（透過 in-memory 或 Android host test 資料庫）與 Koin module 能否正確 resolve `AppDatabase`／DAO。

## Capabilities

### New Capabilities

- `kmp-movie-local-database`：shared KMP 本地資料庫層，使用 Room 持久化電影收藏（`MovieCollectEntity`）與瀏覽紀錄（`MovieHistoryEntity`），並透過 Koin `databaseModule` 提供 `AppDatabase`／DAO。

### Modified Capabilities

（無——本次變更純新增，不改動既有 `kmp-user-preferences-datastore`、`ktor-movie-network`、`kmp-dependency-catalog` 的既有需求。）

## Impact

- **受影響 module**：`shared`、`androidApp`、`iosApp`
- **受影響 source sets**：
  - `shared/commonMain`：新增 `database`（`entity`、`dao`、`di`）package；新增/擴充 `initKoin(...)` 簽名
  - `shared/androidMain`：新增建立 `RoomDatabase.Builder<AppDatabase>` 的 Android 工廠函式
  - `shared/iosMain`：新增建立 `RoomDatabase.Builder<AppDatabase>` 的 iOS 工廠函式；更新 `InitKoinIos.doInitKoinIos`
  - `shared/commonTest`：entity mapping 測試與 Koin resolve-only 測試
  - `shared/iosTest`：需要真正開啟資料庫連線的 DAO／Koin module 整合測試（見 design.md 風險章節與 tasks.md 7.4 的環境限制說明）
  - `androidApp`：`JetpackMovieApplication` 改傳入 Android database builder
- **Dependencies**：不需新增外部依賴——`androidx-room-runtime`、`androidx-room-paging`、`androidx-sqlite-bundled`、`androidx-room-compiler`（KSP）、`room` plugin 均已在 `shared/build.gradle.kts` 配置完成，`shared/schemas` 目錄也已存在；唯一調整是把 `shared/build.gradle.kts` 既有的 `androidx.room.runtime` 依賴宣告從 `implementation` 改成 `api`（比照既有 `androidx.datastore`／`androidx.datastore.preferences` 的作法），讓 `androidApp` 能解析 `RoomDatabase.Builder` 型別——不是新增依賴，只是可見性調整。

## 非目標

- 不建立獨立的 `core:database` Gradle module（Module 化留待之後的 change）。
- 不遷移參考專案的 Hilt `@Module`/`@InstallIn`/`@Singleton` annotations，DI 一律改用 Koin。
- 不新增 repository／use-case 層或收藏/瀏覽紀錄的 UI（畫面、ViewModel）。
- 不變更 `MovieCardResult` 既有欄位或既有 network/datastore 行為。
- 不處理 Room schema migration 策略（`version = 1` 為初版，之後有 schema 變更時再另立 change 處理）。
