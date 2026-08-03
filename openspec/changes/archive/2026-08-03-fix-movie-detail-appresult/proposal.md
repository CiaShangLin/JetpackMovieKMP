## Why

`GetMovieDetailUseCase`、`GetMovieRecommendUseCase` 與 `MovieRepository.getMovieActor` 目前皆回傳 `Flow<kotlin.Result<T>>`。Kotlin 標準庫的 `Result` 在匯出到 iOS（Kotlin/Native + SKIE）時會被 type-erase 成 opaque boxed value，Swift 端無法用 `switch`／`onEnum(of:)` 解析成功或失敗分支，導致這三個方法實質上無法被 iOS 消費。專案已有 `AppResult`（`sealed interface`，可被 SKIE 明確匯出成 Swift enum）與對應轉換慣例（`GetConfigurationUseCase`、`MovieRepository.getMovieGenres`），本次比照既有慣例修復這三個方法，讓電影詳情頁的資料流可以在 iOS 端正確運作。

## What Changes

- `GetMovieDetailUseCase.invoke(movieId)` 回傳型別由 `Flow<Result<MovieDetailBean>>` 改為 `Flow<AppResult<MovieDetailBean>>`；沿用 `GetConfigurationUseCase` 的轉換模式，在 UseCase 層將 `MovieRepository.getMovieDetail(movieId)` 回傳的 `Result` 轉換為 `AppResult`，`MovieRepository` 本身不需要跟著改動。**BREAKING**（Android 呼叫端需同步調整消費方式）
- `GetMovieRecommendUseCase.invoke(movieId)` 回傳型別由 `Flow<Result<List<MovieCardResult>>>` 改為 `Flow<AppResult<List<MovieCardResult>>>`，轉換方式同上。**BREAKING**
- `MovieRepository.getMovieActor(id)` 回傳型別由 `Flow<Result<MovieCastAndCrewBean>>` 改為 `Flow<AppResult<MovieCastAndCrewBean>>`。因為 `getMovieActor` 沒有 UseCase 包裝層（`MovieDetailViewModel` 直接呼叫 Repository），比照 `getMovieGenres` 的既有慣例，直接在 Repository 實作層轉換，不額外新增 UseCase。**BREAKING**
- 同步調整 Android 端唯一呼叫端 `MovieDetailViewModel`：三個資料流的 `.fold(onSuccess, onFailure)` 消費方式改為 `AppResult.Success`／`AppResult.Failure` 的對應處理。
- 同步更新受影響的測試假實作（fake repository／fake usecase）與既有單元測試，改用 `AppResult`。

## Capabilities

### New Capabilities

（無）

### Modified Capabilities

- `kmp-movie-domain-usecases`：`GetMovieDetailUseCase`、`GetMovieRecommendUseCase` 的回傳型別由 `Result` 改為 `AppResult`，成功／失敗判斷邏輯不變，僅型別包裝改變
- `kmp-movie-data-repository`：`MovieRepository.getMovieActor` 的回傳型別由 `Result` 改為 `AppResult`

## Impact

- **受影響模組**：
  - `shared/domain`（`GetMovieDetailUseCase.kt`、`GetMovieRecommendUseCase.kt`）
  - `shared/data`（`MovieRepository.kt` 介面、`MovieRepositoryImpl.kt` 實作）
  - `feature/detail`（`MovieDetailViewModel.kt` 三處呼叫端）
  - 測試假實作：`feature/home`、`feature/collect`、`feature/search`、`feature/history`、`androidApp`、`shared/app`（`commonTest`／`iosTest`）等模組中對 `MovieRepository`／`GetMovieDetailUseCase`／`GetMovieRecommendUseCase` 的 fake 實作
- **不受影響**：`shared/common`（`AppResult`／`AppError` 定義本身不變）、`iosApp`（目前尚未有程式碼呼叫這三個方法，屬前置修復）
- **待確認事項**（於 design.md 討論）：`shared/data` 的 `build.gradle.kts` 目前對 `shared.common` 使用 `implementation(...)` 而非 `api(...)`，但公開介面已回傳 `AppResult`，需評估是否改為 `api(...)` 以避免依賴傳遞脆弱性
