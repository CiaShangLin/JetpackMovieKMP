## Why

目前 Android App 的底部導覽尚未提供設定頁，使用者無法在畫面上切換主題、語言，或檢視開發者資訊，即便底層 `UserDataRepository` 早已支援這些偏好設定的讀寫。遷移來源專案的 setting feature，可補齊 Android 端的設定流程，並沿用 KMP 專案既有的 Koin、Navigation3 架構與現行的 Repository 直接注入慣例。

## What Changes

- 新增 Android-only `feature:setting` Gradle 模組，承載設定清單的 ViewModel、Compose 畫面、三個設定 Dialog、Navigation3 entry 與 Koin module。
- 顯示 `UserDataRepository.userData` 目前的主題模式與語言模式，並提供對應的選擇 Dialog（主題：LIGHT／DARK／SYSTEM；語言：SYSTEM_DEFAULT／TRADITIONAL_CHINESE／ENGLISH）。
- 使用者可透過 Dialog 選擇主題或語言，選擇後呼叫 `UserDataRepository.setThemeMode()`／`setLanguageMode()` 持久化。
- 新增「開發者資訊」Dialog，顯示更新後的新專案名稱（JetpackMovieKMP）、開發者與技術棧資訊、GitHub 連結。
- 將設定分頁接入 `MainNavItem`、`MainActivity` 的 Navigation3 `NavDisplay`，並載入 setting Koin module。
- 加入設定頁所需的 Android 字串資源（僅搬移實際會用到的 key，捨棄來源專案未使用或無對應列舉值的死碼字串），並補齊 ViewModel 行為測試。
- 本次僅處理 Android UI；iOS 的 `SettingView.swift` 維持現有 placeholder，不在此變更範圍內。

## Capabilities

### New Capabilities

- `android-setting-module`: Android 使用者可由底部導覽進入設定頁，檢視並變更主題模式、語言模式，並查看開發者資訊。

### Modified Capabilities

- `android-app-entry`: Android 主導覽新增設定目的地，並將其分派至已遷移的 setting feature。

## Impact

- 受影響模組：`feature/setting`（新增）、`androidApp`、`shared/data`（僅使用既有 `UserDataRepository` 介面）、`shared/model`（僅使用既有 `UserData`／`ThemeMode`／`LanguageMode`）、`core/designsystem`、`core/ui`。
- `settings.gradle.kts` 與 `androidApp/build.gradle.kts` 需接入新 feature module；預期所有 Gradle 依賴皆沿用既有 `gradle/libs.versions.toml` alias，不新增版本或外部依賴。
- 不變更資料庫 schema、`UserDataRepository` API 或 iOS App；本次只處理 Android setting UI 與導覽整合。
