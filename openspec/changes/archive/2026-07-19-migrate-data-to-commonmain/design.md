## Context

`shared/commonMain` 目前已個別遷移完成三層底層依賴：

- `network`（`ktor-movie-network`）：`MovieDataSource`／`MovieDataSourceImpl`，回傳 `NetworkResponse<T>`。
- `database`（`kmp-movie-local-database`）：`MovieCollectDao`／`MovieHistoryDao`，操作 `MovieCollectEntity`／`MovieHistoryEntity`。
- `datastore`（`kmp-user-preferences-datastore`）：`UserPreferenceDataSource`，提供 `userData: Flow<UserData>` 與 `setConfiguration`／`setThemeMode`／`setLanguageMode`。

參考專案 `JetpackMovieCompose/core/data` 是這三層之上的整合層（`MovieRepository`／`UserDataRepository`），用 Hilt DI、`androidx.paging`（Android-only artifact）撰寫。這次要把這個整合層遷移到 `shared/commonMain`，讓 UI 層（未來的 KMP feature module）可以直接拿到整合後的電影資料 API，不需要重新組裝三個底層依賴。

`shared/build.gradle.kts` 已經有 `androidx.paging:paging-common`（`implementation(libs.androidx.paging.common)`）與 `androidx.room.paging`，是先前 database 遷移時就引入的依賴，本次不需要新增 Gradle 依賴，只需要確認 `PagingSource`／`Pager` 在 commonMain 可用（KMP-ready，Paging 3.3+ 的 `paging-common` 已是純 Kotlin 實作，不依賴 Android `Context`）。

## Goals / Non-Goals

**Goals:**
- 把 `MovieRepository`／`UserDataRepository`（含 `MovieRepositoryImpl`／`UserDataRepositoryImpl`）、`MovieGenrePagingSource`／`MovieSearchPagingSource`、mapper（`asCollectEntity()`／`asHistoryEntity()`）遷移到 `shared/commonMain`，行為與參考專案等價。
- 新增 Koin `dataModule()`，並串接進 `initKoin(...)`。
- 沿用既有 `network`／`database`／`datastore` 三層已提供的 commonMain 元件，不重新定義或修改它們的介面。

**Non-Goals:**
- 不建立獨立 Gradle module（例如 `core:data`）。Module 化留待之後的 change。
- 不新增 UI／ViewModel 層，本次只到 Repository 層為止。
- 不改動 `MovieDataSource`／DAO／`UserPreferenceDataSource` 的既有介面或行為。
- 不引入新的 DI 函式庫或 annotation processor（例如 Koin Annotations / `koin-ksp-compiler`）；qualifier 需求以 Koin 既有 plain DSL 的 `named(...)` 機制達成。

## Decisions

### 1. Repository 程式碼放在 `shared/commonMain/data`，不建立獨立 module

沿用 `migrate-database-to-commonmain`／`migrate-datastore-to-commonmain`／`migrate-common-providers-to-commonmain` 的既有模式：先把程式碼放進 `shared/commonMain`，驗證整合可行且測試通過後，才在未來的 change 評估是否拆成獨立 Gradle module（例如 `core:data`）。現階段拆 module 的邊際成本（多一層 Gradle 設定、跨 module 可見性調整）大於效益，維持與既有三次遷移一致的漸進式作法。

### 2. IO 排程：`commonModule()` 提供帶 qualifier 的 `CoroutineDispatcher` single，`MovieRepositoryImpl` 以建構子參數注入

參考專案用 Hilt `@Dispatcher(CommonDispatcher.IO)` 限定注入哪個 `CoroutineDispatcher`。Koin 的 plain DSL（這個專案目前唯一使用的 Koin 風格，`module { }`）本身就支援等價的 qualifier 機制（`named(...)`），不需要另外引入 `koin-ksp-compiler`（Koin Annotations，另一套走 KSP annotation processor、更接近 Hilt 寫法的函式庫，目前專案完全沒有使用）就能做到「同型別、多個綁定、依 qualifier 區分」。

比照 `commonModule()` 已經集中提供跨層共用依賴（目前是 `CoroutineScope`）的既有模式，本次新增一個帶 qualifier 的 `CoroutineDispatcher` single，供 `data` 層與未來其他 module 共用，不把 dispatcher 綁定寫死在單一 module 裡：

```kotlin
// common/di/CommonModule.kt
enum class CommonDispatcher { IO }

fun commonModule() = module {
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single<CoroutineDispatcher>(qualifier = named(CommonDispatcher.IO)) { Dispatchers.IO }
}
```

`MovieRepositoryImpl` 把 `ioDispatcher: CoroutineDispatcher` 設計成**必要建構子參數（無預設值）**，而不是內部直接呼叫 `Dispatchers.IO`：

```kotlin
class MovieRepositoryImpl(
    private val movieDataSource: MovieDataSource,
    private val movieCollectDao: MovieCollectDao,
    private val movieHistoryDao: MovieHistoryDao,
    private val ioDispatcher: CoroutineDispatcher,
) : MovieRepository
```

`dataModule()` 透過 qualifier 向 `commonModule()` 要這個 dispatcher：

```kotlin
fun dataModule() = module {
    single<MovieRepository> {
        MovieRepositoryImpl(get(), get(), get(), get(qualifier = named(CommonDispatcher.IO)))
    }
    single<UserDataRepository> { UserDataRepositoryImpl(get()) }
}
```

