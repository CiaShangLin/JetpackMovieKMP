## Context

`shared/commonMain` 目前已遷移完成四層依賴：

- `network`（`ktor-movie-network`）：`MovieDataSource`，回傳 `NetworkResponse<T>`。
- `database`（`kmp-movie-local-database`）：`MovieCollectDao`／`MovieHistoryDao`。
- `datastore`（`kmp-user-preferences-datastore`）：`UserPreferenceDataSource`。
- `data`（`kmp-movie-data-repository`）：`MovieRepository`／`UserDataRepository`，整合上述三層，並已在 `common/di/CommonModule.kt` 建立帶 `named(CommonDispatcher.IO)` qualifier 的 `CoroutineDispatcher` single。

參考專案 `JetpackMovieCompose/core/domain` 是 `core/data` 之上的 UseCase 層，用 Hilt `@Inject constructor` 注入 `MovieRepository`／`UserDataRepository`／`ioDispatcher`。這次要把這層遷移到 `shared/commonMain`，讓 UI 層（未來的 KMP feature module）可以直接拿到「已合併收藏狀態」「API 失敗自動退回快取」等業務邏輯完整的 UseCase，不需要重新組裝 Repository 呼叫。

5 個 UseCase 都是純 Kotlin coroutines／Flow 邏輯（`combine`／`transform`／`flowOn`／`onEach`），唯一需要留意的平台相依是 `GetHomeMovieListUseCase` 用到的 `androidx.paging.cachedIn`（`PagingData<T>.cachedIn(CoroutineScope)`）——此擴充函式屬於 `androidx.paging:paging-common`，`shared/build.gradle.kts` 已具備此依賴（`data` 遷移時引入），純 Kotlin 實作、無 Android-only API。

## Goals / Non-Goals

**Goals:**
- 把 5 個 UseCase（`GetConfigurationUseCase`、`GetHistoryMovieListUseCase`、`GetHomeMovieListUseCase`、`GetMovieDetailUseCase`、`GetMovieRecommendUseCase`）遷移到 `shared/commonMain`，行為與參考專案等價。
- 新增 Koin `domainModule()`，並串接進 `initKoin(...)`。
- 沿用既有 `data` 層已提供的 `MovieRepository`／`UserDataRepository`／`CommonDispatcher.IO`，不重新定義或修改它們的介面。

**Non-Goals:**
- 不建立獨立 Gradle module（例如 `core:domain`）。Module 化留待之後的 change。
- 不新增 UI／ViewModel 層，本次只到 UseCase 層為止。
- 不改動 `MovieRepository`／`UserDataRepository`／`CommonModule.kt` 的既有介面或行為。
- 不引入新的 DI 函式庫或 Gradle 依賴。

## Decisions

### 1. UseCase 程式碼放在 `shared/commonMain/domain`，不建立獨立 module

延續 Repository 模式：先把程式碼放進 `shared/commonMain`，驗證整合可行且測試通過後，才在未來的 change 評估是否拆成獨立 Gradle module（例如 `core:domain`）。沿用 `migrate-data-to-commonmain` 已確立的漸進式作法，維持一致性。

### 2. UseCase 建構子沿用 Repository 模式：一般 constructor + 必要 `ioDispatcher` 參數，Koin 用 `factory` 而非 `single`

參考專案用 Hilt `@Inject constructor` + `@Dispatcher(CommonDispatcher.IO)` 注入。KMP 專案已在 `data` 遷移時決定用 Koin plain DSL 的 `named(...)` qualifier 機制取代 Hilt annotation，本次沿用同一模式：

```kotlin
class GetHistoryMovieListUseCase(
    private val movieRepository: MovieRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {
    operator fun invoke(): Flow<List<MovieCardResult>> { ... }
}
```

`domainModule()` 用 `factory` 而非 `single` 綁定這 5 個 UseCase——UseCase 本身沒有內部可變狀態（純粹包裝 Repository 呼叫），且 `MovieRepository`／`UserDataRepository`／`CoroutineDispatcher` 三個依賴都已是 Koin `single`，用 `factory` 不會重複建立底層資源，只是每次注入產生一個輕量的新 UseCase wrapper 實例，避免在多個呼叫端（例如不同 ViewModel）之間意外共用同一個 UseCase 物件的疑慮，也比較貼近「UseCase 是一次性操作物件」的一般認知，而非刻意要求它是 singleton：

