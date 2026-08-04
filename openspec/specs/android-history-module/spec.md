# android-history-module Specification

## Purpose

定義 Android 觀看歷史 feature module 的建置、資料互動、導覽與單元測試驗收標準。

## Requirements

### Requirement: `feature:history` MUST be an Android-only independently compilable feature module

專案 MUST 新增 `feature:history`，並在 `settings.gradle.kts` 註冊此模組。其 Android namespace 與 Kotlin package MUST 使用 `com.shang.jetpackmoviekmp.feature.history`；Gradle 依賴 MUST 僅使用既有 version catalog alias 與專案模組（含 `shared/domain`），且不得導入 Hilt、Dagger 或 classic Navigation Compose。

#### Scenario: history module 可獨立建置

- **WHEN** 執行 `./gradlew :feature:history:build`
- **THEN** 建置 MUST 成功完成，且不得因缺少 Compose、Koin、Navigation3、`shared/model`、`shared/data`、`shared/domain` 或 `core/ui` 依賴而失敗

#### Scenario: history 模組不殘留來源專案架構

- **WHEN** 檢查 `feature/history` 的 Gradle 檔與 Kotlin 原始碼
- **THEN** MUST NOT 出現 `com.shang.history`、`com.shang.data`、`com.shang.model`、`com.shang.ui`、Hilt 或 classic Navigation Compose 的依賴、import 或注解

### Requirement: 歷史頁 MUST 反映本機觀看紀錄並提供空狀態

`HistoryViewModel` MUST 透過既有 `GetHistoryMovieListUseCase` 訂閱觀看歷史（含已合併的收藏狀態），並以 `HistoryUiState`（`Empty`／`Success`）提供給 `HistoryScreen`。清單有資料時，畫面 MUST 以既有 `MovieCard` 與 grid 顯示每一筆歷史紀錄；清單為空時 MUST 顯示歷史頁專用的空狀態圖示與文案。

#### Scenario: 有歷史資料時顯示觀看紀錄

- **WHEN** `GetHistoryMovieListUseCase()` 發出至少一筆 `MovieCardResult`
- **THEN** 歷史頁 MUST 顯示歷史標題與每筆對應的 `MovieCard`，且每張卡片的收藏按鈕狀態 MUST 反映該筆 `MovieCardResult.isCollect`

#### Scenario: 尚無歷史資料時顯示空狀態

- **WHEN** `GetHistoryMovieListUseCase()` 發出空清單
- **THEN** 歷史頁 MUST 顯示空狀態圖示與「目前沒有歷史資料」的在地化文案，且不得顯示空白 grid

### Requirement: 歷史頁 MUST allow user to toggle collect status of a movie

歷史清單中的 `MovieCard` MUST 將收藏按鈕回呼交給 `HistoryViewModel`。ViewModel MUST 依被點擊項目目前的 `movieCardIsCollect` 狀態決定行為：若已收藏，MUST 呼叫 `MovieRepository.deleteMovieCollect()`；若尚未收藏，MUST 呼叫 `MovieRepository.insertMovieCollect()`。此操作 MUST NOT 影響該筆歷史紀錄本身是否存在。

#### Scenario: 對已收藏電影點擊收藏按鈕會取消收藏

- **WHEN** 使用者在歷史頁點擊一筆 `movieCardIsCollect == true` 電影的收藏按鈕
- **THEN** ViewModel MUST 呼叫 `deleteMovieCollect()` 一次，且傳入與被點擊電影相同 id 的資料

#### Scenario: 對未收藏電影點擊收藏按鈕會加入收藏

- **WHEN** 使用者在歷史頁點擊一筆 `movieCardIsCollect == false` 電影的收藏按鈕
- **THEN** ViewModel MUST 呼叫 `insertMovieCollect()` 一次，且傳入與被點擊電影相同 id 的資料

### Requirement: 歷史頁 MUST allow user to clear all history

歷史頁 MUST 提供清空全部歷史紀錄的操作入口。觸發時，`HistoryViewModel` MUST 呼叫 `MovieRepository.deleteAllMovieHistory()`；Repository 發出更新後，畫面 MUST 反映清空結果（切換為空狀態）。

#### Scenario: 清空全部歷史

- **WHEN** 使用者在歷史頁點擊清空按鈕
- **THEN** ViewModel MUST 呼叫 `deleteAllMovieHistory()` 一次

#### Scenario: 清空後顯示空狀態

- **WHEN** 使用者清空全部歷史，且 Repository 後續發出空清單
- **THEN** 歷史頁 MUST 由電影 grid 切換為空狀態

### Requirement: history ViewModel MUST be supplied by Koin and unit tested

`feature:history` MUST 提供 `historyModule()`，以 Koin `viewModel { }` 建立 `HistoryViewModel` 並注入 `GetHistoryMovieListUseCase` 與 `MovieRepository`。歷史清單轉換、切換收藏、清空歷史等行為 MUST 以 AAA 結構的單元測試覆蓋，不得要求 Android 裝置或 Hilt 測試環境。

#### Scenario: Koin 可解析 history ViewModel

- **WHEN** App 已載入 `shared/domain` module、`shared/data` module 與 `historyModule()`
- **THEN** Koin MUST 能建立 `HistoryViewModel` 並提供其 `GetHistoryMovieListUseCase`與 `MovieRepository` 依賴

#### Scenario: ViewModel 行為測試通過

- **WHEN** 執行 `feature:history` 的 JVM 單元測試
- **THEN** 測試 MUST 驗證歷史清單轉為成功／空 UI state、點擊已收藏電影會呼叫刪除收藏、點擊未收藏電影會呼叫新增收藏，並驗證清空操作會呼叫 `deleteAllMovieHistory()`

### Requirement: 歷史頁字串資源 MUST 具備繁體中文與英文版本

`feature:history` 的 `res/values/strings.xml` MUST 同時提供繁體中文（預設）與英文（`values-en-rUS/strings.xml`）兩份翻譯，確保切換到英文語系時歷史頁文字正確顯示英文，而非 fallback 回中文。

#### Scenario: 語言模式為英文時歷史頁顯示英文

- **WHEN** `languageMode` 為 `LanguageMode.ENGLISH`
- **THEN** 歷史頁 MUST 顯示 `values-en-rUS/strings.xml` 定義的英文文案
