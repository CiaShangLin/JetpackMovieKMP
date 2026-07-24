## 1. shared/common（本次範圍：Android／共用）

- [ ] 1.1 新增 `AppError`（`sealed interface`，package `com.shang.jetpackmoviekmp.common`）：`Network(val exception: NetworkException)`、`Unknown` 兩個子型別
- [ ] 1.2 新增 `AppResult<out T>`（`sealed interface`，同 package）：`Success<T>(val data: T)`、`Failure(val error: AppError) : AppResult<Nothing>` 兩個子型別
- [ ] 1.3 補上 `shared/common` 的單元測試（AAA），驗證 `AppResult.Success`／`Failure` 建構行為與 `AppError.Network` 攜帶的 `NetworkException` 正確保留

## 2. shared/data（本次範圍：Android／共用）

- [ ] 2.1 修改 `MovieRepository.getConfiguration()` 介面簽名，回傳型別由 `Flow<Result<ConfigurationBean>>` 改為 `Flow<AppResult<ConfigurationBean>>`
- [ ] 2.2 修改 `MovieRepositoryImpl.getConfiguration()` 實作：`NetworkResponse.isSuccess` 為 `true` 時回傳 `AppResult.Success(data)`；為 `false` 時將 `NetworkResponse.error`（`NetworkException`）包成 `AppResult.Failure(AppError.Network(error))`
- [ ] 2.3 更新 `MovieRepositoryImplTest` 中 `getConfiguration` 相關測試案例，斷言改為對 `AppResult` 的型別檢查
- [ ] 2.4 執行 `./gradlew :shared:data:koverVerify` 確認 80% 覆蓋率門檻仍達標

## 3. shared/domain（本次範圍：Android／共用）

- [ ] 3.1 修改 `GetConfigurationUseCase` 回傳型別為 `Flow<AppResult<ConfigurationBean>>`，將原本的 `result.fold(...)` 快取寫入／fallback 邏輯改寫為對 `AppResult` 的 `when` 分支等價實作，行為（成功寫快取、失敗有快取則 fallback 為 Success、失敗無快取回傳 Failure）維持不變
- [ ] 3.2 更新 `GetConfigurationUseCase` 對應測試的三個案例（成功寫入快取並回傳成功／失敗但有快取時退回快取／失敗且無快取時回傳原始錯誤），斷言改為對 `AppResult` 的型別檢查

## 4. androidApp（本次範圍：Android）

- [ ] 4.1 修改 `MainViewModel`，將 `getConfigurationUseCase().map { result -> result.fold(...) }` 改為消費 `AppResult`（`is AppResult.Success` / `is AppResult.Failure`）
- [ ] 4.2 視 `MainUiState.Error` 目前欄位型別（`Throwable`）決定轉換方式：若維持 `Throwable`，由 `AppError.Network.exception`（本身即 `NetworkException : Exception`）或 `AppError.Unknown` 對應的預設例外提供；不需新增額外欄位
- [ ] 4.3 執行 `./gradlew :androidApp:assembleDebug` 驗證編譯成功
- [ ] 4.4 手動啟動 App，驗證 Loading／Error／Success 三態顯示行為與改動前一致（含網路離線時觸發 Error 狀態的手動驗證）

## 5. 整體驗證

- [ ] 5.1 執行 `./gradlew ktlintCheck` 確認格式通過
- [ ] 5.2 執行 `./gradlew :shared:data:testAndroidHostTest :shared:domain:testAndroidHostTest :shared:common:testAndroidHostTest` 確認三個模組測試皆通過
- [ ] 5.3 執行 `./gradlew check` 確認整體建置與既有測試無回歸

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
