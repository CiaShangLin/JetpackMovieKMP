## 1. shared/data

- [x] 1.1 `MovieRepository.kt`：將 `getMovieActor(id: Int): Flow<Result<MovieCastAndCrewBean>>` 簽名改為 `Flow<AppResult<MovieCastAndCrewBean>>`
- [x] 1.2 `MovieRepositoryImpl.kt`：調整 `getMovieActor` 實作，比照 `getMovieGenres` 的轉換方式，將成功／失敗轉為 `AppResult.Success`／`AppResult.Failure(error.toAppError())`
- [x] 1.3 補齊／調整 `shared/data` 對應的單元測試（`MovieRepositoryImplTest` 或同等測試檔），驗證 `getMovieActor` 成功與失敗情境皆回傳正確的 `AppResult`
- [x] 1.4 執行 `./gradlew :shared:data:koverVerify` 確認覆蓋率仍達 80% 下限

## 2. shared/domain

- [x] 2.1 `GetMovieDetailUseCase.kt`：回傳型別改為 `Flow<AppResult<MovieDetailBean>>`，沿用 `GetConfigurationUseCase` 的 `transform { result.fold(...) }` 轉換模式，`MovieRepository.getMovieDetail(movieId)` 呼叫維持不變
- [x] 2.2 `GetMovieRecommendUseCase.kt`：回傳型別改為 `Flow<AppResult<List<MovieCardResult>>>`，同樣沿用轉換模式，維持既有的收藏狀態標記邏輯
- [x] 2.3 調整 `shared/domain` 對應的單元測試，驗證兩個 UseCase 在成功／失敗情境下皆回傳正確的 `AppResult`（含 `GetMovieDetailUseCase` 的瀏覽紀錄寫入行為、`GetMovieRecommendUseCase` 的收藏狀態標記行為維持不變）
- [x] 2.4 執行 `./gradlew :shared:data:testAndroidHostTest` 確認 domain 層測試（依 module 結構可能位於 `shared/domain` 對應測試 task）全數通過

## 3. shared/app（iOS 匯出邊界）

- [x] 3.1 檢查 `KoinHelper.kt`（`shared/app/src/iosMain`）是否需要新增或調整 `getMovieRecommendUseCase()`／`getMovieActor` 相關 accessor，確保回傳型別已對應 `AppResult`
- [x] 3.2 調整 `shared/app` 的 `commonTest`／`iosTest` 中對 `MovieRepository`／`GetMovieDetailUseCase`／`GetMovieRecommendUseCase` 的 fake 實作（例如 `AppDiagnosticsTest`、`SearchMovieListPresenterTest` 所在檔案），改用 `AppResult`
- [x] 3.3 執行 `./gradlew :shared:app:iosSimulatorArm64Test`（或對應 iOS 測試 task）確認 iOS 端測試通過

## 4. feature/detail（Android 呼叫端）

- [x] 4.1 `MovieDetailViewModel.kt`：將 `getMovieDetailUseCase`、`getMovieRecommendUseCase`、`movieRepository.getMovieActor` 三處的 `Result.fold(onSuccess, onFailure)` 消費方式改為對應 `AppResult.Success`／`AppResult.Failure` 的處理，維持既有 `MovieDetailUiState`／`DetailSectionState` 的 Success／Error 對應行為不變
- [x] 4.2 更新 `MovieDetailViewModelTest`（`feature/detail/src/test`）中的 fake 實作與斷言，改用 `AppResult`
- [x] 4.3 執行 `feature/detail` 對應單元測試確認通過

## 5. 其餘模組 fake 實作同步

- [x] 5.1 `feature/home/src/test/.../HomeViewModelTestFakes.kt`：`getMovieActor` fake 實作改回傳 `AppResult`（若有引用 `GetMovieDetailUseCase`／`GetMovieRecommendUseCase` 一併調整）
- [x] 5.2 `feature/collect/src/test/.../FakeMovieRepository.kt`：`getMovieActor` fake 實作改回傳 `AppResult`
- [x] 5.3 `feature/search/src/test/.../SearchViewModelTest.kt`：`getMovieActor` fake 實作改回傳 `AppResult`
- [x] 5.4 `feature/history/src/test/.../FakeMovieRepository.kt`：`getMovieActor` fake 實作改回傳 `AppResult`
- [x] 5.5 `androidApp/src/test/kotlin/.../MainViewModelTestFakes.kt`：`getMovieActor` fake 實作改回傳 `AppResult`
- [x] 5.6 逐一執行各模組單元測試（`./gradlew :feature:home:testDebugUnitTest`、`:feature:collect:...`、`:feature:search:...`、`:feature:history:...`、`:androidApp:testDebugUnitTest` 等），確認編譯與測試皆通過，無遺漏的 `Result` 型別參照

## 6. 全域驗證

- [x] 6.1 執行 `./gradlew ktlintCheck` 確保格式符合規範
- [x] 6.2 執行 `./gradlew check` 確認整體建置（含所有單元測試）通過
- [x] 6.3 執行 `./gradlew :androidApp:assembleDebug` 確認 Android app 可正常建置
- [x] 6.4 全文搜尋確認 `GetMovieDetailUseCase`、`GetMovieRecommendUseCase`、`MovieRepository.getMovieActor` 已無殘留的 `kotlin.Result` 呼叫端或 fake 實作
