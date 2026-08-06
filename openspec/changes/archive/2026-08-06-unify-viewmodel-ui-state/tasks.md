## 1. shared/common：新增 UiState<T> 與 toUiState()

- [x] 1.1 新增 `shared/common/src/commonMain/kotlin/com/shang/jetpackmoviekmp/common/UiState.kt`：定義 `sealed interface UiState<out T>`（`Loading`、`Success<T>(data)`、`Error(throwable: Throwable)`）
- [x] 1.2 在同檔案或 `AppResult.kt` 新增 `AppResult<T>.toUiState(): UiState<T>` extension function
- [x] 1.3 新增 `shared/common/src/commonTest/kotlin/com/shang/jetpackmoviekmp/common/UiStateTest.kt`，以 AAA 模式驗證 `toUiState()` 對 `AppResult.Success`／`AppResult.Failure` 的轉換行為
- [x] 1.4 執行 `./gradlew :shared:common:testAndroidHostTest` 與 `./gradlew :shared:common:koverVerify`，確認新增程式碼不拉低 80% 覆蓋率門檻

## 2. androidApp：MainUiState 移除，改用 UiState<T>

- [x] 2.1 移除 `androidApp/src/main/kotlin/com/shang/jetpackmoviekmp/ui/MainUiState.kt`（原手寫 sealed interface）
- [x] 2.2 調整 `MainViewModel` 的 `configuration: StateFlow<UiState<ConfigurationBean>>` mapping，改用 `AppResult<ConfigurationBean>.toUiState()` 取代手寫 `when` 分支
- [x] 2.3 調整 `MainActivity` 消費端，`MainUiState.Loading`／`Success`／`Error` 改為 `UiState.Loading`／`Success`／`Error`
- [x] 2.4 執行既有 `androidApp/src/test/kotlin/com/shang/jetpackmoviekmp/ui/MainViewModelTest.kt`（`./gradlew :androidApp:testDebugUnitTest`），確認全數通過

## 3. feature/home：HomeUiState 移除，改用 UiState<T>，並修正 Error.throwable 非 null

- [x] 3.1 將 `HomeUiState.kt` 移除，`HomeViewModel`／`HomeScreen.kt` 直接改用 `UiState<MovieGenreBean>`（技術限制：typealias 無法存取巼狀 classifier，見下方偏差記錄）
- [x] 3.2 調整 `HomeViewModel` 的 mapping 邏輯改用 `toUiState()`；`Error.throwable` 已隨 `UiState.Error(throwable: Throwable)` 統一為非 null
- [x] 3.3 確認 `HomeContentViewModel`、`HomeScreen.kt` 中依賴 AndroidX Paging `LoadState` 的部分不受影響，不需修改
- [x] 3.4 執行既有 `feature/home/src/test/java/com/shang/jetpackmoviekmp/feature/home/ui/HomeViewModelTest.kt`（`./gradlew :feature:home:testDebugUnitTest`），確認全數通過並視需要更新 `HomeViewModelTestFakes.kt`

## 4. feature/detail：MovieDetailUiState 移除，改用 UiState<T>，並移除 DetailSectionState<T>

- [x] 4.1 移除 `MovieDetailUiState.kt` 與 `DetailSectionState<T>` 定義，改用 `UiState<T>`
- [x] 4.2 調整 `MovieDetailViewModel` 的 `movieDetail`、`movieRecommendations: StateFlow<UiState<List<MovieCardResult>>>`、`movieActors: StateFlow<UiState<List<MovieCastAndCrewBean.Cast>>>` 改用 `toUiState()` mapping（`movieActors` 因需要 `.cast` 資料轉換，保留手寫 `when` mapping）
- [x] 4.3 調整 `MovieDetailScreen.kt` 中對 `DetailSectionState.Loading/Success/Error` 的 `when` 分支，改為對應的 `UiState.Loading/Success/Error`（分支邏輯與「Error 時隱藏區塊、不顯示 ErrorScreen」的既有行為保持不變）
- [x] 4.4 執行既有 `feature/detail/src/test/java/com/shang/jetpackmoviekmp/feature/detail/ui/MovieDetailViewModelTest.kt`（`./gradlew :feature:detail:testDebugUnitTest`），涵蓋主 detail 成功／失敗／重試、演員與推薦區塊獨立狀態，確認全數通過

## 5. 最終驗證

- [x] 5.1 執行 `./gradlew ktlintCheck`
- [x] 5.2 執行 `./gradlew :androidApp:assembleDebug`，確認整合建置成功
- [x] 5.3 執行 `./gradlew :shared:common:koverVerify`
- [x] 5.4 執行 `openspec validate unify-viewmodel-ui-state --type change --strict --no-interactive`
- [x] 5.5 不需要 iOS simulator test：本次未變更 `iosMain`、expect/actual 或 Swift interop 邊界
