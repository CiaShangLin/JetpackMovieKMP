## 1. shared/commonMain UseCase 遷移

- [x] 1.1 建立 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/domain/usecase/`。
- [x] 1.2 遷移 `GetConfigurationUseCase`：依賴 `MovieRepository`、`UserDataRepository`、`ioDispatcher: CoroutineDispatcher`（無預設值）；API 失敗時退回 `UserDataRepository.userData` 目前快取的 configuration，皆無快取才回傳原始錯誤。
- [x] 1.3 遷移 `GetHistoryMovieListUseCase`：依賴 `MovieRepository`、`ioDispatcher`；`combine(getAllMovieHistory(), getCollectedMovieIds())` 標記 `isCollect`。
- [x] 1.4 遷移 `GetHomeMovieListUseCase`：依賴 `MovieRepository`、`ioDispatcher`；`invoke(withGenres, scope)` 簽名（`scope` 參數 code review 後由 `viewModelScope` 改名，避免 commonMain 綁死 Android 概念，見下方「Code review 修正」），`cachedIn(scope)` 後與 `getCollectedMovieIds()` 合併標記 `isCollect`。
- [x] 1.5 遷移 `GetMovieDetailUseCase`：依賴 `MovieRepository`、`ioDispatcher`；成功時呼叫 `insertMovieHistory(...)`。
- [x] 1.6 遷移 `GetMovieRecommendUseCase`：依賴 `MovieRepository`、`ioDispatcher`；`combine` 推薦清單與收藏 id 標記 `isCollect`。

## 2. Koin DI module

- [x] 2.1 建立 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/domain/di/DomainModule.kt`。
- [x] 2.2 實作 `domainModule()`：5 個 UseCase 皆用 `factory { GetXxxUseCase(get(), ..., get(qualifier = named(CommonDispatcher.IO))) }`（見 design.md 決策 2），依賴既有 `dataModule()` 提供的 `MovieRepository`／`UserDataRepository`。

## 3. initKoin 串接

- [x] 3.1 擴充 `shared/commonMain` 的 `initKoin(...)`（含兩個重載），在 `modules(...)` 加入 `domainModule()`；確認不需要新增任何函式參數。
- [x] 3.2 確認 `androidApp/JetpackMovieApplication`、`shared/iosMain/InitKoinIos.doInitKoinIos` 不需要修改（`domainModule()` 無平台專屬參數）。
- [x] 3.3 更新 `shared/commonTest` 既有的 `InitKoinTest.kt`，補上 5 個 UseCase 的 resolve 驗證。

## 4. Test fakes

- [x] 4.1 在 `shared/src/commonTest/kotlin/com/shang/jetpackmoviekmp/domain/` 新增 `FakeMovieRepository`（實作 `MovieRepository` 全部方法，用 `MutableStateFlow`／可控制回傳值模擬各種情境，未使用到的方法回傳合理預設值）。放在 `DomainTestFakes.kt`，比照既有 `data.repository.RepositoryTestFakes.kt` 把多個 fake 合併於同一檔案的慣例。
- [x] 4.2 新增 `FakeUserDataRepository`（實作 `UserDataRepository` 全部方法，`userData` 用 `MutableStateFlow<UserData>`，`setConfiguration` 等方法會更新該 `MutableStateFlow`）。同樣位於 `DomainTestFakes.kt`。

## 5. Tests

