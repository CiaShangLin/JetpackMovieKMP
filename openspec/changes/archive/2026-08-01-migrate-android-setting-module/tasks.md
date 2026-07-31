## 1. feature/setting

- [x] 1.1 在 `settings.gradle.kts` 註冊 `:feature:setting`，建立 Android library Gradle 組態與 `com.shang.jetpackmoviekmp.feature.setting` 原始碼結構，依賴既有 core/shared 模組（`core:designsystem`、`core:ui`、`shared:common`、`shared:data`、`shared:model`）及 version catalog alias，不依賴 `shared:domain`。
- [x] 1.2 建立 `SettingViewModel`，直接注入 `UserDataRepository`：以 `stateIn` 轉發 `userData`，並提供 `setThemeMode()`／`setLanguageMode()`。
- [x] 1.3 建立 `SettingScreen`，列出主題設定、語言設定、開發者資訊三個項目，改用 `androidx.compose.material3.HorizontalDivider`（取代來源專案的 M2 `Divider`）。
- [x] 1.4 建立 `ThemeSettingDialog`（LIGHT／DARK／SYSTEM 單選）與 `LanguageSettingDialog`（SYSTEM_DEFAULT／TRADITIONAL_CHINESE／ENGLISH 單選），沿用來源專案的 `Dialog` + `Card` + `selectableGroup` 結構。
- [x] 1.5 建立 `DevelopersSettingDialog`，app 名稱更新為「JetpackMovieKMP」、技術棧文案更新為 Kotlin Multiplatform / Compose Multiplatform，開發者姓名與 GitHub 連結維持原樣。
- [x] 1.6 新增設定頁字串資源：僅加入實際會用到的 key（清單見 design.md「字串資源清理」），不搬移來源專案未使用的 `theme_setting_content`／`language_setting_content` 與無對應列舉值的 `language_simplified_chinese`。
- [x] 1.7 建立 `SettingKey`、setting Navigation3 entry factory（`settingEntry()`，無需回呼參數）與 `settingModule()`，以 Koin 提供 `SettingViewModel`，不得使用 Hilt 或 classic Navigation Compose。
- [x] 1.8 新增 `SettingViewModel` JVM 單元測試（`kotlin.test` + Fake `UserDataRepository`，比照 `CollectViewModelTest` 風格），依 AAA 驗證 `userData` 轉發，以及 `setThemeMode`／`setLanguageMode` 各自轉發至 repository 一次。

## 2. androidApp

- [x] 2.1 在 `androidApp/build.gradle.kts` 加入 `feature:setting` 依賴，並在 App 啟動 Koin 流程載入 `settingModule()`。
- [x] 2.2 在 `androidApp` 新增 `nav_setting` 字串資源，恢復 `MainNavItem.SETTING`（取消註解），以 `SettingKey` 與 `Icons.Rounded.Settings`／`Icons.Outlined.Settings` 建立底部導覽項目。
- [x] 2.3 更新 `MainActivity` 的 Navigation3 `NavDisplay` entryProvider，將 `SettingKey` 分派至 `settingEntry()`，保留尚未遷移目的地的 placeholder 回退行為。

## 3. 驗證

- [x] 3.1 執行 `./gradlew :feature:setting:test`，確認 setting ViewModel JVM 測試通過。
- [x] 3.2 執行 `./gradlew :feature:setting:build` 與 `./gradlew :androidApp:assembleDebug`，確認 feature 與 Android App 編譯及接線成功。
- [x] 3.3 執行 `./gradlew ktlintCheck`，確認新增 Kotlin 程式碼符合專案格式規範。
