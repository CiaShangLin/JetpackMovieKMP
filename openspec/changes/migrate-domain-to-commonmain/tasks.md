## 1. shared/commonMain UseCase 遷移

- [ ] 1.1 建立 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/domain/usecase/`。
- [ ] 1.2 遷移 `GetConfigurationUseCase`：依賴 `MovieRepository`、`UserDataRepository`、`ioDispatcher: CoroutineDispatcher`（無預設值）；API 失敗時退回 `UserDataRepository.userData` 目前快取的 configuration，皆無快取才回傳原始錯誤。
- [ ] 1.3 遷移 `GetHistoryMovieListUseCase`：依賴 `MovieRepository`、`ioDispatcher`；`combine(getAllMovieHistory(), getCollectedMovieIds())` 標記 `isCollect`。
- [ ] 1.4 遷移 `GetHomeMovieListUseCase`：依賴 `MovieRepository`、`ioDispatcher`；`invoke(withGenres, viewModelScope)` 簽名不變，`cachedIn(viewModelScope)` 後與 `getCollectedMovieIds()` 合併標記 `isCollect`。
- [ ] 1.5 遷移 `GetMovieDetailUseCase`：依賴 `MovieRepository`、`ioDispatcher`；成功時呼叫 `insertMovieHistory(...)`。
- [ ] 1.6 遷移 `GetMovieRecommendUseCase`：依賴 `MovieRepository`、`ioDispatcher`；`combine` 推薦清單與收藏 id 標記 `isCollect`。

## 2. Koin DI module

- [ ] 2.1 建立 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/domain/di/DomainModule.kt`。
- [ ] 2.2 實作 `domainModule()`：5 個 UseCase 皆用 `factory { GetXxxUseCase(get(), ..., get(qualifier = named(CommonDispatcher.IO))) }`（見 design.md 決策 2），依賴既有 `dataModule()` 提供的 `MovieRepository`／`UserDataRepository`。

## 3. initKoin 串接

- [ ] 3.1 擴充 `shared/commonMain` 的 `initKoin(...)`（含兩個重載），在 `modules(...)` 加入 `domainModule()`；確認不需要新增任何函式參數。
- [ ] 3.2 確認 `androidApp/JetpackMovieApplication`、`shared/iosMain/InitKoinIos.doInitKoinIos` 不需要修改（`domainModule()` 無平台專屬參數）。
- [ ] 3.3 更新 `shared/commonTest` 既有的 `InitKoinTest.kt`，補上 5 個 UseCase 的 resolve 驗證。

## 4. Test fakes

- [ ] 4.1 在 `shared/src/commonTest/kotlin/com/shang/jetpackmoviekmp/domain/` 新增 `FakeMovieRepository`（實作 `MovieRepository` 全部方法，用 `MutableStateFlow`／可控制回傳值模擬各種情境，未使用到的方法回傳合理預設值）。
- [ ] 4.2 新增 `FakeUserDataRepository`（實作 `UserDataRepository` 全部方法，`userData` 用 `MutableStateFlow<UserData>`，`setConfiguration` 等方法會更新該 `MutableStateFlow`）。

## 5. Tests

- [ ] 5.1 新增 `GetConfigurationUseCase` 測試：API 成功寫入快取並回傳成功、API 失敗但有快取時退回快取、API 失敗且無快取時回傳原始錯誤（見 specs 三個 scenario）。
- [ ] 5.2 新增 `GetHistoryMovieListUseCase` 測試：瀏覽紀錄中已收藏／未收藏電影的 `isCollect` 標記正確。
- [ ] 5.3 新增 `GetHomeMovieListUseCase` 測試：驗證合併收藏狀態邏輯；分頁本身比照 `data` 層既有作法做 collectible smoke test，不斷言 `PagingData` 實際內容（專案未引入 `paging-testing`）。
- [ ] 5.4 新增 `GetMovieDetailUseCase` 測試：成功時 `insertMovieHistory` 被呼叫一次、失敗時不被呼叫。
- [ ] 5.5 新增 `GetMovieRecommendUseCase` 測試：成功時標記 `isCollect`、失敗時回傳原始錯誤。
- [ ] 5.6 新增 `domainModule()` resolve 測試：安裝 `commonModule()`、`networkModule(...)`、`databaseModule(...)`、`datastoreModule(...)`、`dataModule()`、`domainModule()` 後可成功 resolve 全部 5 個 UseCase。
- [ ] 5.7 確認測試涵蓋率達到專案最低 80% 單元測試覆蓋率要求。調整 `shared/build.gradle.kts` 的 Kover filters：新增 `domain.usecase`、`domain.di`。

## 6. Verification

- [ ] 6.1 執行 `.\gradlew.bat :shared:testAndroidHostTest`，確認全數通過。
- [ ] 6.2 執行 `.\gradlew.bat :shared:koverVerify`，確認覆蓋率門檻通過。
- [ ] 6.3 執行 `.\gradlew.bat :androidApp:assembleDebug`，確認編譯成功。
- [ ] 6.4 執行 `ktlintCheck`，確認格式符合專案規範。
- [ ] 6.5 記錄環境限制（iOS 模擬器測試因 Windows 主機無法執行，比照先前四次遷移的既有記錄方式註明）。
