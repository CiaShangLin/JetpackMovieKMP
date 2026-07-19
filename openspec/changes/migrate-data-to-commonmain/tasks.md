## 1. shared/commonMain mapper 遷移

- [ ] 1.1 建立 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/data/model/`。
- [ ] 1.2 新增 `MovieMapper.kt`：`MovieCardResult.asCollectEntity()`、`MovieCardResult.asHistoryEntity()`（欄位對應與參考專案一致）；`entity → model` 的 `asExtendedModel()` 沿用既有 `database.entity` 定義，不重複建立。

## 2. shared/commonMain paging 遷移

- [ ] 2.1 建立 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/data/paging/`。
- [ ] 2.2 遷移 `MovieGenrePagingSource`（`PagingSource<Int, MovieCardResult>`，呼叫 `MovieDataSource.getDiscoverMovie(withGenres, page)`，`prevKey`／`nextKey` 依 `totalPages` 計算）。
- [ ] 2.3 遷移 `MovieSearchPagingSource`（`PagingSource<Int, MovieCardResult>`，呼叫 `MovieDataSource.getMovieSearch(query, page)`）。

## 3. commonModule 擴充：共用 IO CoroutineDispatcher

- [ ] 3.1 在 `common/di/CommonModule.kt` 新增 `enum class CommonDispatcher { IO }`。
- [ ] 3.2 擴充 `commonModule()`，新增 `single<CoroutineDispatcher>(qualifier = named(CommonDispatcher.IO)) { Dispatchers.IO }`（見 design.md 決策 2）。
- [ ] 3.3 新增／更新 `CommonModuleTest`，確認可用 `named(CommonDispatcher.IO)` qualifier 成功 resolve `CoroutineDispatcher`。

## 4. shared/commonMain Repository 遷移

- [ ] 4.1 建立 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/data/repository/`。
- [ ] 4.2 遷移 `MovieRepository` 介面（`getConfiguration`、`getMovieGenres`、`getMovieListPager`、`getMovieSearchPager`、`getMovieDetail`、`getMovieRecommendations`、`getMovieActor`、收藏 CRUD、瀏覽紀錄 CRUD）。
- [ ] 4.3 遷移 `MovieRepositoryImpl`：依賴 `MovieDataSource`、`MovieCollectDao`、`MovieHistoryDao`，並新增必要建構子參數 `ioDispatcher: CoroutineDispatcher`（無預設值，見 design.md 決策 2）；`Pager` 組裝比照參考專案 `PagingConfig`（`pageSize = 20`、`enablePlaceholders = false`、`initialLoadSize = 20`、`prefetchDistance = 2`）。
- [ ] 4.4 遷移 `UserDataRepository` 介面與 `UserDataRepositoryImpl`：包裝既有 `UserPreferenceDataSource`。

## 5. Koin DI module

- [ ] 5.1 建立 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/data/di/DataModule.kt`。
- [ ] 5.2 實作 `dataModule()`：`single<MovieRepository> { MovieRepositoryImpl(get(), get(), get(), get(qualifier = named(CommonDispatcher.IO))) }`、`single<UserDataRepository> { UserDataRepositoryImpl(get()) }`。

## 6. initKoin 串接

- [ ] 6.1 擴充 `shared/commonMain` 的 `initKoin(...)`（含兩個重載），在 `modules(...)` 加入 `dataModule()`；確認不需要新增任何函式參數。
- [ ] 6.2 確認 `androidApp/JetpackMovieApplication`、`shared/iosMain/InitKoinIos.doInitKoinIos` 不需要修改（`dataModule()` 無平台專屬參數）。
- [ ] 6.3 確認並更新 `shared/commonTest` 中既有呼叫 `initKoin(...)` 的測試（`InitKoinTest.kt`），視需要補上 `dataModule()` 相關的 resolve 驗證。

## 7. Tests

- [ ] 7.1 新增 `MovieMapper` 測試（`asCollectEntity()`／`asHistoryEntity()` 欄位對應正確）於 `commonTest/data/model`。
- [ ] 7.2 新增 `MovieRepositoryImpl` 測試：使用 fake `MovieDataSource`（比照 `network` 遷移的 `MovieDataSourceTestSupport` 手法）、in-memory fake `MovieCollectDao`／`MovieHistoryDao`，並直接建構 `MovieRepositoryImpl(fake, fake, fake, testDispatcher)`（不透過 Koin），涵蓋 configuration／genres 成功與失敗、分頁載入、收藏／瀏覽紀錄新增刪除查詢、`deleteAllMovieHistory()` 回傳值。
- [ ] 7.3 新增 `UserDataRepositoryImpl` 測試：沿用既有 `InMemoryPreferencesDataStore`（或直接注入既有 `UserPreferenceDataSource` 搭配 in-memory DataStore），驗證 `userData` 反映 `setConfiguration`／`setThemeMode`／`setLanguageMode`。
- [ ] 7.4 新增 `dataModule()` resolve 測試：安裝 `commonModule()`、`networkModule(...)`、`databaseModule(...)`、`datastoreModule(...)`、`dataModule()` 後可成功 resolve `MovieRepository`／`UserDataRepository`。
- [ ] 7.5 確認測試涵蓋率達到專案最低 80% 單元測試覆蓋率要求，視需要調整 `shared/build.gradle.kts` 的 Kover filters（納入 `data.repository`、`data.paging`、`data.model`、`data.di`）。

## 8. Verification

- [ ] 8.1 執行 `.\gradlew.bat :shared:testAndroidHostTest`，確認全數通過。
- [ ] 8.2 執行 `.\gradlew.bat :shared:koverVerify`，確認覆蓋率門檻通過。
- [ ] 8.3 執行 `.\gradlew.bat :androidApp:assembleDebug`，確認編譯成功。
- [ ] 8.4 執行 `ktlintCheck`，確認格式符合專案規範。
- [ ] 8.5 記錄環境限制（若 iOS 模擬器測試因 Windows 主機無法執行，比照先前三次遷移的既有記錄方式註明）。
