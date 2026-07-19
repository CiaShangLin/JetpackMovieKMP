## 1. shared/commonMain mapper 遷移

- [x] 1.1 建立 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/data/model/`。
- [x] 1.2 新增 `MovieMapper.kt`：`MovieCardResult.asCollectEntity()`、`MovieCardResult.asHistoryEntity()`（欄位對應與參考專案一致）；`entity → model` 的 `asExtendedModel()` 沿用既有 `database.entity` 定義，不重複建立。

## 2. shared/commonMain paging 遷移

- [x] 2.1 建立 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/data/paging/`。
- [x] 2.2 遷移 `MovieGenrePagingSource`（`PagingSource<Int, MovieCardResult>`，呼叫 `MovieDataSource.getDiscoverMovie(withGenres, page)`，`prevKey`／`nextKey` 依 `totalPages` 計算）。
- [x] 2.3 遷移 `MovieSearchPagingSource`（`PagingSource<Int, MovieCardResult>`，呼叫 `MovieDataSource.getMovieSearch(query, page)`）。修正參考專案原本 `response.error?.cause` 的寫法：KMP `NetworkResponse.error` 是 `NetworkException`，`HttpError` 等子型別的 `cause` 常為 null，改用 `response.error` 本身（比照 `MovieGenrePagingSource`），避免吞掉實際錯誤資訊。

## 3. commonModule 擴充：共用 IO CoroutineDispatcher

- [x] 3.1 在 `common/di/CommonModule.kt` 新增 `enum class CommonDispatcher { IO }`。
- [x] 3.2 擴充 `commonModule()`，新增 `single<CoroutineDispatcher>(qualifier = named(CommonDispatcher.IO)) { Dispatchers.IO }`（見 design.md 決策 2）。
- [x] 3.3 新增／更新 `CommonModuleTest`，確認可用 `named(CommonDispatcher.IO)` qualifier 成功 resolve `CoroutineDispatcher`。

## 4. shared/commonMain Repository 遷移

- [x] 4.1 建立 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/data/repository/`。
- [x] 4.2 遷移 `MovieRepository` 介面（`getConfiguration`、`getMovieGenres`、`getMovieListPager`、`getMovieSearchPager`、`getMovieDetail`、`getMovieRecommendations`、`getMovieActor`、收藏 CRUD、瀏覽紀錄 CRUD）。
- [x] 4.3 遷移 `MovieRepositoryImpl`：依賴 `MovieDataSource`、`MovieCollectDao`、`MovieHistoryDao`，並新增必要建構子參數 `ioDispatcher: CoroutineDispatcher`（無預設值，見 design.md 決策 2）；`Pager` 組裝比照參考專案 `PagingConfig`（`pageSize = 20`、`enablePlaceholders = false`、`initialLoadSize = 20`、`prefetchDistance = 2`）。
- [x] 4.4 遷移 `UserDataRepository` 介面與 `UserDataRepositoryImpl`：包裝既有 `UserPreferenceDataSource`。

## 5. Koin DI module

- [x] 5.1 建立 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/data/di/DataModule.kt`。
- [x] 5.2 實作 `dataModule()`：`single<MovieRepository> { MovieRepositoryImpl(get(), get(), get(), get(qualifier = named(CommonDispatcher.IO))) }`、`single<UserDataRepository> { UserDataRepositoryImpl(get()) }`。

## 6. initKoin 串接

- [x] 6.1 擴充 `shared/commonMain` 的 `initKoin(...)`（含兩個重載），在 `modules(...)` 加入 `dataModule()`；確認不需要新增任何函式參數。
- [x] 6.2 確認 `androidApp/JetpackMovieApplication`、`shared/iosMain/InitKoinIos.doInitKoinIos` 不需要修改（`dataModule()` 無平台專屬參數）。
- [x] 6.3 確認並更新 `shared/commonTest` 中既有呼叫 `initKoin(...)` 的測試（`InitKoinTest.kt`），補上 `MovieRepository`／`UserDataRepository` 的 resolve 驗證。

