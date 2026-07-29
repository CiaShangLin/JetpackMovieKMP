## 1. feature:history 模組建立

- [ ] 1.1 在 `settings.gradle.kts` 新增 `include(":feature:history")`
- [ ] 1.2 建立 `feature/history/build.gradle.kts`，比照 `feature/collect/build.gradle.kts`（namespace `com.shang.jetpackmoviekmp.feature.history`），並額外加入 `implementation(projects.shared.domain)`
- [ ] 1.3 確認 `./gradlew :feature:history:build` 可成功執行（模組尚無原始碼時允許空模組通過）

## 2. UI State 與 ViewModel

- [ ] 2.1 建立 `HistoryUiState`（`sealed class`：`Empty`／`Success(historyList: List<MovieCardResult>)`），比照 `CollectUiState`
- [ ] 2.2 建立 `HistoryViewModel`，建構子注入 `GetHistoryMovieListUseCase` 與 `MovieRepository`
- [ ] 2.3 實作歷史清單訂閱：`GetHistoryMovieListUseCase()` → `map` 轉為 `HistoryUiState` → `stateIn(WhileSubscribed(5_000))`
- [ ] 2.4 實作切換收藏：依 `MovieCardData.movieCardIsCollect` 分流呼叫 `MovieRepository.insertMovieCollect()` 或 `deleteMovieCollect()`
- [ ] 2.5 實作清空歷史：呼叫 `MovieRepository.deleteAllMovieHistory()`

## 3. Screen 與 UI 元件

- [ ] 3.1 建立 `HistoryScreen`（`koinViewModel()` 取得 `HistoryViewModel`），依 `HistoryUiState` 切換空狀態與清單畫面
- [ ] 3.2 建立歷史清單畫面：沿用 `JMLazyVerticalGrid` + `MovieCard`，收藏按鈕回呼接至 ViewModel 的切換收藏方法
- [ ] 3.3 建立標題列（含清空按鈕），Divider 改用 Material3 `HorizontalDivider`（取代舊版 Material2 `TabRowDefaults.Divider`）
- [ ] 3.4 建立空狀態畫面（沿用歷史頁專屬文案）

## 4. 資源檔

- [ ] 4.1 建立 `feature/history/src/main/res/values/strings.xml`：`history_title`、`history_clear`、`history_empty`
- [ ] 4.2 建立空狀態 vector drawable `icon_empty.xml`（比照 `feature/collect` 的做法，取代舊版 `icon_empty.webp`）

## 5. Koin module 與導覽進入點

- [ ] 5.1 建立 `HistoryModule.kt`：`historyModule()`，以 Koin `viewModel { }` 建立 `HistoryViewModel`
- [ ] 5.2 建立 `HistoryNavigation.kt`：`HistoryKey`（`@Serializable data object` 實作 `NavKey`）與 `historyEntry()`

## 6. androidApp 組裝層

- [ ] 6.1 `androidApp/src/main/res/values/strings.xml` 新增 `nav_history` 字串
- [ ] 6.2 `navigation/MainNavItem.kt`：取消註解 `HISTORY` entry，`key` 改指向 `feature:history` 的 `HistoryKey`
- [ ] 6.3 `ui/MainActivity.kt`：`entryProvider` 的 `when` 分支新增 `HistoryKey -> historyEntry().second`
- [ ] 6.4 `JetpackMovieApplication.kt`：`loadKoinModules` 加入 `historyModule()`

## 7. 單元測試

- [ ] 7.1 在 `feature/history/src/test` 建立本地 `FakeMovieRepository`（比照 `feature/collect` 的做法）
- [ ] 7.2 撰寫 `HistoryViewModelTest`（AAA 模式）：驗證歷史清單轉為 `Success`／`Empty` state，且每筆資料的收藏狀態正確反映
- [ ] 7.3 撰寫測試：對已收藏電影觸發收藏按鈕會呼叫 `deleteMovieCollect()` 一次
- [ ] 7.4 撰寫測試：對未收藏電影觸發收藏按鈕會呼叫 `insertMovieCollect()` 一次
- [ ] 7.5 撰寫測試：觸發清空操作會呼叫 `deleteAllMovieHistory()` 一次
- [ ] 7.6 執行 `./gradlew :feature:history:test`（或對應 test task）確認全數通過

## 8. 整合驗證

- [ ] 8.1 執行 `./gradlew ktlintCheck`
- [ ] 8.2 執行 `./gradlew :androidApp:assembleDebug` 確認整體可建置
- [ ] 8.3 手動驗證：App 底部導覽列可切換至歷史頁，顯示歷史清單／空狀態、收藏切換與清空功能皆正常運作
