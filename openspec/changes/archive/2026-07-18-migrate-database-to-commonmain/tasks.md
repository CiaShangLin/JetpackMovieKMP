## 1. shared/commonMain database entity 與 DAO 遷移

- [x] 1.1 建立 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/database/entity/`。
- [x] 1.2 遷移 `MovieCollectEntity`（`@Entity(tableName = "MovieCollectEntity")`，欄位與 `@ColumnInfo` 對應保持一致）。
- [x] 1.3 遷移 `MovieHistoryEntity`（`@Entity(tableName = "MovieHistoryEntity")`，欄位與 `@ColumnInfo` 對應保持一致）。
- [x] 1.4 建立 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/database/dao/`。
- [x] 1.5 遷移 `MovieCollectDao`（`getAllMovies()`、`insertMovieCollect()`、`deleteMovie()`、`collectedMovieIds()`、`getMovieCollectEntityById()`）。
- [x] 1.6 遷移 `MovieHistoryDao`（`getAllMovies()`、`insertMovie()`、`deleteMovie()`、`deleteAllMovies()`）。

## 2. shared/commonMain AppDatabase 與共用建立邏輯

- [x] 2.1 建立 `AppDatabase`（`@Database(entities = [MovieCollectEntity::class, MovieHistoryEntity::class], version = 1, exportSchema = false)`，含 `createMovieCollectDao()`／`createMovieHistoryDao()` 抽象方法與 `DB_NAME` 常數）。額外補上 tasks.md 原先沒列出、但 Room KMP 編譯必要的 `@ConstructedBy(AppDatabaseConstructor::class)` 與 `expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>`（由 room-compiler KSP 於各 target 自動產生 actual，不需手動實作）。
- [x] 2.2 新增 `getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase`，統一設定 `setDriver(BundledSQLiteDriver())` 與 `setQueryCoroutineContext(Dispatchers.IO)` 後 `build()`。

## 3. 平台專屬 database builder

- [x] 3.1 新增 `shared/src/androidMain/kotlin/com/shang/jetpackmoviekmp/database/` 下的 `getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase>`，使用 `context.getDatabasePath(AppDatabase.DB_NAME)` 取得檔案路徑。
- [x] 3.2 新增 `shared/src/iosMain/kotlin/com/shang/jetpackmoviekmp/database/` 下的 `getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>`，比照 `UserPreferencesDataStoreFactory.ios.kt` 的 document 目錄取得方式組出資料庫檔案路徑。

## 4. Koin DI module

