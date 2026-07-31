## Context

`shared:data` 已透過 `UserDataRepository` 提供 `userData`（含 `themeMode`／`languageMode`）的即時 `Flow`，且 `setThemeMode()`／`setLanguageMode()` 已可持久化使用者選擇；`androidApp` 的 `MainActivity`／`MainViewModel` 也已讀取 `userData` 套用主題與語言（`ThemeProvider`／`LanguageProvider`／`LanguageSettingUtils`）。但目前 Android App 的 Navigation3 導覽尚未註冊任何設定目的地，`MainNavItem.SETTING` 仍被註解，因此使用者沒有管道去「變更」這些偏好設定，只能被動套用預設值。

本次以來源專案 `JetpackMovieCompose/feature/setting` 的使用者行為為參考，但必須遵循本專案的 Android feature 分層：Koin 取代 Hilt、Navigation3 取代 classic Navigation Compose，並使用本專案的 package 與既有共用資料模型。iOS App（`SettingView.swift` 目前為 placeholder）與 shared KMP API 均不在範圍內。

## Goals / Non-Goals

**Goals:**

- 建立可獨立編譯的 Android-only `feature:setting` 模組。
- 依既有 MVVM + Repository 模式，以 `SettingViewModel` 直接轉發 `UserDataRepository.userData`，並提供 `setThemeMode()`／`setLanguageMode()`。
- 提供主題選擇、語言選擇、開發者資訊三個 Dialog，並以 Koin 注入 ViewModel、以 Navigation3 `SettingKey`／entry factory 整合至底部導覽。
- 更新「開發者資訊」內容為新專案資訊（app 名稱 JetpackMovieKMP），技術棧文案更新為反映 Kotlin Multiplatform / Compose Multiplatform。
- 只搬移實際會用到的字串資源，捨棄來源專案的死碼字串（未被畫面引用、或無對應 `LanguageMode` 列舉值）。
- 為 ViewModel 的 userData 轉發、setThemeMode／setLanguageMode 轉發行為補 AAA 單元測試。

**Non-Goals:**

- 不新增或調整 `shared:data`、`shared:domain`、Room schema 或資料庫 migration；不新增 `SetThemeModeUseCase`／`SetLanguageModeUseCase` 之類的 domain UseCase。
- 不處理 iOS 的設定畫面（`iosApp/iosApp/Setting/SettingView.swift` 維持現有 placeholder）。
- 不遷移搜尋、歷史、收藏等其餘已完成或待處理的底部導覽頁。
- 不新增設定項目（例如通知、快取清除等來源專案沒有的功能），也不新增共用 Dialog 元件（目前專案沒有先例，本次沿用來源專案手刻 `Card` + `Dialog` 的寫法）。

## Decisions

### 新增 Android-only feature module，沿用 `feature:collect` 模組結構

新增 `feature/setting`，其 package 為 `com.shang.jetpackmoviekmp.feature.setting`，並包含 `ui`、`navigation`、`di`（Dialog 一併放在 `ui` 底下，不另外拆 `dialog` package，因為數量少且都只被 `SettingScreen` 使用）。Gradle 組態與必要依賴沿用 `feature:collect` 的 Android library 與既有 version catalog alias，直接依賴 `core:designsystem`、`core:ui`、`shared:common`、`shared:data`、`shared:model` 與 Koin，不依賴 `shared:domain`。

選擇此作法是為了維持每個 Android 頁面可獨立遷移、編譯與接線的邊界，與 `feature:collect`、`feature:home` 已建立的遷移模式一致。替代方案是把畫面直接放進 `androidApp`；這會讓 feature UI 與 app 入口耦合，因此不採用。

### ViewModel 直接注入 `UserDataRepository`，不新增 domain UseCase

`SettingViewModel` 直接注入 `UserDataRepository`，以 `stateIn` 轉發 `userData`，並在 `setThemeMode()`／`setLanguageMode()` 內於 `viewModelScope` 呼叫 repository 對應方法。

這是遵循既有 MVVM + Repository 模式：`CollectViewModel`、`MainViewModel` 對於單純轉發型操作（無額外業務邏輯或資料轉換）都直接注入 Repository，未經過 `shared:domain` 的 UseCase 層；`domainModule()` 目前也沒有任何 UseCase 涉及 `setThemeMode`／`setLanguageMode`。替代方案是新增 `SetThemeModeUseCase`／`SetLanguageModeUseCase`，但這只是一層轉發包裝，沒有額外商業邏輯價值，且會讓這個單一 feature 偏離其餘同類型 ViewModel 的既有慣例，因此不採用。

### Koin 與 Navigation3 依既有 collect 模式接線

