# 開發備忘錄（Backlog）

開發途中發現、留待之後建立新 change 處理的項目，由 /flow-note 維護。

## 首頁 feature 層消費 domain UseCase 的架構設計（CoroutineScope 注入方式）
- 類型: feature
- 記錄日期: 2026-07-19
- 來源: migrate-domain-to-commonmain（實作中發現）
- 前置依賴: 無
- 狀態: 待處理

### 背景

`GetHomeMovieListUseCase.invoke(withGenres, scope: CoroutineScope)` 需要呼叫端提供
`CoroutineScope` 給 `cachedIn`。這個專案的 iOS 端（`iosApp`）是原生 SwiftUI，`shared`
模組沒有掛 Compose Multiplatform plugin，所以 Swift 無法直接建立/傳入 `CoroutineScope`。

### 決策方向

1. 不要把這個 scope 改成 app 層級單例注入（比照 `common/di/CommonModule.kt` 現有的
   app-scoped `single<CoroutineScope>`）——那個 scope 是設計給 `DatastoreLanguageProvider`
   這類永不停止的背景 provider 用，若拿來給 `cachedIn` 用，快取會永遠不會釋放（使用者離開
   首頁、切換類型篩選都不會回收，等同記憶體洩漏）。`cachedIn` 的 scope 本質是「畫面該活
   多久」的生命週期問題，不是無狀態資源，不能比照 `ioDispatcher` 那樣全域注入。
2. 建議在 `commonMain` 新增一個 `HomeScreenModel`（純 Kotlin class，不繼承 androidx
   `ViewModel`），自己建立並持有 `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`，
   暴露 `observeMovies(withGenres)` 呼叫 `GetHomeMovieListUseCase.invoke(withGenres, scope)`，
   並提供 `onDestroy()` 取消該 scope。`GetHomeMovieListUseCase` 簽名維持不變。
   - 刻意不讓 `HomeScreenModel` 直接繼承 JetBrains KMP `ViewModel`（`org.jetbrains.androidx.lifecycle`）：
     那套的自動收尾機制是設計給 Compose Multiplatform 的 `ViewModelStoreOwner` 走的，在原生
     SwiftUI（沒有 Compose Multiplatform 承載）能否正確手動觸發收尾沒有把握，改用手動
     `CoroutineScope` + `onDestroy()` 更保證兩平台行為一致。
3. Android 端：用一個真正的 `androidx.lifecycle.ViewModel` 包一層 `HomeScreenModel`，
   `onCleared()` 呼叫 `homeScreenModel.onDestroy()`。透過 Koin 的 `viewModel { HomeViewModel(get()) }`
   DSL 注入（`koin-compose-viewmodel` 已經在 `gradle/libs.versions.toml` 版本目錄裡，但目前
   還沒接到任何模組，之後要記得加進 `androidApp`）。
4. iOS 端：`HomeScreenModel` 走 Koin 解析，但 Swift 沒辦法呼叫 Kotlin 的 reified generic
   `get<T>()`，需要在 commonMain 寫一個明確的橋接物件（比照 Koin 官方 KMP 建議模式）：
   ```kotlin
   object KoinHelper : KoinComponent {
       fun homeScreenModel(): HomeScreenModel = getKoin().get()
   }
   ```
   Swift 端呼叫 `KoinHelper.shared.homeScreenModel()`，並在 SwiftUI 的 `.onDisappear`／包裝的
   `ObservableObject.deinit` 呼叫 `.onDestroy()`。目前專案完全還沒有任何 Koin↔Swift 橋接程式碼
   （`iosApp` 裡搜尋不到 `KoinHelper`／`koin.get<` 之類的東西），這是全新要補的部分。
5. 這整個 iOS 注入的需求**不需要用 `expect`/`actual` 處理**：`KoinHelper` 的邏輯在 Android／iOS
   上完全相同（不是平台實作差異，是 Swift↔Kotlin 語言互通問題），且即使未來 `domain` 拆成獨立
   Gradle module（`core:domain`），這 5 個 UseCase 都是純 commonMain 邏輯，沒有任何平台專屬
   程式碼需要 `expect`/`actual`，模組化與 `expect`/`actual` 是兩個不相干的問題。

### 適用範圍

下一個「首頁 feature 層」或「domain 模組化」相關 change 開始前，可以先翻這則記錄，不需要
重新推導這些取捨。

