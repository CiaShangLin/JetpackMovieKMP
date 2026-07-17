## 1. shared/commonMain database entity 與 DAO 遷移

- [ ] 1.1 建立 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/database/entity/`。
- [ ] 1.2 遷移 `MovieCollectEntity`（`@Entity(tableName = "MovieCollectEntity")`，欄位與 `@ColumnInfo` 對應保持一致）。
- [ ] 1.3 遷移 `MovieHistoryEntity`（`@Entity(tableName = "MovieHistoryEntity")`，欄位與 `@ColumnInfo` 對應保持一致）。
- [ ] 1.4 建立 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/database/dao/`。
- [ ] 1.5 遷移 `MovieCollectDao`（`getAllMovies()`、`insertMovieCollect()`、`deleteMovie()`、`collectedMovieIds()`、`getMovieCollectEntityById()`）。
- [ ] 1.6 遷移 `MovieHistoryDao`（`getAllMovies()`、`insertMovie()`、`deleteMovie()`、`deleteAllMovies()`）。

## 2. shared/commonMain AppDatabase 與共用建立邏輯

- [ ] 2.1 建立 `AppDatabase`（`@Database(entities = [MovieCollectEntity::class, MovieHistoryEntity::class], version = 1, exportSchema = false)`，含 `createMovieCollectDao()`／`createMovieHistoryDao()` 抽象方法與 `DB_NAME` 常數）。
- [ ] 2.2 新增 `getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase`，統一設定 `setDriver(BundledSQLiteDriver())` 與 `setQueryCoroutineContext(Dispatchers.IO)` 後 `build()`。

## 3. 平台專屬 database builder

- [ ] 3.1 新增 `shared/src/androidMain/kotlin/com/shang/jetpackmoviekmp/database/` 下的 `getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase>`，使用 `context.getDatabasePath(AppDatabase.DB_NAME)` 取得檔案路徑。
- [ ] 3.2 新增 `shared/src/iosMain/kotlin/com/shang/jetpackmoviekmp/database/` 下的 `getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>`，比照 `UserPreferencesDataStoreFactory.ios.kt` 的 document 目錄取得方式組出資料庫檔案路徑。

## 4. Koin DI module

- [ ] 4.1 建立 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/database/di/DatabaseModule.kt`。
- [ ] 4.2 實作 `databaseModule(databaseBuilder: RoomDatabase.Builder<AppDatabase>)`，提供 `AppDatabase`、`MovieCollectDao`、`MovieHistoryDao`。

## 5. initKoin 串接

- [ ] 5.1 擴充 `shared/commonMain` 的 `initKoin(...)`，新增 `databaseBuilder: RoomDatabase.Builder<AppDatabase>` 參數並安裝 `databaseModule(databaseBuilder)`。
- [ ] 5.2 更新 `androidApp/JetpackMovieApplication`，傳入 `getDatabaseBuilder(context = this)`。
- [ ] 5.3 更新 `shared/iosMain/InitKoinIos.doInitKoinIos`，傳入 iOS 版 `getDatabaseBuilder()`。
- [ ] 5.4 確認並更新 `shared/commonTest` 中既有呼叫 `initKoin(...)` 的測試，補上新參數。

## 6. Tests

- [ ] 6.1 新增 `MovieCollectDao` 讀寫測試：insert 後可在 `getAllMovies()`／`collectedMovieIds()`／`getMovieCollectEntityById()` 查到，delete 後查不到。
- [ ] 6.2 新增 `MovieHistoryDao` 讀寫測試：insert 後可在 `getAllMovies()` 查到，`deleteAllMovies()` 後清空。
- [ ] 6.3 新增 Koin 測試，確認 database module 可 resolve `AppDatabase`、`MovieCollectDao`、`MovieHistoryDao`，且兩個 DAO 背後為同一個 `AppDatabase` 實例。
- [ ] 6.4 確認測試涵蓋率達到專案最低 80% 單元測試覆蓋率要求（比照 `shared/build.gradle.kts` 既有 Kover 設定，視需要調整 filters/verify rule 範圍）。

## 7. Verification

- [ ] 7.1 執行 `.\gradlew.bat :shared:testAndroidHostTest`。
- [ ] 7.2 執行 `.\gradlew.bat :androidApp:assembleDebug`。
- [ ] 7.3 若機器環境允許，安裝/啟動 Android app，確認啟動流程不因新增 database wiring 而崩潰。
- [ ] 7.4 記錄任何環境限制，特別是 Windows 上無法執行 iOS simulator tests（沿用 `migrate-datastore-to-commonmain` 的既有記錄方式）。