`settingModule()` 以 Koin `viewModel { }` 提供 `SettingViewModel`，由 `JetpackMovieApplication` 的 `loadKoinModules` 載入。`SettingNavigation.kt` 定義序列化的 `SettingKey : NavKey` 及 `settingEntry()`（無需 `onMovieClick` 之類的回呼參數）；`MainNavItem` 恢復 `SETTING` 項目，以 `SettingKey` 作為 key 並補上 `R.string.nav_setting`，由 `MainActivity` 的 `NavDisplay` 對應到 setting entry。

選擇此作法可使 back stack 只保存 typed `NavKey`，與既有 `HomeKey`／`CollectKey`／`HistoryKey`／`SearchKey` 一致。替代方案是採 classic Navigation Compose 的字串 route 或 Hilt ViewModel，皆與既有 Navigation3 / Koin 架構不相容，因此不採用。

### UI 全面改用 Material3，Dialog 沿用手刻寫法

`SettingScreen` 移除來源專案混用的 `androidx.compose.material.TabRowDefaults.Divider`（M2），改用 `androidx.compose.material3.HorizontalDivider`。三個 Dialog（`ThemeSettingDialog`、`LanguageSettingDialog`、`DevelopersSettingDialog`）沿用來源專案以 `androidx.compose.ui.window.Dialog` + `Card` 手刻的結構與單選互動（`selectableGroup`／`RadioButton`），僅調整 import 與型別命名以符合本專案慣例。

專案目前沒有共用 Dialog 元件先例，本次不額外新增 `core:designsystem` 的共用 Dialog 元件；若未來有第二個需要 Dialog 的 feature，再評估是否抽出共用元件。

### 字串資源清理

僅新增畫面實際會用到的 key：`setting`、`theme_setting_title`、`theme_setting_current_format`、`language_setting_title`、`language_setting_current_format`、`developers_setting_title`、`developers_setting_content`、`theme_selection_title`、`theme_light_mode`、`theme_dark_mode`、`theme_system_default`、`language_selection_title`、`language_traditional_chinese`、`language_english`、`language_system_default`、`developer_info_title`，以及 `androidApp` 既有缺少的 `nav_setting`。捨棄來源專案未被引用的 `theme_setting_content`／`language_setting_content`，以及沒有對應 `LanguageMode` 列舉值的 `language_simplified_chinese`。

### 開發者資訊內容更新

`DevelopersSettingDialog` 顯示的 app 名稱由「JetpackComposeMovie」改為「JetpackMovieKMP」；技術棧文案由單純「Compose」更新為反映本專案的 Kotlin Multiplatform / Compose Multiplatform 技術棧；開發者姓名與 GitHub 連結維持原樣。

## Risks / Trade-offs

- [Dialog 手刻邏輯與畫面狀態耦合在 `SettingScreen`] → 沿用來源專案已驗證過的 `remember { mutableStateOf(false) }` 顯示控制方式，範圍小、無額外狀態管理需求，暫不引入額外的 UI state 封裝。
- [新增 feature 接線遺漏會造成 Koin 或導覽執行期失敗] → 以 ViewModel unit test、`feature:setting` 模組編譯與 `:androidApp:assembleDebug` 驗證 settings、依賴、Koin module 與 Navigation3 接線。
- [字串資源清理可能遺漏來源專案實際有用到的 key] → 遷移時逐一比對 `SettingScreen`／三個 Dialog 的 `stringResource` 呼叫，確保新 `strings.xml` 涵蓋所有實際引用的 key。
- [開發者資訊內容變動屬產品文案決策] → 已在提案討論階段與使用者確認採用新專案資訊，非開發者片面決定。

## Migration Plan

1. 加入 `feature:setting` 至 Gradle settings，建立模組與 Koin / Navigation3 骨架。
2. 遷移 `SettingViewModel`、`SettingScreen` 與三個 Dialog，改用現有 `UserDataRepository`、`UserData`／`ThemeMode`／`LanguageMode`、Koin API，並更新開發者資訊內容與清理字串資源。
3. 在 `androidApp` 接上 Gradle 依賴、Koin module、`MainNavItem.SETTING`（含 `nav_setting` 字串）與 `NavDisplay` entry。
4. 執行 setting ViewModel 測試、`./gradlew :feature:setting:build`、`./gradlew :androidApp:assembleDebug` 與 `./gradlew ktlintCheck`。

本次無持久化資料或公開 API migration；若接線造成啟動問題，回退方式為移除 `feature:setting` 的 app 依賴、導航項與 Koin 載入，即可回復目前沒有設定頁的行為。

## Open Questions

- 無阻塞問題；依已確認的決策，本次僅處理 Android UI，iOS 設定畫面留待未來另立 change。