```kotlin
fun domainModule() = module {
    factory { GetConfigurationUseCase(get(), get(), get(qualifier = named(CommonDispatcher.IO))) }
    factory { GetHistoryMovieListUseCase(get(), get(qualifier = named(CommonDispatcher.IO))) }
    factory { GetHomeMovieListUseCase(get(), get(qualifier = named(CommonDispatcher.IO))) }
    factory { GetMovieDetailUseCase(get(), get(qualifier = named(CommonDispatcher.IO))) }
    factory { GetMovieRecommendUseCase(get(), get(qualifier = named(CommonDispatcher.IO))) }
}
```

測試不需要透過 Koin，直接 `GetXxxUseCase(fakeRepository, testDispatcher)` 建構即可。

### 3. 測試改用 fake `MovieRepository`／`UserDataRepository`（介面層級），而非沿用 `data` 層的底層 fake

`data` 層既有的測試 fake（`commonTest/data`）是針對 `MovieDataSource`／DAO／`UserPreferenceDataSource` 這些底層元件而寫，`domain` 層的 UseCase 依賴的是 `MovieRepository`／`UserDataRepository` 介面本身，兩者不是同一層，無法直接沿用。本次在 `commonTest/domain` 新增 `FakeMovieRepository`／`FakeUserDataRepository`（實作介面全部方法，用 `MutableStateFlow`／可控制的回傳值模擬各種情境），供 5 個 UseCase 的行為測試共用。

考慮過的替代方案：用 MockK 之類的 mocking 函式庫直接 mock 介面。專案目前 `commonTest` 沒有引入任何 mocking 函式庫（`data`／`database`／`datastore` 遷移都是手寫 fake），且 MockK 在 Kotlin/Native（iOS）上的支援有限，手寫 fake 維持與既有測試風格一致、也能在所有 target 執行，故不採用。

### 4. `GetHomeMovieListUseCase` 的 `cachedIn` 與 `CoroutineScope` 參數維持不變

參考專案的 `invoke(withGenres, viewModelScope)` 需要呼叫端傳入 `CoroutineScope` 給 `cachedIn`，本次直接沿用相同簽名，UseCase 本身不持有或建立 `CoroutineScope`，符合「由呼叫端決定生命週期」的既有註解與設計意圖，無需更動。

## Risks / Trade-offs

- **[Risk]** 手寫 `FakeMovieRepository`／`FakeUserDataRepository` 需要實作介面全部方法（`MovieRepository` 有 13 個方法），即使 UseCase 只用到其中少數幾個，也增加測試程式碼量。
  → **Mitigation**：比照既有 fake 的作法，未使用到的方法回傳合理預設值（例如空 `Flow`／`TODO()` 明確標示未使用），只針對每個 UseCase 測試實際會呼叫到的方法客製化回傳值。
- **[Risk]** `GetHomeMovieListUseCase`／`GetMovieRecommendUseCase`／`GetHistoryMovieListUseCase` 依賴 `combine` 合併兩個 Flow（電影清單／推薦與收藏 id），測試需要確保兩個來源 Flow 的 emission 時機正確才能驗證合併結果，容易寫出 flaky test。
  → **Mitigation**：測試中的 fake Flow 一律用 `MutableStateFlow`（有初始值、不會掛起），避免依賴 emission 時序；分頁相關（`GetHomeMovieListUseCase`）比照 `data` 層既有作法，只做「可正常 collect 不拋例外」的 smoke test，不斷言 `PagingData` 實際內容（專案未引入 `paging-testing`）。
- **[Risk]** iOS 模擬器測試在 Windows 主機無法執行（沿用先前四次遷移已記錄的既有環境限制）。
  → **Mitigation**：`domainModule()`／5 個 UseCase 皆為純 Kotlin 邏輯，不含平台專屬程式碼，風險低；比照先前作法在 tasks.md 記錄環境限制。

## Migration Plan

1. 建立 `shared/commonMain/domain`（`usecase`、`di`）package，遷移 5 個 UseCase。
2. 新增 `domainModule()`，擴充 `initKoin(...)` 安裝它。
3. 新增 `commonTest/domain` fake Repository，補上 5 個 UseCase 行為測試與 Koin resolve 測試。
4. 執行 `:shared:testAndroidHostTest`、`:androidApp:assembleDebug`、`koverVerify` 確認不破壞既有功能。
5. 沒有 rollback 特殊步驟——這是新增 package，未修改任何既有 commonMain 程式碼的公開介面，revert commit 即可完整還原。

## Open Questions

- 未來 module 化（`core:domain`）時，是否需要把 `domain` 對 `data` 的依賴收斂成 `api` 還是 `implementation`？留待 module 化的 change 決定，本次不處理。
