# android-setting-module Specification

## Purpose

定義 Android 設定頁 feature module（`feature:setting`）：底部導覽的設定入口、主題模式／語言模式的檢視與變更、開發者資訊顯示，以及對應的 Koin 接線與單元測試驗收標準。

## Requirements

### Requirement: `feature:setting` MUST be an Android-only independently compilable feature module

專案 MUST 新增 `feature:setting`，並在 `settings.gradle.kts` 註冊此模組。其 Android namespace 與 Kotlin package MUST 使用 `com.shang.jetpackmoviekmp.feature.setting`；Gradle 依賴 MUST 僅使用既有 version catalog alias 與專案模組，且不得導入 Hilt、Dagger 或 classic Navigation Compose。

#### Scenario: setting module 可獨立建置

- **WHEN** 執行 `./gradlew :feature:setting:build`
- **THEN** 建置 MUST 成功完成，且不得因缺少 Compose、Koin、Navigation3、shared model/data 或 core UI 依賴而失敗

#### Scenario: setting 模組不殘留來源專案架構

- **WHEN** 檢查 `feature/setting` 的 Gradle 檔與 Kotlin 原始碼
- **THEN** MUST NOT 出現 `com.shang.setting`、`com.shang.data`、`com.shang.model`、Hilt 或 classic Navigation Compose 的依賴、import 或注解

### Requirement: 設定頁 MUST 反映目前的主題模式與語言模式

`SettingViewModel` MUST 透過既有 `UserDataRepository.userData` 訂閱使用者偏好設定，並以可觀察的 UI state 提供給 `SettingScreen`。畫面 MUST 顯示目前的主題模式（LIGHT／DARK／SYSTEM）與語言模式（SYSTEM_DEFAULT／TRADITIONAL_CHINESE／ENGLISH）對應的在地化文案。

#### Scenario: 顯示目前主題與語言設定

- **WHEN** `userData` 發出含特定 `themeMode`／`languageMode` 的資料
- **THEN** 設定頁 MUST 顯示對應該 `themeMode` 的主題文案與對應該 `languageMode` 的語言文案

### Requirement: 使用者 MUST 能透過設定頁變更主題模式

設定頁 MUST 提供主題選擇 Dialog，列出 LIGHT／DARK／SYSTEM 三個選項。使用者選擇後，`SettingViewModel` MUST 呼叫 `UserDataRepository.setThemeMode()` 持久化選擇，並關閉 Dialog。

#### Scenario: 選擇新主題後持久化

- **WHEN** 使用者在主題選擇 Dialog 點擊一個與目前不同的主題選項
- **THEN** `SettingViewModel` MUST 呼叫 `setThemeMode()` 一次，並傳入使用者選擇的 `ThemeMode`
- **AND** Dialog MUST 關閉

### Requirement: 使用者 MUST 能透過設定頁變更語言模式

設定頁 MUST 提供語言選擇 Dialog，列出 SYSTEM_DEFAULT／TRADITIONAL_CHINESE／ENGLISH 三個選項。使用者選擇後，`SettingViewModel` MUST 呼叫 `UserDataRepository.setLanguageMode()` 持久化選擇，並關閉 Dialog。

#### Scenario: 選擇新語言後持久化

- **WHEN** 使用者在語言選擇 Dialog 點擊一個與目前不同的語言選項
- **THEN** `SettingViewModel` MUST 呼叫 `setLanguageMode()` 一次，並傳入使用者選擇的 `LanguageMode`
- **AND** Dialog MUST 關閉

### Requirement: 設定頁 MUST 提供開發者資訊 Dialog

設定頁 MUST 提供開發者資訊項目，點擊後顯示 Dialog，內容 MUST 包含更新後的 app 名稱「JetpackMovieKMP」、開發者資訊與可點擊的 GitHub 連結。

#### Scenario: 顯示開發者資訊

- **WHEN** 使用者點擊設定頁的開發者資訊項目
- **THEN** MUST 顯示開發者資訊 Dialog，且內容 MUST 顯示「JetpackMovieKMP」而非來源專案的舊名稱

### Requirement: setting ViewModel MUST be supplied by Koin and unit tested

`feature:setting` MUST 提供 `settingModule()`，以 Koin `viewModel { }` 建立 `SettingViewModel` 並注入 `UserDataRepository`。`userData` 轉發與 `setThemeMode`／`setLanguageMode` 轉發行為 MUST 以 AAA 結構的單元測試覆蓋，不得要求 Android 裝置或 Hilt 測試環境。

#### Scenario: Koin 可解析 setting ViewModel

- **WHEN** App 已載入 shared data module 與 `settingModule()`
- **THEN** Koin MUST 能建立 `SettingViewModel` 並提供其 `UserDataRepository` 依賴

#### Scenario: ViewModel 行為測試通過

- **WHEN** 執行 `feature:setting` 的 JVM 單元測試
- **THEN** 測試 MUST 驗證 `userData` 直接反映 repository 資料流，並驗證 `setThemeMode`／`setLanguageMode` 呼叫各自轉發至 repository 對應方法一次