## 收斂 shared:data 對底層模組的 api 暴露
- 類型: refactor
- 記錄日期: 2026-07-20
- 分支: feature/modularize-shared-kmp-modules
- 相關 change: modularize-shared-kmp-modules

### 背景

模組化後 `shared:data` 目前對 `shared:network`、`shared:database`、`shared:datastore`
使用 `api` 依賴，這能讓上層模組先穩定編譯，但會把 data 層的底層實作依賴以
transitive API 形式暴露給 `shared:domain` / `shared:app`。

架構上 `shared:domain` 不應直接接觸 network / database / datastore。domain 應只明確依賴
自己的 public API 真正需要的 `shared:model`、`shared:common`，以及目前 UseCase 回傳型別需要的
`androidx-paging-common`。

### 後續調整

1. 檢查 `shared:data` public signatures。
2. 將未出現在 public API 的 `shared:network`、`shared:database`、`shared:datastore`
   依賴由 `api` 改為 `implementation`。
3. 確認 `shared:domain` 顯式宣告 `shared:model`、`shared:common`、`androidx-paging-common`
   等真正需要的依賴，不靠 `shared:data` transitive dependency。
4. 評估下一步是否把 `MovieRepository` / `UserDataRepository` interface 移到 `shared:domain`，
   讓依賴方向演進為 `domain -> common/model`、`data -> domain/network/database/datastore`。

## 評估 flow-discuss 討論產物改為跟隨分支/worktree commit
- 類型: refactor
- 記錄日期: 2026-07-23
- 來源: master
- 前置依賴: 無
- 狀態: 待處理

### 背景

目前 `flow-discuss` 收尾時會直接把討論產物（`openspec/changes/<name>/` 底下的
proposal/design/tasks/`.flow.yaml`）commit + push 到當前所在分支（通常是 master），
之後 `flow-apply` 才從該分支（記錄為 `base_branch`）切出 feature branch 或建立 worktree。

使用者提出疑問：討論文件是否該改成 commit 在 feature branch/worktree 裡，而不是先進 master。

### 目前結論

現況設計是必要的、非疏漏：`flow-apply` 第一步要讀取當前分支上已存在的 `.flow.yaml` 的
`type` 欄位才能決定分支前綴（沒指定 change 名稱時還要用 `openspec list --json` 列出可選項），
這個檔案必須先存在於目前分支才找得到；而 `flow-apply` 的前置檢查又會在工作目錄有未提交變更時
直接停下詢問，因此討論產物必須先在 master commit 乾淨，flow-apply 才能順利往下走。

這個設計的好處是討論產物像 RFC 一樣先落地到 master，任何分支都能看到目前有哪些 change
在規劃中。取捨則是：change 若被放棄，master 會留下一個「文件已存在但功能沒做」的孤兒
commit（無害但會累積）；且文件是直接 push 到 master、沒經過 PR 審查，跟後續實作的 PR
是分開審查的。

若之後要改成「文件也跟著分支/worktree 走」，需要同時修改 `flow-discuss`（討論完不 push，
留在工作目錄）與 `flow-apply`（改成先建分支/worktree，再把未提交的文件一起 commit 上去），
且會跟 flow-apply 現有「有未提交變更就停下詢問」的規則衝突，需要一併調整判斷邏輯。

### 適用範圍

尚未決定是否要改，待評估團隊是否需要「規劃文件也走 PR 審查」或「不想在 master 留下
未實作的孤兒 commit」，之後有需要再回頭處理 `flow-discuss`／`flow-apply` 這兩個 skill。

## 評估 Swift 友善的 Flow Result 匯出模型
- 類型: refactor
- 記錄日期: 2026-07-23
- 來源: add-skie（實作驗證中發現）
- 前置依賴: add-skie
- 狀態: 待處理

### 背景

`add-skie` 導入 SKIE 後，`GetMovieDetailUseCase.invoke(movieId:)` 已可在 Swift 端以
`for await` 迭代，SKIE 產生的 apinotes 顯示其回傳型別為 `SkieKotlinFlow<id>`。
這代表 `Flow<Result<MovieDetailBean>>` 的 `Flow` 互通已成立，但 `kotlin.Result<T>` 的
泛型成功/失敗語意在 Swift 端被 type erase 成 `Any`，無法以 Swift 型別系統直接辨識
`Result.success(MovieDetailBean)` 與 `Result.failure(Throwable)`。

### 後續調整

