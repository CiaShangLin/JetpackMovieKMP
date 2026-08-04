## 1. androidApp：語言切換刷新流程

- [x] 1.1 修改 `MainActivity.kt` 的 `LaunchedEffect(userData.languageMode)`，在 `LanguageSettingUtils.updateActivityLocale()` 完成後呼叫 `activity.recreate()`
- [x] 1.2 移除包住 `rememberNavBackStack(HomeKey)` 的 `key(languageMode)` 包裝，改為讓 backstack 依 Navigation3 既有序列化保存機制在 `recreate()` 後自動還原
- [x] 1.3 手動驗證：在非首頁畫面（例如 Setting 頁、Detail 頁）切換語言，確認字串立即以新語言顯示，且停留在原本畫面
- [x] 1.4 以 `MainViewModel` 保留已套用語言，確保同語言重建後不重複呼叫 `recreate()`，並新增單元測試防止 Splash 重建循環

## 2. androidApp：字串資源補齊

- [x] 2.1 新增 `androidApp/src/main/res/values-en-rUS/strings.xml`，翻譯現有 `values/strings.xml` 的 6 個 key（`app_name`、`nav_home`、`nav_favor`、`nav_history`、`nav_search`、`nav_setting`）
- [x] 2.2 手動驗證：切換語言模式為英文，確認底部導覽列文字正確顯示英文

## 3. feature/collect：字串資源補齊

- [x] 3.1 新增 `feature/collect/src/main/res/values-en-rUS/strings.xml`，翻譯現有 `values/strings.xml` 的 key
- [x] 3.2 手動驗證：切換語言模式為英文，確認收藏頁文字正確顯示英文

## 4. feature/history：字串資源補齊

- [x] 4.1 新增 `feature/history/src/main/res/values-en-rUS/strings.xml`，翻譯現有 `values/strings.xml` 的 key
- [x] 4.2 手動驗證：切換語言模式為英文，確認歷史頁文字正確顯示英文

## 5. feature/home：語言改變時重新查詢電影列表

- [x] 5.1 修改 `HomeContentViewModel.kt`，將 `userDataRepository.userData.map { it.languageMode }.distinctUntilChanged()` 併入電影列表資料流，語言改變時以 `flatMapLatest` 重新呼叫 `getMovieGenreUseCase(...)` 並重建 `Pager`，外層維持 `cachedIn(viewModelScope)`
- [x] 5.2 新增／更新單元測試：驗證 `languageMode` 改變時觸發新的 Pager 查詢、`languageMode` 未改變（例如僅 `themeMode` 改變）時不重複查詢；維持 `shared:data`／`feature:home` 既有覆蓋率門檻

## 6. feature/detail：語言改變時重新查詢電影詳情

- [x] 6.1 修改 `MovieDetailViewModel.kt`，將語言改變事件與既有 `retryTrigger` 用 `merge()` 合成同一個 trigger flow
- [x] 6.2 將 `movieDetail` 改為訂閱合成後的 trigger flow（取代原本只綁 `retryTrigger`）
- [x] 6.3 將 `movieRecommendations`、`movieActors` 從建構時一次性 `stateIn` 改為訂閱合成後的 trigger flow 再 `flatMapLatest`／`stateIn`
- [x] 6.4 新增／更新單元測試：驗證語言改變時 `movieDetail`／`movieRecommendations`／`movieActors` 皆重新查詢；驗證既有手動 `retryMovieDetail()` 行為不受影響

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