測試不需要透過 Koin，直接 `MovieRepositoryImpl(fake, fake, fake, testDispatcher)` 建構即可，建構子注入仍然保有可測試性；差別只在於正式環境的值改由 `commonModule()` 集中提供、而不是寫死在 `dataModule()` 裡的字面值，讓下一個需要 IO dispatcher 的 module 可以直接 `get(named(CommonDispatcher.IO))`，不必重新決定要不要建立這個綁定。

不給 `ioDispatcher` 預設值的理由不變（見上一版說明）：預設值只有在多個呼叫點想省略重複輸入時才有價值，這裡正式環境的唯一呼叫點（`dataModule()`）本來就要明確寫出要注入哪個 dispatcher。

只新增 `CommonDispatcher.IO` 一個成員，不比照 Hilt 版本一次定義 `IO`／`DEFAULT`／`MAIN`：目前只有 `data` 層需要 IO dispatcher，其餘成員等到真的有消費者時再加，避免預先定義用不到的列舉值。

### 3. Mapper 沿用既有 `asExtendedModel()`，只新增反向轉換

`database.entity.MovieCollectEntity.asExtendedModel()`／`MovieHistoryEntity.asExtendedModel()`（entity → `MovieCardResult`）已經在 database 遷移時建立好，直接沿用、不重複定義。本次只需要在 `data.model.MovieMapper` 新增反向轉換：`MovieCardResult.asCollectEntity()`／`MovieCardResult.asHistoryEntity()`（model → entity），對應參考專案 `core/data/model/MovieMapper.kt` 的邏輯。

### 4. Paging：沿用 `androidx.paging` KMP common 依賴，PagingSource 直接搬移

`MovieGenrePagingSource`／`MovieSearchPagingSource` 是純 Kotlin 邏輯（呼叫 `MovieDataSource`、組 `LoadResult.Page`／`LoadResult.Error`），沒有任何 Android-only API，可以逐字遷移到 `shared/commonMain/data/paging`，只需要把 import 換成 commonMain 版本的 `MovieDataSource`／`NetworkResponse`／`MovieCardResult`。`Pager` 的組裝邏輯留在 `MovieRepositoryImpl.getMovieListPager()`／`getMovieSearchPager()`，比照參考專案的 `PagingConfig`（`pageSize = 20`、`enablePlaceholders = false`、`initialLoadSize = 20`、`prefetchDistance = 2`）。

### 5. Koin `dataModule()` 不需要平台專屬參數，直接安裝進 `initKoin(...)`

`dataModule()` 依賴的 `MovieDataSource`／`MovieCollectDao`／`MovieHistoryDao`／`UserPreferenceDataSource` 都已由 `networkModule`／`databaseModule`／`datastoreModule` 提供，本身沒有任何 expect/actual 或平台專屬邏輯，因此 `dataModule()` 是純 commonMain 函式，`initKoin(...)` 的簽名不需要新增參數，只需要在 `modules(...)` 清單加入 `dataModule()`。`androidApp`／`iosMain` 呼叫 `initKoin(...)` 的既有程式碼不需要修改。

## Risks / Trade-offs

- **[Risk]** `MovieRepositoryImpl`／`UserDataRepositoryImpl` 的單元測試需要假的 `MovieDataSource`／DAO／`UserPreferenceDataSource`，若沒有現成 test double 需要自行建立 fake，增加測試程式碼量。
  → **Mitigation**：`network` 遷移時已經有 `MovieDataSourceTestSupport`（`commonTest/network/datasource`）可參考同樣手法建立 fake `MovieDataSource`；DAO／`UserPreferenceDataSource` 沿用既有 `InMemoryPreferencesDataStore`（datastore 測試已有）與簡單的 in-memory fake DAO（不需要真正的 Room 連線）。
- **[Risk]** iOS 模擬器測試在 Windows 主機無法執行（沿用先前三次遷移已記錄的既有環境限制），`dataModule()` 在 iOS 平台的 resolve 行為無法在本機驗證。
  → **Mitigation**：`dataModule()` 本身沒有平台專屬程式碼，只依賴既有已驗證過的 module，風險低；比照先前作法在 tasks.md 記錄環境限制，不影響 commonMain／Android host test 的驗證完整性。
- **[Risk]** Paging（`Pager`／`PagingSource`）在 Kotlin/Native（iOS）上的實際執行行為，本機無法驗證（同樣受限於 iOS 模擬器測試無法執行）。
  → **Mitigation**：`PagingSource` 邏輯不含平台專屬 API，且 `paging-common` 官方文件已列為 KMP-ready 函式庫；改動範圍限於邏輯搬移，非新邏輯，風險可接受。

## Migration Plan

1. 建立 `shared/commonMain/data`（`repository`、`paging`、`model`、`di`）package，遷移程式碼。
2. 新增 `dataModule()`，擴充 `initKoin(...)` 安裝它。
3. 補上單元測試（repository 行為、Koin resolve、mapper）。
4. 執行 `:shared:testAndroidHostTest`、`:androidApp:assembleDebug`、`koverVerify` 確認不破壞既有功能。
5. 沒有 rollback 特殊步驟——這是新增 package，未修改任何既有 commonMain 程式碼的公開介面，revert commit 即可完整還原。

## Open Questions

- 未來 module 化（`core:data`）時，是否需要把 `data` 對 `database`／`datastore`／`network` 的依賴收斂成 `api` 還是 `implementation`？留待 module 化的 change 決定，本次不處理。