- [x] 5.1 新增 `GetConfigurationUseCase` 測試：API 成功寫入快取並回傳成功、API 失敗但有快取時退回快取、API 失敗且無快取時回傳原始錯誤（見 specs 三個 scenario）。
- [x] 5.2 新增 `GetHistoryMovieListUseCase` 測試：瀏覽紀錄中已收藏／未收藏電影的 `isCollect` 標記正確。
- [x] 5.3 新增 `GetHomeMovieListUseCase` 測試：驗證合併收藏狀態邏輯；分頁本身比照 `data` 層既有作法做 collectible smoke test，不斷言 `PagingData` 實際內容（專案未引入 `paging-testing`）。
- [x] 5.4 新增 `GetMovieDetailUseCase` 測試：成功時 `insertMovieHistory` 被呼叫一次、失敗時不被呼叫。
- [x] 5.5 新增 `GetMovieRecommendUseCase` 測試：成功時標記 `isCollect`、失敗時回傳原始錯誤。
- [x] 5.6 新增 `domainModule()` resolve 測試：安裝 `commonModule()`、`networkModule(...)`、`databaseModule(...)`、`datastoreModule(...)`、`dataModule()`、`domainModule()` 後可成功 resolve 全部 5 個 UseCase。
- [x] 5.7 確認測試涵蓋率達到專案最低 80% 單元測試覆蓋率要求。調整 `shared/build.gradle.kts` 的 Kover filters：新增 `domain.usecase`、`domain.di`。line coverage：`domain.usecase` 70/71（98.6%）、`domain.di` 22/22（100%）。`:shared:koverVerify` 通過。

## 6. Verification

- [x] 6.1 執行 `.\gradlew.bat :shared:testAndroidHostTest`，確認全數通過。全數通過（含既有測試共 104 個測試 class，0 failures／0 errors，修正 combine 相關的 scheduler 問題後）。
- [x] 6.2 執行 `.\gradlew.bat :shared:koverVerify`，確認覆蓋率門檻通過。通過（見 5.7）。
- [x] 6.3 執行 `.\gradlew.bat :androidApp:assembleDebug`，確認編譯成功。BUILD SUCCESSFUL。
- [x] 6.4 執行 `ktlintCheck`，確認格式符合專案規範。`:shared:testAndroidHostTest`、`:shared:koverVerify`、`:androidApp:assembleDebug` 三次執行皆內含 `ktlintFormat`／`ktlintCheck` 任務且全數通過。
- [x] 6.5 記錄環境限制（iOS 模擬器測試因 Windows 主機無法執行，比照先前四次遷移的既有記錄方式註明）。見下方「環境限制紀錄」。

### 環境限制紀錄

- **iOS 模擬器測試**：Windows 主機無法執行 `iosSimulatorArm64Test`／`iosArm64Test`，Gradle 自動略過（沿用先前四次遷移已記錄的既有限制）。`domainModule()`、5 個 UseCase 皆為純 Kotlin／Koin resolve 邏輯，不含平台專屬程式碼，本機僅能保證原始碼正確，未經 iOS 模擬器實機驗證。
- **`combine()` 與 `UnconfinedTestDispatcher` 的 scheduler 衝突**：`GetHistoryMovieListUseCase`／`GetMovieRecommendUseCase`／`GetHomeMovieListUseCase` 內部用到 `combine()`（依賴 `kotlinx.coroutines.channels.produce` 實作），若測試中的 `ioDispatcher` 用「與 `runTest` 不同的 `TestCoroutineScheduler`」建立的 `UnconfinedTestDispatcher()`，會在執行期拋出 `IllegalStateException: Detected use of different schedulers`。修正為改用 `UnconfinedTestDispatcher(testScheduler)`（`TestScope.testScheduler`），與 `runTest` 共用同一個 scheduler；`GetHomeMovieListUseCase` 另外把 `cachedIn` 用的 scope 從 `runTest` 的 `this` 改為 `backgroundScope`，避免其常駐 job 造成 `UncompletedCoroutinesError`。此問題只在本次新增的 `combine`／`cachedIn` 測試中出現，不影響既有 `data` 層測試（該層未使用 `combine`）。

## 7. Code review 修正（驗收階段）

驗收階段委派 `code-review:review-local-changes` 執行 6 個並行 review agent（bug-hunter、code-reviewer、security-auditor、test-coverage-reviewer、contracts-reviewer、historical-context-reviewer）對本次變更做獨立審查，發現並處理以下問題：

