## 1. 文案準備（Localizable.xcstrings）

- [ ] 1.1 打開 `iosApp/iosApp/Localizable.xcstrings`，新增以下 key 的 `en` 與 `zh-Hant` 兩組翻譯：`setting_theme_title`（主題設定）、`setting_theme_light`（淺色）、`setting_theme_dark`（深色）、`setting_theme_system`（系統預設）
- [ ] 1.2 新增語言設定相關 key（文案措辭聚焦「電影內容語言」而非泛稱「語言」，避免使用者誤以為會改變 App 顯示語言）：`setting_language_title`、`setting_language_system_default`、`setting_language_traditional_chinese`、`setting_language_english`
- [ ] 1.3 新增開發者資訊相關 key：`setting_developer_title`、`setting_developer_name_label`、`setting_developer_tech_stack_label`、`setting_developer_github_label`
- [ ] 1.4 確認既有 `main_setting_placeholder` key 之後不再被任何 View 使用，若確定無其他引用可一併移除（非必要，可留待清理任務）

## 2. ThemeMode 套用（App 根層級）

- [ ] 2.1 在 `iosApp/iosApp/Setting/`（或既有 Common 區域）新增一個 `ThemeMode` → SwiftUI `ColorScheme?` 的轉換 extension／helper（`LIGHT` → `.light`、`DARK` → `.dark`、`SYSTEM` → `nil`）
- [ ] 2.2 修改 `iosApp/iosApp/iOSApp.swift`：新增 `@State private var themeMode: ThemeMode = .system`（或等效初始值）
- [ ] 2.3 在 `IosApp` 加上 `.task` 修飾（掛在 `WindowGroup` 內容或以 `.onAppear` 皆可），以 `for await userData in KoinHelper.shared.userDataRepository().userData` 持續更新 `themeMode`
- [ ] 2.4 對 `WindowGroup` 的內容套用 `.preferredColorScheme(themeMode.toColorScheme())`（依 2.1 的 helper 命名調整）
- [ ] 2.5 手動驗證：App 啟動後於設定頁切換主題，確認 Splash（重新啟動 App 觀察）與所有分頁的外觀都同步套用新主題

## 3. SettingViewModel

- [ ] 3.1 新增 `iosApp/iosApp/Setting/SettingViewModel.swift`，比照 `HistoryViewModel.swift` 的結構：`@Observable @MainActor final class SettingViewModel`
- [ ] 3.2 建構子注入 `userDataRepository: UserDataRepository`（由 `SettingView.init()` 透過 `KoinHelper.shared.userDataRepository()` 取得後傳入）
- [ ] 3.3 新增 `private(set) var userData: UserData`（預設值可用 shared 的 `UserData.companion.getDefault()`，若 Swift 端可見；否則用等效初始狀態），並新增 `func observeUserData() async` 以 `for await userData in userDataRepository.userData` 更新此狀態
- [ ] 3.4 新增 `func setThemeMode(_ mode: ThemeMode) async`：呼叫 `try await userDataRepository.setThemeMode(mode)`，錯誤處理比照 `HistoryViewModel`（`catch` 後 `print` 錯誤訊息，不向上拋出）
- [ ] 3.5 新增 `func setLanguageMode(_ mode: LanguageMode) async`：呼叫 `try await userDataRepository.setLanguageMode(mode)`，錯誤處理同上

## 4. SettingView UI

- [ ] 4.1 將 `iosApp/iosApp/Setting/SettingView.swift` 由 placeholder 改為 `@State private var viewModel: SettingViewModel`，`init()` 中以 `KoinHelper.shared.userDataRepository()` 建立
- [ ] 4.2 加上 `.task { await viewModel.observeUserData() }`
- [ ] 4.3 實作清單畫面：三個列項（主題設定／語言設定／開發者資訊），列項右側顯示目前值的文案（比照 Android `theme_setting_current_format`／`language_setting_current_format` 的「目前為 xxx」呈現方式）
- [ ] 4.4 主題列項綁定 `.confirmationDialog`，列出 LIGHT／DARK／SYSTEM 三個按鈕 + 一個 `role: .cancel` 的「取消」按鈕；點擊選項後呼叫 `viewModel.setThemeMode(_:)`。按鈕文字不額外標註目前選中項目（目前值只在列項本身顯示，見 4.3）
- [ ] 4.5 語言列項綁定另一個 `.confirmationDialog`，列出 SYSTEM_DEFAULT／TRADITIONAL_CHINESE／ENGLISH 三個按鈕 + 一個 `role: .cancel` 的「取消」按鈕；點擊選項後呼叫 `viewModel.setLanguageMode(_:)`。同樣不在按鈕文字標註目前值
- [ ] 4.6 開發者資訊列項改為觸發 `.sheet(isPresented:)`，Sheet 內容顯示 App 名稱「JetpackMovieKMP」、開發者資訊、技術棧文字，以及可點擊開啟 GitHub 網址的連結（`Link` 或 `onTapGesture` + `openURL`）

## 5. 手動驗證

- [x] 5.1 驗證主題：分別選擇 LIGHT／DARK／SYSTEM，確認外觀立即改變且重啟 App 後仍保留選擇（DataStore 持久化）
- [x] 5.2 驗證語言：選擇 TRADITIONAL_CHINESE／ENGLISH，前往 Home／Search 等頁面確認電影資料的內容語言改變，同時確認 App 選單文字（String Catalog）維持系統語言不變（第一次驗證時發現已載入清單沒有即時反映新語言，已透過 7.1~7.4 修正並重新驗證通過）
- [x] 5.3 驗證 SYSTEM_DEFAULT：選擇 SYSTEM_DEFAULT 後，於裝置系統設定切換系統語言，重新整理內容確認 TMDB 內容語言跟著系統語言變化
- [x] 5.4 驗證開發者資訊：確認顯示「JetpackMovieKMP」且 GitHub 連結可正常開啟
- [x] 5.5 確認 `MainTab.swift` 不需要任何修改（`.setting` case 已存在），且沒有新增依賴轉送參數（已用 `git diff` 對照分支起點確認 `MainTab.swift` 全程沒有異動）

## 6. 選用：單元測試（視時間決定，非必要）

- [ ] 6.1（選用）在 `iosApp/iosAppTests/Setting/` 新增 `SettingViewModelTests.swift`，比照 `Search/SearchViewModelTests.swift` 的 Fake Repository 模式，驗證 `setThemeMode`／`setLanguageMode` 有轉發呼叫給 `UserDataRepository`
- [ ] 6.2（選用）驗證 `observeUserData()` 正確反映 Fake Repository 發出的 `userData` Flow

## 7. 語言切換即時刷新（Home／Search）

- [x] 7.1 `HomeViewModel.swift` 新增 `lastLanguageMode: LanguageMode?` 與 `observeLanguageMode() async`：監聽 `userDataRepository().userData` 的 `languageMode`，變化時對所有已快取的 `presenters.values` 呼叫 `.refresh()`
- [x] 7.2 `HomeView.swift` 加上並行的 `.task { await viewModel.observeLanguageMode() }`
- [x] 7.3 `SearchViewModel.swift` 同樣新增 `lastLanguageMode` 與 `observeLanguageMode() async`，變化時呼叫既有的 `refresh()`
- [x] 7.4 `SearchView.swift` 加上對應的 `.task`
- [x] 7.5 確認 Favorites／History 不需要相同處理（本地已收藏/已看過資料快照，語言切換不回溯翻譯，跟 Android 版行為一致）
