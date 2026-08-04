## 1. androidApp：語言切換刷新流程

> 本章原方案為 `activity.recreate()`，因會導致 Splash 卡住無法消失，已改為下列「同步套用 Locale ＋ 縮小範圍 `key(languageMode)`」方案，不再呼叫 `recreate()`。

- [x] 1.1（已取代）~~修改 `MainActivity.kt` 的 `LaunchedEffect(userData.languageMode)`，在 `LanguageSettingUtils.updateActivityLocale()` 完成後呼叫 `activity.recreate()`~~ → 改為 `remember(userData.languageMode) { LanguageSettingUtils.updateActivityLocale(...) }` 同步呼叫，確保套用新語言的時機早於下方畫面重組
- [x] 1.2 用 `key(userData.languageMode)` 只包住 `MainScreen`（畫面內容），`rememberNavBackStack(HomeKey)` 留在 `key()` 外面只呼叫一次，語言切換時只重組畫面內容、不重置 backstack
- [x] 1.3 移除 `MainViewModel` 的 `shouldRecreateForLanguage`／`lastAppliedLanguageMode`（原本用來防止 `recreate()` 循環），連同對應單元測試一併移除——不再需要 `recreate()`，此機制已無用武之地
- [x] 1.4 手動驗證：在非首頁畫面（例如 Setting 頁、Detail 頁）切換語言，確認字串立即以新語言顯示、停留在原本畫面，且 Splash 不會卡住

## 2. androidApp：字串資源補齊

- [x] 2.1 新增 `androidApp/src/main/res/values-en-rUS/strings.xml`，翻譯現有 `values/strings.xml` 的 6 個 key（`app_name`、`nav_home`、`nav_favor`、`nav_history`、`nav_search`、`nav_setting`）
- [x] 2.2 手動驗證：切換語言模式為英文，確認底部導覽列文字正確顯示英文

## 3. feature/collect：字串資源補齊

- [x] 3.1 新增 `feature/collect/src/main/res/values-en-rUS/strings.xml`，翻譯現有 `values/strings.xml` 的 key
- [x] 3.2 手動驗證：切換語言模式為英文，確認收藏頁文字正確顯示英文

## 4. feature/history：字串資源補齊

- [x] 4.1 新增 `feature/history/src/main/res/values-en-rUS/strings.xml`，翻譯現有 `values/strings.xml` 的 key
- [x] 4.2 手動驗證：切換語言模式為英文，確認歷史頁文字正確顯示英文

## 5. feature/home：語言改變時重新查詢電影列表（已還原，移出本次範圍）

> 原本已實作「語言改變時 `flatMapLatest` 重建 `Pager` 重新查詢」，但使用者決定收斂本次 change 範圍到 Splash 卡住與字串刷新問題，資料過期問題延後處理。已還原 `HomeContentViewModel.kt`／`HomeModule.kt` 回到建構時一次性查詢，移除 `userDataRepository` 依賴與相關單元測試／fake 擴充。若後續要重新處理，需另開新 change。

- [x] 5.1（已還原）`HomeContentViewModel.kt` 回到建構時呼叫一次 `getMovieGenreUseCase(...)`，不再觀察 `languageMode`；`HomeModule.kt` 移除 `userDataRepository` 注入
- [x] 5.2（已還原）移除語言相關單元測試與 `FakeMovieRepository` 的 `movieListRequests` 追蹤

## 6. feature/detail：語言改變時重新查詢電影詳情（已還原，移出本次範圍）

> 原本已實作「語言改變事件與 `retryTrigger` 用 `merge()` 合成 trigger flow」，理由同第 5 章，已還原並移出本次範圍。

- [x] 6.1（已還原）`MovieDetailViewModel.kt` 回到只綁 `retryTrigger`（不再合成語言改變事件），`DetailModule.kt` 移除 `userDataRepository` 注入
- [x] 6.2（已還原）移除語言相關單元測試與對應 Fake 擴充

## 7. shared/network：移除預設 LanguageProvider 選項

- [x] 7.1 刪除 `shared/network/src/commonMain/kotlin/com/shang/jetpackmoviekmp/network/provider/DefaultLanguageProvider.kt`
- [x] 7.2 刪除 `shared/network/src/commonTest/kotlin/com/shang/jetpackmoviekmp/network/provider/DefaultLanguageProviderTest.kt`
- [x] 7.3 移除 `NetworkModule.kt` 的 `provideDefaultLanguageProvider` 參數與其綁定邏輯，`networkModule()` 簽名改為僅接受 `isDebug`
- [x] 7.4 更新 `shared/app/src/commonMain/kotlin/com/shang/jetpackmoviekmp/InitKoin.kt` 呼叫 `networkModule()` 的引數
- [x] 7.5 更新 `shared/data/src/commonTest/.../di/DataModuleTest.kt`、`shared/domain/src/commonTest/.../di/DomainModuleTest.kt` 呼叫 `networkModule()` 的引數

## 8. 全模組驗證

- [x] 8.1 執行 `./gradlew ktlintCheck`
- [x] 8.2 執行 `./gradlew :shared:data:testAndroidHostTest :shared:domain:testAndroidHostTest`（若 domain 模組有對應測試任務名稱需對齊實際 task）
- [x] 8.3 執行 `./gradlew :shared:data:koverVerify :shared:network:koverVerify`
- [x] 8.4 執行 `./gradlew :androidApp:assembleDebug` 確認整體可編譯
- [x] 8.5（新增）還原後重新執行 `./gradlew ktlintCheck :androidApp:testDebugUnitTest :feature:home:testDebugUnitTest :feature:detail:testDebugUnitTest :androidApp:assembleDebug` 確認皆通過