## 7. Tests

- [x] 7.1 新增 `MovieMapper` 測試（`asCollectEntity()`／`asHistoryEntity()` 欄位對應正確）於 `commonTest/data/model`。
- [x] 7.2 新增 `MovieRepositoryImpl` 測試：使用 fake `MovieDataSource`、in-memory fake `MovieCollectDao`／`MovieHistoryDao`（`RepositoryTestFakes.kt`），並直接建構 `MovieRepositoryImpl(fake, fake, fake, UnconfinedTestDispatcher())`（不透過 Koin），涵蓋 configuration／genres／detail 成功與失敗、收藏／瀏覽紀錄新增刪除查詢、`deleteAllMovieHistory()` 回傳值（true／false 兩種情況）。分頁邏輯改用專屬的 `MovieGenrePagingSourceTest`／`MovieSearchPagingSourceTest`（直接呼叫 `PagingSource.load(...)` 驗證 `LoadResult.Page`／`Error`，含 `MovieSearchPagingSource` 修正 `.cause` bug 的回歸測試），`MovieRepositoryImpl.getMovieListPager`／`getMovieSearchPager` 則做 collectible smoke test（專案沒有 `androidx.paging:paging-testing` 依賴，無法在 commonTest 直接斷言 `PagingData` 內容）。
- [x] 7.3 新增 `UserDataRepositoryImpl` 測試：沿用既有 `InMemoryPreferencesDataStore`，驗證 `userData` 反映 `setConfiguration`／`setThemeMode`／`setLanguageMode`。
- [x] 7.4 新增 `dataModule()` resolve 測試：安裝 `commonModule()`、`networkModule(...)`、`databaseModule(...)`、`datastoreModule(...)`、`dataModule()` 後可成功 resolve `MovieRepository`／`UserDataRepository`。
- [x] 7.5 確認測試涵蓋率達到專案最低 80% 單元測試覆蓋率要求。調整 `shared/build.gradle.kts` 的 Kover filters：新增 `data.repository`、`data.paging`、`data.model`、`data.di`，以及本次一併擴充出邏輯的 `common.di`。line coverage：`data.repository` 84%、`data.paging` 82.2%、`data.model`／`data.di`／`common.di` 100%。`:shared:koverVerify` 通過。

## 8. Verification

- [x] 8.1 執行 `.\gradlew.bat :shared:testAndroidHostTest`，確認全數通過。全數通過（21 個測試 class，0 failures／0 errors）。
- [x] 8.2 執行 `.\gradlew.bat :shared:koverVerify`，確認覆蓋率門檻通過。通過（見 7.5）。
- [x] 8.3 執行 `.\gradlew.bat :androidApp:assembleDebug`，確認編譯成功。BUILD SUCCESSFUL。
- [x] 8.4 執行 `ktlintCheck`，確認格式符合專案規範。`:shared:testAndroidHostTest`、`:shared:koverVerify`、`:androidApp:assembleDebug` 三次執行皆內含 `ktlintFormat`／`ktlintCheck` 任務且全數通過（`enum class CommonDispatcher { IO }` 曾被自動格式化為含 trailing comma，已確認）。
- [x] 8.5 記錄環境限制（若 iOS 模擬器測試因 Windows 主機無法執行，比照先前三次遷移的既有記錄方式註明）。見下方「環境限制紀錄」。

### 環境限制紀錄

