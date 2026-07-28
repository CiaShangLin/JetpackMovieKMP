## ADDED Requirements

### Requirement: `feature:collect` MUST be an Android-only independently compilable feature module

專案 MUST 新增 `feature:collect`，並在 `settings.gradle.kts` 註冊此模組。其 Android namespace 與 Kotlin package MUST 使用 `com.shang.jetpackmoviekmp.feature.collect`；Gradle 依賴 MUST 僅使用既有 version catalog alias 與專案模組，且不得導入 Hilt、Dagger 或 classic Navigation Compose。

#### Scenario: collect module 可獨立建置

- **WHEN** 執行 `./gradlew :feature:collect:build`
- **THEN** 建置 MUST 成功完成，且不得因缺少 Compose、Koin、Navigation3、shared model/data 或 core UI 依賴而失敗

#### Scenario: collect 模組不殘留來源專案架構

- **WHEN** 檢查 `feature/collect` 的 Gradle 檔與 Kotlin 原始碼
- **THEN** MUST NOT 出現 `com.shang.collect`、`com.shang.data`、`com.shang.model`、`com.shang.ui`、Hilt 或 classic Navigation Compose 的依賴、import 或注解

### Requirement: 收藏頁 MUST 反映本機收藏資料並提供空狀態

`CollectViewModel` MUST 透過既有 `MovieRepository.getAllMovieCollect()` 訂閱收藏電影，並以可觀察的 UI state 提供給 `CollectScreen`。清單有資料時，畫面 MUST 以既有 `MovieCard` 與 grid 顯示每一筆收藏；清單為空時 MUST 顯示收藏頁專用的空狀態圖示與文案。

#### Scenario: 有收藏資料時顯示收藏電影

- **WHEN** `getAllMovieCollect()` 發出至少一筆 `MovieCardResult`
- **THEN** 收藏頁 MUST 顯示收藏標題與每筆對應的 `MovieCard`

#### Scenario: 尚無收藏資料時顯示空狀態

- **WHEN** `getAllMovieCollect()` 發出空清單
- **THEN** 收藏頁 MUST 顯示空狀態圖示與「目前沒有收藏」的在地化文案，且不得顯示空白 grid

### Requirement: 收藏頁 MUST allow user to remove a collected movie

收藏清單中的 `MovieCard` MUST 將收藏按鈕回呼交給 `CollectViewModel`。對已收藏電影觸發回呼時，ViewModel MUST 將 `MovieCardData` 轉為既有 `MovieCardResult` 並呼叫 `MovieRepository.deleteMovieCollect()`；Repository 發出更新後，畫面 MUST 反映移除結果。

#### Scenario: 移除一筆收藏

- **WHEN** 使用者在收藏頁點擊一筆已收藏電影的收藏按鈕
- **THEN** ViewModel MUST 呼叫 `deleteMovieCollect()` 一次，且傳入與被點擊電影相同 id 的資料

#### Scenario: 移除最後一筆收藏後顯示空狀態

- **WHEN** 使用者移除最後一筆收藏，且 Repository 後續發出空清單
- **THEN** 收藏頁 MUST 由電影 grid 切換為空狀態

### Requirement: collect ViewModel MUST be supplied by Koin and unit tested

`feature:collect` MUST 提供 `collectModule()`，以 Koin `viewModel { }` 建立 `CollectViewModel` 並注入 `MovieRepository`。收藏資料轉換與移除行為 MUST 以 AAA 結構的單元測試覆蓋，不得要求 Android 裝置或 Hilt 測試環境。

#### Scenario: Koin 可解析 collect ViewModel

- **WHEN** App 已載入 shared data module 與 `collectModule()`
- **THEN** Koin MUST 能建立 `CollectViewModel` 並提供其 `MovieRepository` 依賴

#### Scenario: ViewModel 行為測試通過

- **WHEN** 執行 `feature:collect` 的 JVM 單元測試
- **THEN** 測試 MUST 驗證收藏清單轉為成功 UI state，並驗證點擊已收藏電影會呼叫 repository 的刪除操作
