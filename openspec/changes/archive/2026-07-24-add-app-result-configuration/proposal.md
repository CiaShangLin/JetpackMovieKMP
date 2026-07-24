## Why

`kotlin.Result<T>` 匯出給 iOS 後，Swift 端只能拿到 opaque boxed value（`Optional(Success(...))`），無法用
`as? ConfigurationBean` 判斷成功/失敗，SKIE 也未對這個泛型型別的匯出行為提供正式保證。目前只能靠
`shared/app` iosMain 手寫一個 `IosConfigurationLoader`／`IosConfigurationLoadState` wrapper，把
`Result<ConfigurationBean>` 拆成 Swift 看得懂的明確 state；這個 wrapper 每個 UseCase 都要各自重寫一次，
無法規模化。需要在 `shared/common` 定義一個能被 SKIE 明確匯出成 Swift enum 的通用 `AppResult<T>` /
`AppError`，讓 UseCase／Repository 直接回傳它，不再需要個別 wrapper。

## What Changes

- 在 `shared/common` 新增 `AppResult<out T>`（`sealed interface`，`Success<T>`／`Failure`）與
  `AppError`（`sealed class`，**繼承 `kotlin.Exception`**，本次先定義 `Network(val exception:
  NetworkException)` 與 `Unknown` 兩種，其餘分類留待後續遷移其他 UseCase 時再擴充），並提供
  `Throwable.toAppError()` 轉換 extension。`AppError` 繼承 `Exception` 只是為了讓型別本身「是一個」
  `Throwable`，方便呼叫端直接持有；`AppError` 本身不會被 `throw`，一律透過 `AppResult.Failure`
  當作一般資料值經由 `Flow` 傳遞，不涉及 Kotlin/Native 的例外跨界拋出機制（`@Throws`／`NSError` 轉換）。
- **BREAKING**：`GetConfigurationUseCase` 的回傳型別由 `Flow<Result<ConfigurationBean>>` 改為
  `Flow<AppResult<ConfigurationBean>>`。轉換只發生在這一層——`MovieRepository.getConfiguration()`
  維持 `Flow<Result<ConfigurationBean>>` 不變，因為它是內部實作，不會被 iOS 直接呼叫；真正會被
  iOS 端透過 `KoinHelper.getConfigurationUseCase()` 呼叫的邊界是 `GetConfigurationUseCase`，型別轉換
  只需要發生在「最後輸出會被直接呼叫的地方」。既有「失敗時若本地有快取則靜默 fallback 回傳成功」的
  行為不變，只換對外型別，不換語意。
- 其餘 4 個目前回傳 `Result<T>` 的 `MovieRepository`／UseCase 方法（`getMovieGenres`、
  `getMovieDetail`、`getMovieRecommendations`、`getMovieActor`）本次不動，維持
  `kotlin.Result<T>`，留待後續 change 個別遷移。
- **BREAKING**：`androidApp` 的 `MainViewModel`／`MainUiState` 同步改為消費 `AppResult`（不再用
  `kotlin.Result.fold`）。
- **BREAKING**：移除 `shared/app` iosMain 的 `IosConfigurationLoader`／`IosConfigurationLoadState`
  wrapper 與 `KoinHelper.getConfigurationLoader()`；改由 iOS 端直接消費
  `GetConfigurationUseCase` 回傳的 `Flow<AppResult<ConfigurationBean>>`。此段 iosMain Kotlin
  與對應 Swift 端調整（`SplashViewModel.swift` 等）**不在本次 change 的實作任務範圍**，由使用者
  另行處理；本 change 只在 `design.md`／spec delta 中定義新的 Kotlin/Swift 邊界契約供對齊。

## Capabilities

### New Capabilities

（無新增能力，`AppResult`／`AppError` 併入既有 `common-kernel` 能力範圍）

### Modified Capabilities

- `common-kernel`：新增 `AppResult<T>`／`AppError` 型別定義的 requirement。
- `kmp-movie-domain-usecases`：`GetConfigurationUseCase` 的 requirement 改為描述 `AppResult`
  回傳語意，快取 fallback 行為維持不變；`kmp-movie-data-repository`（`MovieRepository`）不受影響，
  不列入本次 Modified Capabilities。
- `ios-koin-bridge`：移除 `KoinHelper.getConfigurationLoader()` 相關 requirement／scenario。
- `ios-splash-screen`：`SplashViewModel` 取得 configuration 的方式改為直接消費
  `GetConfigurationUseCase` 回傳的 `AppResult`，不再透過 `IosConfigurationLoader`。

## Impact

- **`shared/common`**：新增 `AppResult`、`AppError`、`Throwable.toAppError()`（`commonMain`），需補單元測試。
- **`shared/data`**：不變更。`MovieRepository`／`MovieRepositoryImpl` 維持 `Flow<Result<ConfigurationBean>>`。
- **`shared/domain`**：`GetConfigurationUseCase` 改回傳 `AppResult`（內部消化 `MovieRepository` 的
  `Result` 並轉換）；既有測試需同步調整。
- **`androidApp`**：`MainViewModel` 改消費 `AppResult`；`MainUiState.Error` 欄位型別維持 `Throwable`
  不變，因 `AppError` 本身即為 `Throwable`，`result.error` 可直接傳入，不需要額外的轉換函式，不涉及
  UI 畫面顯示邏輯變更。
- **`shared/app`（commonMain）**：`AppDiagnostics` 透傳 `GetConfigurationUseCase` 結果，無需改邏輯；
  對應測試（`AppDiagnosticsTest`）的預期字串需同步更新。
- **`shared/app`（iosMain）／`iosApp`（Swift）**：`IosConfigurationLoader`／`IosConfigurationLoadState`／
  `KoinHelper.getConfigurationLoader()` 需配合移除或調整，此部分由使用者自行處理，不在本 change
  的 tasks.md 實作範圍內，但相關 spec delta 會納入本 change 一併更新以定義目標契約。
- 不新增外部依賴，不涉及 `buildSrc` 或 `gradle/libs.versions.toml` 改動。
- 不涉及 Room schema／資料庫 migration。