- [x] 7.1 `GetMovieDetailUseCase` 的 `runCatching { movieRepository.insertMovieHistory(...) }` 會連帶吞掉 `CancellationException`，破壞 coroutine cooperative cancellation——與 `migrate-data-to-commonmain` tasks.md §9.1 修過的 `PagingSource` 同一類問題（此問題繼承自參考專案原始碼，非本次遷移引入）。修正為先 `catch (e: CancellationException) { throw e }` 再 `catch (e: Exception)`，並補上 `invoke_rethrows_cancellationException_from_movie_history_write_instead_of_swallowing_it` 回歸測試；同時補上 `invoke_still_emits_detail_when_movie_history_write_fails` 驗證一般例外仍會被吞掉、不影響已成功取得的電影詳情。`FakeMovieRepository` 新增 `insertMovieHistoryThrows` 屬性支援模擬寫入失敗。
- [x] 7.2 `GetHomeMovieListUseCase.invoke` 的 `viewModelScope: CoroutineScope` 參數改名為 `scope`：`shared/commonMain` 是簡報框架無關的 domain 程式碼，參數名稱不應假定呼叫端一定是 Android `ViewModel`（獨立由 contracts-reviewer 與 code-reviewer 兩個 agent 各自提出同一項發現）。同步更新對應測試與 KDoc；目前尚無任何消費端引用此 UseCase，改名零風險。
- [x] 7.3 `GetHomeMovieListUseCase.kt:38` 修正與實際程式碼矛盾的過時註解（原註解寫「不需要 cachedIn，讓調用方決定」，但下一行實際呼叫了 `.cachedIn(scope)`）。
- [x] 7.4 `GetMovieRecommendUseCase` 移除兩個多餘的 `.flowOn(ioDispatcher)`（`collectIdsFlow`／`movieRecommendationsFlow` 各自呼叫一次，實際上只需要對合併後的終端 Flow 呼叫一次），與 sibling `GetHistoryMovieListUseCase` 的單一終端 `flowOn` 風格一致。
- [x] 7.5 `DomainModuleTest` 補上 `domainModule_without_dataModule_fails_to_resolve_getConfigurationUseCase` 負向測試，鎖住 `domainModule()` 依賴 `dataModule()` 提供的 `MovieRepository`／`UserDataRepository` 這個 DI 相依關係（比照 `migrate-data-to-commonmain` tasks.md §9.4 建立、`DataModuleTest`／`DatastoreModuleTest` 已沿用的既有測試慣例）。

### 已知限制（review 發現但本次不修，經用戶確認登記）

以下兩項問題經比對 `JetpackMovieCompose` 原始碼確認為**忠實移植自參考專案的既有行為**，不是這次遷移引入的新 bug；修復需要偏離「依樣建立」的本次範圍（前者需跨層修改 `UserData`／`ConfigurationBean` 模型，後者是對快取語意的實質行為變更），經與用戶確認後僅登記為已知限制，留待未來 change 評估：

- **`GetConfigurationUseCase` 的「無快取」分支在正式環境永遠不會被觸發**（bug-hunter 發現，信心高）：`UserDataRepository.userData` 底層由 DataStore 支撐，`UserData.configuration` 為非 nullable 欄位且有預設值 `ConfigurationBean()`，因此 `userDataRepository.userData.firstOrNull()?.configuration` 只要 Flow 有任何一次 emission（正式環境必然如此）就恆為非 null。結果是 API 失敗且本地從未快取過時，仍會被判定為「有快取」並回傳 `Result.success(ConfigurationBean())`（空白 baseUrl 等欄位），而非預期的 `Result.failure`——呼叫端無法得知這是一次偽裝成功的失敗。已確認 `JetpackMovieCompose/core/domain/usecase/GetConfigurationUseCase.kt` 與 `core/model/UserData.kt` 有完全相同的結構，故為參考專案既有行為。
- **`GetHomeMovieListUseCase` 的 `cachedIn` 套用順序不符 Android Paging 官方指引**（bug-hunter 發現）：官方建議 `cachedIn` 必須是套用在 `Flow<PagingData>` 上的最後一個操作，但目前寫法是 `getMovieListPager(...).flowOn(...).cachedIn(scope)` 在前、`combine` + `isCollect` mapping 在後，導致 `isCollect` 標記邏輯脫離 `cachedIn` 的快取範圍，多個 collector 重新收集時會重複查詢收藏 id 並重新套用 mapping。已確認參考專案 `JetpackMovieCompose/core/domain/usecase/GetHomeMovieListUseCase.kt` 為相同結構。