1. 評估是否新增 Swift 友善的 sealed class / data class，例如 `MovieDetailResult`，避免直接匯出
   `kotlin.Result<T>`。
2. 若維持既有 UseCase 簽名，評估在 `shared/app` 或 `iosMain` 增加專供 Swift 消費的 wrapper，
   將 `Result<T>` 轉成可被 SKIE 匯出為 Swift enum / class hierarchy 的型別。
3. 補 iOS 端整合測試或 Xcode smoke test，實際覆蓋成功 movieId 與失敗 movieId，確認不只
   `for await` 可編譯，也能在 runtime 明確區分成功與失敗。

## 評估 commonMain 通用 AppResult / AppError 取代跨 iOS 匯出的 Kotlin Result
- 類型: refactor
- 記錄日期: 2026-07-23
- 來源: ios-splash-rewrite（實作中發現）
- 前置依賴: 無
- 狀態: 待處理

### 背景

`ios-splash-rewrite` 除錯確認：SKIE 可以讓 Swift 端正常以 `for await` 消費 `Flow`，
但 `Flow<Result<ConfigurationBean>>` 迭代出的元素在 Swift runtime 會是
`Optional(Success(ConfigurationBean(...)))` 這類 Kotlin `Result` boxed value，而不是
`ConfigurationBean` 本體，因此 Swift 端用 `as? ConfigurationBean` 無法判斷成功。

本次 Splash 先用 `shared/app` iosMain 的 `IosConfigurationLoader` 作為局部 wrapper，
在 Kotlin 端把 `Result<ConfigurationBean>` 拆成 `IosConfigurationLoadState.Success/Failure`
後再匯出給 Swift。這是最小修正，但不應每個 UseCase 都各自手寫一次 bridge。

### 後續調整

1. 評估在 `commonMain` 定義通用 sealed result，例如：
   ```kotlin
   sealed interface AppResult<out T> {
       data class Success<T>(val data: T) : AppResult<T>
       data class Failure(val error: AppError) : AppResult<Nothing>
   }
   ```
2. 不要把所有錯誤都硬轉成 `NetworkException`。`NetworkException` 應維持只代表 network
   request 流程中的錯誤；若要做跨 UseCase 的通用錯誤模型，應另定義更高階的 `AppError` /
   `DomainError`，例如 `Network(NetworkException)`、`Database`、`LocalStorage`、`Unknown`。
3. 長期可評估讓 domain UseCase 從 `Flow<Result<T>>` 漸進改成 `Flow<AppResult<T>>`，
   讓 Android / common / iOS 都消費同一組明確狀態；短期仍可保留局部 iosMain wrapper，
   避免在目前 change 中擴大重構範圍。

## iOS 端導入統一 App Log 機制
- 類型: feature
- 記錄日期: 2026-07-23
- 來源: ios-splash-rewrite（實作中發現）
- 前置依賴: 無
- 狀態: 待處理

iOS 端目前除錯主要靠 `print`、Xcode console 與 Ktor network logging。後續需要規劃一套
App log 機制，讓 Swift / shared bridge / network 相關訊息能以一致格式輸出，並能依 Debug /
Release 或環境變數動態控制開關，避免正式版輸出過多敏感或雜訊 log。

## iOS 端多國語言設置
- 類型: feature
- 記錄日期: 2026-07-23
- 來源: ios-splash-rewrite（實作中發現）
- 前置依賴: 無
- 狀態: 待處理

iOS SwiftUI 畫面目前仍直接寫固定文案，例如 Splash 的「載入中」、「準備完成」、「重試」與
錯誤訊息。後續需要補 iOS 端 localization 設置，評估使用 `Localizable.strings` /
String Catalog，並與 shared 層既有語言設定、TMDB API `language` 參數的來源協調，避免
UI 語言與 API 語言狀態不一致。

## iOS 端 lint 與自動排版流程
- 類型: refactor
- 記錄日期: 2026-07-23
- 來源: ios-splash-rewrite（實作中發現）
- 前置依賴: 無
- 狀態: 已處理（add-ios-lint-formatting）

目前專案已有 Kotlin / Android 端 `ktlintCheck` 與 `ktlintFormat`，但 iOS Swift 程式碼尚未
建立對應的 lint 與自動排版流程。後續需要評估導入 SwiftFormat / SwiftLint，決定規則檔、
Xcode build phase 或 CLI 驗證指令，並把常用指令補到專案文件，讓 Swift 檔案格式與風格能
穩定檢查。