- **iOS 模擬器測試**：Windows 主機無法執行 `iosSimulatorArm64Test`／`iosArm64Test`，Gradle 自動略過（沿用先前三次遷移已記錄的既有限制）。`dataModule()`、`MovieRepositoryImpl`、`UserDataRepositoryImpl` 皆為純 Kotlin／Koin resolve 邏輯，不含平台專屬程式碼，本機僅能保證原始碼正確，未經 iOS 模擬器實機驗證。
- **Paging 分頁資料斷言**：專案未引入 `androidx.paging:paging-testing`（KMP 上是否有穩定支援待確認），因此無法在 commonTest 用 `asSnapshot()` 直接斷言 `MovieRepositoryImpl.getMovieListPager()`／`getMovieSearchPager()` 回傳的 `Flow<PagingData<MovieCardResult>>` 實際內容。改以兩層驗證：分頁核心邏輯（`prevKey`／`nextKey`／`LoadResult.Error`）直接對 `MovieGenrePagingSource`／`MovieSearchPagingSource` 呼叫 `PagingSource.load(...)` 驗證；`MovieRepositoryImpl` 層則只做「`Flow` 可正常 collect 不拋例外」的 smoke test，`Pager`／`PagingConfig` wiring 本身邏輯簡單（純參數字面值 + factory lambda），風險可接受。
- **Android host test 無法開啟真正的 Room 資料庫連線**：沿用 `migrate-database-to-commonmain` 已記錄的既有限制（`sqliteJni` native library 在 host test JVM 環境無法載入）。`MovieRepositoryImpl`／`dataModule()` 的測試因此改用實作 `MovieCollectDao`／`MovieHistoryDao` 介面的記憶體內 fake（`RepositoryTestFakes.kt`），不透過真正的 Room 連線；`dataModule()` 的 Koin resolve 測試（`DataModuleTest`）比照 `DatabaseModuleResolutionTest` 的作法，只驗證 DI graph 可解析、不觸發實際 DAO 讀寫。

## 9. Code review 修正（驗收階段，codex 第二意見）

驗收階段委派 codex 對本次變更做獨立 code review，發現並驗證以下問題，經用戶確認後修正：

- [x] 9.1 `MovieGenrePagingSource`／`MovieSearchPagingSource` 的 `catch (e: Exception)` 會連帶吞掉 `CancellationException`，破壞 coroutine cooperative cancellation（此問題繼承自參考專案原始碼，非本次遷移引入）。修正為先 `catch (e: CancellationException) { throw e }` 再 `catch (e: Exception)`，並補上 `load_rethrows_cancellationException_instead_of_wrapping_as_error` 回歸測試。
- [x] 9.2 `MovieGenrePagingSource`／`MovieSearchPagingSource` 的 `nextKey` 判斷式 `page == totalPages` 在 `totalPages = 0`（無搜尋結果）時會誤判成「還有下一頁」，造成無限分頁載入（同樣繼承自參考專案原始碼）。修正為 `page >= totalPages`（並以 `response.data?.totalPages ?: page` 防呆），補上 `load_returns_null_nextKey_when_totalPages_is_zero` 回歸測試。
- [x] 9.3 `MovieRepositoryImpl` 的 `response.error!!`（5 處）在 `NetworkResponse.isSuccess = false` 但 `error` 也為 null 的邊界情況下會 NPE（`safeApiCall` 目前所有失敗路徑都會填入 `error`，理論上不會走到，但型別系統沒鎖住這個保證）。改為 `response.error ?: unexpectedNetworkFailure()` 保底 fallback，補上 `getConfiguration_emits_failure_without_crashing_when_response_has_no_error` 回歸測試。
- [x] 9.4 補上 `dataModule_without_commonModule_fails_to_resolve_movieRepository` negative Koin 測試，鎖住 `MovieRepositoryImpl` 依賴 `commonModule()` 提供的 `CommonDispatcher.IO` qualifier 這個 DI 相依關係（比照既有 `DatastoreModuleTest` 的 `*_without_commonModule_fails_to_resolve_*` 測試慣例）。
- 「KDoc 呈現亂碼」的回報經 `file`／`cat -A` 確認為 codex sandbox 終端編碼問題（false positive），檔案本身為正確 UTF-8，未修改。

修正後重跑 `:shared:testAndroidHostTest`（全數通過，含新增的 4 個回歸/負向測試）與 `:shared:koverVerify`（通過），詳見對話紀錄。