- [x] 4.1 建立 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/database/di/DatabaseModule.kt`。
- [x] 4.2 實作 `databaseModule(databaseBuilder: RoomDatabase.Builder<AppDatabase>)`，提供 `AppDatabase`、`MovieCollectDao`、`MovieHistoryDao`。

## 5. initKoin 串接

- [x] 5.1 擴充 `shared/commonMain` 的 `initKoin(...)`，新增 `databaseBuilder: RoomDatabase.Builder<AppDatabase>` 參數並安裝 `databaseModule(databaseBuilder)`。額外把 `shared/build.gradle.kts` 的 `androidx.room.runtime` 依賴從 `implementation` 改成 `api`（比照既有 `androidx.datastore`／`androidx.datastore.preferences` 的作法），否則 `androidApp` 無法在呼叫 `initKoin(...)` 時解析 `RoomDatabase.Builder` 型別。
- [x] 5.2 更新 `androidApp/JetpackMovieApplication`，傳入 `getDatabaseBuilder(context = this)`。
- [x] 5.3 更新 `shared/iosMain/InitKoinIos.doInitKoinIos`，傳入 iOS 版 `getDatabaseBuilder()`。
- [x] 5.4 確認並更新 `shared/commonTest` 中既有呼叫 `initKoin(...)` 的測試（`InitKoinTest.kt`），補上新參數。

## 6. Tests

- [x] 6.1 新增 `MovieCollectDao` 讀寫測試：insert 後可在 `getAllMovies()`／`collectedMovieIds()`／`getMovieCollectEntityById()` 查到，delete 後查不到。實作於 `shared/src/iosTest/.../database/dao/MovieCollectDaoTest.kt`（原因見下方 6.4 說明）。
- [x] 6.2 新增 `MovieHistoryDao` 讀寫測試：insert 後可在 `getAllMovies()` 查到，`deleteAllMovies()` 後清空。實作於 `shared/src/iosTest/.../database/dao/MovieHistoryDaoTest.kt`（原因同上）。
- [x] 6.3 新增 Koin 測試，確認 database module 可 resolve `AppDatabase`、`MovieCollectDao`、`MovieHistoryDao`，且兩個 DAO 背後為同一個 `AppDatabase` 實例。resolve-only 子集（`DatabaseModuleResolutionTest`）在 `commonTest` 可安全執行；含實際讀寫驗證「同一個 AppDatabase 實例」的完整版（`DatabaseModuleTest`）在 `iosTest`。另外補上 `MovieCollectEntityTest`／`MovieHistoryEntityTest`（純 Kotlin `asExtendedModel()` mapping 測試，不需要 Room runtime）在 `commonTest`，涵蓋 entity mapping 邏輯。
- [x] 6.4 確認測試涵蓋率達到專案最低 80% 單元測試覆蓋率要求。調整 `shared/build.gradle.kts` 的 Kover filters：`database`、`database.di`、`database.entity` 納入覆蓋率範圍（皆有可在 Android host test 執行的測試覆蓋）；`database.dao`（`@Dao` interface 本身無邏輯、`*_Impl` 為 Room KSP 生成的樣板 SQL binding 程式碼）、`AppDatabase_Impl`、`AppDatabaseConstructor`（KSP 生成的 actual）、`DatabaseBuilder_androidKt`（需要真正可用的 Android Context）比照既有 `network.model` 的排除理由不納入。`.\gradlew.bat :shared:koverVerify` 通過。

## 7. Verification

- [x] 7.1 執行 `.\gradlew.bat :shared:testAndroidHostTest`。全數通過（entity mapping 測試、`InitKoinTest`、`DatabaseModuleResolutionTest` 等）。
- [x] 7.2 執行 `.\gradlew.bat :androidApp:assembleDebug`。BUILD SUCCESSFUL。
- [x] 7.3 若機器環境允許，安裝/啟動 Android app，確認啟動流程不因新增 database wiring 而崩潰。此開發機 `adb devices` 回報無可用裝置/模擬器，與 `migrate-datastore-to-commonmain` 記錄的環境限制相同，無法實機驗證；已透過 `androidApp:assembleDebug` 成功編譯，確認 `getDatabaseBuilder(context)` 呼叫鏈型別/依賴正確。
- [x] 7.4 記錄環境限制。

### 環境限制紀錄

- **iOS simulator tests**：Windows 主機無法執行 `iosSimulatorArm64Test`／`iosArm64Test`，Gradle 自動略過（沿用 `migrate-datastore-to-commonmain` 的既有限制）。`database.dao`、`database.di` 下涉及真實 DB 讀寫的測試（`MovieCollectDaoTest`、`MovieHistoryDaoTest`、`DatabaseModuleTest`）因此刻意放在 `shared/src/iosTest`（而非 `commonTest`），只在有 macOS/iOS 環境時才會實際執行與驗證，此機器上僅能保證原始碼正確（比照官方 Room KMP API 撰寫，經 decompile `androidx.room:room-runtime` 2.8.4 原始碼確認 API 簽名），未經模擬器實機驗證。
- **Android host test 無法開啟真正的 Room 資料庫連線**：實測發現 `androidx.sqlite:sqlite-bundled-android` 的 native library（`sqliteJni`）是為真實 Android ABI（arm64-v8a 等）打包，Android host test（`:shared:testAndroidHostTest`，純 JVM 執行，非模擬器/真機）的 `java.library.path` 找不到對應原生函式庫，任何實際開啟連線的 Room 操作都會拋出 `UnsatisfiedLinkError: no sqliteJni in java.library.path`。此外 Android host test 環境的 `android.content.Context` 也不是真正可用的實作（`context.getDatabasePath(...)` 等方法呼叫會拋出「not mocked」例外，這台機器上未安裝 Robolectric，且本次 proposal 明確排除新增依賴），因此 Android host test 只能驗證「型別/DI resolve 是否正確」（`DatabaseModuleResolutionTest`），無法驗證「實際 SQL 讀寫行為」。真正驗證讀寫行為的測試（`MovieCollectDaoTest`／`MovieHistoryDaoTest`／`DatabaseModuleTest` 的整合案例）因此改放 `iosTest`（原生 Kotlin/Native 編譯，`Room.databaseBuilder(name)` 不需要 Context），待未來在 macOS/iOS 環境或改用 Android instrumented test／Robolectric 時才能實際執行驗證。這是比 design.md 原先預期（僅 iOS 模擬器無法驗證）更廣的限制，設計文件已同步更新。
