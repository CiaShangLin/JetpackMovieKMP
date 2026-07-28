## 1. feature/collect

- [ ] 1.1 在 `settings.gradle.kts` 註冊 `:feature:collect`，建立 Android library Gradle 組態與 `com.shang.jetpackmoviekmp.feature.collect` 原始碼結構，依賴既有 core/shared 模組及 version catalog alias。
- [ ] 1.2 建立 `CollectUiState` 與 `CollectViewModel`；以 `MovieRepository.getAllMovieCollect()` 提供可觀察的收藏清單 state，並以既有資料模型處理取消收藏操作。
- [ ] 1.3 建立 `CollectScreen`、成功清單與空狀態 Composable；重用 `MovieCard`／`JMLazyVerticalGrid`，加入頁面專用字串與空狀態圖片資源。
- [ ] 1.4 建立 `CollectKey`、collect Navigation3 entry factory 與 `collectModule()`，以 Koin 提供 `CollectViewModel`，不得使用 Hilt 或 classic Navigation Compose。
- [ ] 1.5 新增 `CollectViewModel` JVM 單元測試，依 AAA 驗證收藏清單 state、空清單與取消收藏時的 repository 呼叫。

## 2. androidApp

- [ ] 2.1 在 `androidApp/build.gradle.kts` 加入 `feature:collect` 依賴，並在 App 啟動 Koin 流程載入 `collectModule()`。
- [ ] 2.2 恢復 `MainNavItem.COLLECT`，以 `CollectKey` 與既有收藏圖示／字串建立底部導覽項目。
- [ ] 2.3 更新 `MainActivity` 的 Navigation3 `NavDisplay` entryProvider，將 `CollectKey` 分派至 `collectEntry()`，保留尚未遷移目的地的 placeholder 回退行為。

## 3. 驗證

- [ ] 3.1 執行 `./gradlew :feature:collect:test`，確認 collect ViewModel JVM 測試通過。
- [ ] 3.2 執行 `./gradlew :feature:collect:build` 與 `./gradlew :androidApp:assembleDebug`，確認 feature 與 Android App 編譯及接線成功。
- [ ] 3.3 執行 `./gradlew ktlintCheck`，確認新增 Kotlin 程式碼符合專案格式規範。
