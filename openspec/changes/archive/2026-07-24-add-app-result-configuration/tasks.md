## 1. shared/common（本次範圍：Android／共用）

- [x] 1.1 新增 `AppError`（`sealed class`，繼承 `kotlin.Exception`，package `com.shang.jetpackmoviekmp.common`）：`Network(val exception: NetworkException)`（`cause` 即為 `exception`）、`Unknown` 兩個子型別。繼承 `Exception` 是為了讓呼叫端可直接把 `AppError` 當 `Throwable` 持有，不需要額外轉換；`AppError` 本身不會被 `throw`，一律經 `AppResult.Failure` 當資料值傳遞
- [x] 1.2 新增 `AppResult<out T>`（`sealed interface`，同 package）：`Success<T>(val data: T)`、`Failure(val error: AppError) : AppResult<Nothing>` 兩個子型別
- [x] 1.3 新增 `Throwable.toAppError()` extension（`NetworkException` 對應 `AppError.Network`，其餘歸為 `AppError.Unknown`），供 `GetConfigurationUseCase` 轉換用
- [x] 1.4 補上 `shared/common` 的單元測試（AAA），驗證 `AppResult.Success`／`Failure` 建構行為、`AppError.Network` 攜帶的 `NetworkException` 正確保留（含 `cause` 一致）、`AppError` 可當 `Throwable` 直接持有，以及 `toAppError()` 的兩種分類

## 2. shared/domain（本次範圍：Android／共用）

`MovieRepository`／`MovieRepositoryImpl` 維持回傳 `Flow<Result<ConfigurationBean>>` 不變——它是內部實作，不會被 iOS 直接呼叫。實際會被 iOS 端透過 `KoinHelper.getConfigurationUseCase()` 呼叫的邊界是 `GetConfigurationUseCase`，因此型別轉換只發生在這一層。

- [x] 2.1 修改 `GetConfigurationUseCase` 回傳型別為 `Flow<AppResult<ConfigurationBean>>`：內部呼叫 `MovieRepository.getConfiguration()`（仍是 `Flow<Result<ConfigurationBean>>`）並用 `result.fold(...)` 消化，把原本 `Result.success`／`Result.failure` 的 emit 改為 `AppResult.Success`／`AppResult.Failure(it.toAppError())`；快取寫入／fallback 邏輯（成功寫快取、失敗有快取則 fallback 為 Success、失敗無快取回傳 Failure）維持不變
- [x] 2.2 更新 `GetConfigurationUseCase` 對應測試的三個案例（成功寫入快取並回傳成功／失敗但有快取時退回快取／失敗且無快取時回傳原始錯誤），斷言改為對 `AppResult` 的型別檢查

## 3. androidApp（本次範圍：Android）

- [x] 3.1 修改 `MainViewModel`，將 `getConfigurationUseCase().map { result -> result.fold(...) }` 改為消費 `AppResult`（`when (result) { is AppResult.Success -> ...; is AppResult.Failure -> ... }`）
- [x] 3.2 `MainUiState.Error` 維持 `Throwable` 欄位型別不變；因 `AppError` 已繼承 `Exception`，`MainViewModel` 直接 `MainUiState.Error(result.error)` 即可，不需要額外的轉換函式
- [x] 3.3 執行 `./gradlew :androidApp:assembleDebug` 驗證編譯成功
- [x] 3.4 手動啟動 App，驗證 Loading／Error／Success 三態顯示行為與改動前一致（含網路離線時觸發 Error 狀態的手動驗證）——已由使用者確認 UI 測試沒有問題

## 4. 連帶調整（因介面簽名變更而需同步修正的既有程式碼）

- [x] 4.1 `shared/app` 的 `AppDiagnosticsTest`：`FakeMovieRepository` 不受影響（`MovieRepository` 介面未變），只需把預期字串 `Result.success(configuration).toString()` 改為 `AppResult.Success(configuration).toString()`

## 5. 整體驗證

- [x] 5.1 執行 `./gradlew ktlintCheck` 確認格式通過（BUILD SUCCESSFUL）
- [x] 5.2 執行 `./gradlew :shared:common:testAndroidHostTest :shared:app:testAndroidHostTest :shared:data:testAndroidHostTest` 確認皆通過（BUILD SUCCESSFUL）。`:shared:domain:testAndroidHostTest` 因 pre-existing 問題無法執行——見下方說明
- [x] 5.3 執行 `./gradlew check`：目前於 `:shared:domain:compileAndroidHostTest` 失敗，原因為 `shared/domain` 測試 source set 缺少 `shared:network`／`shared:database`／`shared:datastore` 相關測試依賴，出現 `Room`、`DataStore`、`networkModule`、`databaseModule` 等 unresolved reference；這是既有測試依賴問題，不屬於本 change 修正範圍。Android 相關的 ktlint、`androidApp` 編譯、`shared/common`／`shared/app`／`shared:data` 測試已個別驗證通過

**已知問題（與本次改動無關，不在本 change 修正範圍）**：`shared:domain` 的
`compileAndroidHostTest` 本身編譯失敗（`TestDatabaseBuilder.kt`／`InMemoryPreferencesDataStore.kt`／
`DomainModuleTest.kt` 缺少對 `shared:network`／`shared:database`／`shared:datastore` 的測試依賴，
`Unresolved reference` 大量報錯）。已用 `git stash` 驗證：把本次所有改動（含新增檔案）暫存後，
在同一個 commit 上執行 `:shared:domain:compileAndroidHostTest` 出現完全相同的錯誤，證實這是
與 `AppResult`／`AppError` 改動無關的既有環境問題，不屬於本 change 範圍，建議另開 issue／change
處理。因此 `GetConfigurationUseCaseTest.kt` 的新斷言邏輯正確性已透過程式碼審閱與型別檢查確認，
但無法用 `./gradlew` 實際執行驗證。

## 6. 本次範圍外（不列入本 change 任務，供使用者對齊 iOS 端實作參考）

以下項目由使用者於 `shared/app` iosMain 與 `iosApp` 自行處理，對應的目標契約已定義於
`specs/ios-koin-bridge/spec.md`、`specs/ios-splash-screen/spec.md`：

- 移除 `shared/app` iosMain 的 `IosConfigurationLoader.kt`（`IosConfigurationLoader`／`IosConfigurationLoadState`）
- 調整 `KoinHelper.kt`：移除 `getConfigurationLoader()` accessor
- 調整 `iosApp/iosApp/Splash/SplashViewModel.swift`：改為直接呼叫
  `KoinHelper.shared.getConfigurationUseCase()`，消費 `Flow<AppResult<ConfigurationBean>>`
  並轉換為既有的 `SplashUiState`
- 其餘 4 個仍回傳 `kotlin.Result<T>` 的方法／UseCase（`getMovieGenres`、`getMovieDetail`、
  `getMovieRecommendations`、`getMovieActor`）之後續遷移，需另開 change 處理
