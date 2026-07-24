## Why

`kotlin.Result<T>` 匯出給 iOS 後，Swift 端只能拿到 opaque boxed value（`Optional(Success(...))`），無法用
`as? ConfigurationBean` 判斷成功/失敗，SKIE 也未對這個泛型型別的匯出行為提供正式保證。目前只能靠
`shared/app` iosMain 手寫一個 `IosConfigurationLoader`／`IosConfigurationLoadState` wrapper，把
`Result<ConfigurationBean>` 拆成 Swift 看得懂的明確 state；這個 wrapper 每個 UseCase 都要各自重寫一次，
無法規模化。需要在 `shared/common` 定義一個能被 SKIE 明確匯出成 Swift enum 的通用 `AppResult<T>` /
`AppError`，讓 UseCase／Repository 直接回傳它，不再需要個別 wrapper。

## What Changes

- 在 `shared/common` 新增 `AppResult<out T>`（`sealed interface`，`Success<T>`／`Failure`）與
  `AppError`（`sealed interface`，本次先定義 `Network(val exception: NetworkException)` 與 `Unknown` 兩種，
  其餘分類留待後續遷移其他 UseCase 時再擴充）。
- **BREAKING**：`MovieRepository.getConfiguration()` 回傳型別由 `Flow<Result<ConfigurationBean>>`
  改為 `Flow<AppResult<ConfigurationBean>>`；`GetConfigurationUseCase` 的回傳型別同步改為
  `Flow<AppResult<ConfigurationBean>>`。既有「失敗時若本地有快取則靜默 fallback 回傳成功」的行為不變，
  只換型別，不換語意。
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
- `kmp-movie-data-repository`：`MovieRepository.getConfiguration()` 的 requirement 改為描述
  `AppResult` 回傳語意。
- `kmp-movie-domain-usecases`：`GetConfigurationUseCase` 的 requirement 改為描述 `AppResult`
  回傳語意，快取 fallback 行為維持不變。
- `ios-koin-bridge`：移除 `KoinHelper.getConfigurationLoader()` 相關 requirement／scenario。
- `ios-splash-screen`：`SplashViewModel` 取得 configuration 的方式改為直接消費
  `GetConfigurationUseCase` 回傳的 `AppResult`，不再透過 `IosConfigurationLoader`。

## Impact

- **`shared/common`**：新增 `AppResult`、`AppError`（`commonMain`），需補單元測試。
- **`shared/data`**：`MovieRepository`（介面）與 `MovieRepositoryImpl`（`getConfiguration()`）改回傳
  `AppResult`；既有測試需同步調整。
- **`shared/domain`**：`GetConfigurationUseCase` 改回傳 `AppResult`；既有測試需同步調整。
- **`androidApp`**：`MainViewModel`、`MainUiState` 改消費 `AppResult`；不涉及 UI 畫面顯示邏輯變更。
- **`shared/app`（iosMain）／`iosApp`（Swift）**：`IosConfigurationLoader`／`IosConfigurationLoadState`／
  `KoinHelper.getConfigurationLoader()` 需配合移除或調整，此部分由使用者自行處理，不在本 change
  的 tasks.md 實作範圍內，但相關 spec delta 會納入本 change 一併更新以定義目標契約。
- 不新增外部依賴，不涉及 `buildSrc` 或 `gradle/libs.versions.toml` 改動。
- 不涉及 Room schema／資料庫 migration。
