## Context

`iosApp` 目前的設定分頁只有 `SettingView.swift` 一個 placeholder View，`MainTab.swift` 已經把 `.setting` case 接好（icon、tab title、指向 `SettingView()`），`shared` 層（`UserData`／`ThemeMode`／`LanguageMode`／`UserDataRepository`／`DatastoreLanguageProvider`）與 `KoinHelper.userDataRepository()` 也已存在且可直接被 Swift 呼叫，不需要新增任何 shared Kotlin API。本次要做的是純 iOS（Swift）端的功能補齊，讓 iOS 設定頁的能力對齊 Android `feature:setting`（主題／語言／開發者資訊），但語言部分刻意縮小範圍（見 Non-Goals）。

使用者會依 tasks.md 的步驟自行在 Xcode 手動實作（不是委派自動寫 code），因此設計需要拆得夠細、每一步都可獨立驗證。

## Goals / Non-Goals

**Goals:**
- iOS 設定頁可檢視並變更主題模式（LIGHT／DARK／SYSTEM），變更後整個 App（不只設定頁）套用新主題。
- iOS 設定頁可檢視並變更語言模式（SYSTEM_DEFAULT／TRADITIONAL_CHINESE／ENGLISH），此設定僅影響 TMDB 內容語言（透過既有 `DatastoreLanguageProvider`），`SYSTEM_DEFAULT` 行為與現有系統語言偵測（`SystemLanguage.ios.kt` 的 `currentSystemLanguageCode()`）一致。
- iOS 設定頁可顯示開發者資訊（App 名稱、開發者、技術棧、可點擊 GitHub 連結）。
- 沿用 `ios-movie-history` 已確立的慣例：Feature View 直接透過 `KoinHelper` 取得 shared 依賴並自行組出 ViewModel，不由 `MainView`／`MainTab` 逐一轉送依賴。
- 新增文案透過 `Localizable.xcstrings`（`en` + `zh-Hant`）管理，不寫死字面字串。

**Non-Goals:**
- 不提供 App 內 UI 顯示語言切換（不覆蓋 `Localizable.xcstrings` 的 lookup 語言），維持 `ios-localization` 規格現狀：UI 文案永遠跟系統語言走。
- 不修改任何 shared Kotlin 模組（`shared/model`／`datastore`／`data`／`domain`／`app`）或 `KoinHelper` 既有 accessor（`userDataRepository()` 已存在，足夠使用）。
- 不修改 Android 端（`feature:setting`、`androidApp`、`core/*`）。
- 不要求 100% XCTest 覆蓋；是否補 `SettingViewModel` 的 XCTest（比照 `SearchViewModelTests` 用 Fake Repository 的模式）留給使用者依時間決定，tasks.md 會將其列為可選步驟而非必要步驟。

## Decisions

### 1. ViewModel 建構方式：View `init()` 直接呼叫 `KoinHelper`（沿用 History 模式）

`SettingView` 比照 `HistoryView` 的做法，在 `init()` 中直接呼叫 `KoinHelper.shared.userDataRepository()` 建立 `SettingViewModel`，而不是由 `MainView`／`MainTab` 轉送依賴。

- 目前 repo 內存在兩份看似矛盾的規格：`ios-koin-bridge` 舊規格寫「消費端 SHALL 由組裝根取得實例後以建構子傳遞」，但 `ios-movie-history`（較新）明確寫「View SHALL 直接透過 KoinHelper 取得依賴，MainView／MainTab SHALL NOT 逐一轉送」，且 `HistoryView.swift` 的實際程式碼就是直接呼叫 `KoinHelper`。本設計以「實際程式碼 + 較新規格」為準，沿用 History 模式，維持一致性；不在本次 change 中處理兩份規格的歷史矛盾（非本次範圍）。
- 主題套用是唯一例外：因為主題需要在 App 根層級（`IosApp` / `WindowGroup`）套用，範圍跨越所有 tab，`IosApp.swift` 本身已經是組裝根並已直接呼叫 `KoinHelper`（`InitKoinIosKt.doInitKoinIos`），所以在同一層直接呼叫 `KoinHelper.shared.userDataRepository()` 觀察 `themeMode` 是合理延伸，不需要額外抽象。

### 2. 主題套用位置：`IosApp`（App 根層級），而非 `SettingView` 或 `MainView`

在 `IosApp` 新增一個小型狀態持有者（例如 `@State private var themeMode: ThemeMode`），於 `.task` 中以 `for await userData in KoinHelper.shared.userDataRepository().userData` 更新，並在 `WindowGroup` 內容套用 `.preferredColorScheme(themeMode.toColorScheme())`（`SYSTEM` 對應 `nil`，交還系統決定）。

- 理由：主題若只套在 `MainView` 內，`SplashView` 階段不會套用新主題，會出現「設定頁改了、但 Splash 沒反應」的不一致；套在 `IosApp` 的 `WindowGroup` 層級可以同時覆蓋 Splash 與 Main。
- 對照 Android：`MainActivity` 的 `ThemeProvider` 是在 Activity（App 進入點）層級套用 `JetpackMovieComposeTheme`，位置概念一致。

### 3. 主題／語言選擇 UI 元件：`confirmationDialog`，而非自訂 Sheet 或 Picker 頁面

主題與語言都是「3 個互斥選項、選了就關閉」的簡單選擇，SwiftUI 原生 `.confirmationDialog`（action sheet）語意與 Android 的 radio-button `AlertDialog` 最接近，且不需要額外的 View 檔案、狀態管理或 navigation push，最省工。開發者資訊維持用 `.sheet`，因為內容是資訊呈現＋連結，不是選項清單。

- 替代方案（已否決）：獨立 `ThemeSettingView`／`LanguageSettingView` 全螢幕頁面 — 對三選一場景是不必要的重量級方案，且需要額外的 navigation 狀態；`Picker` inline 展開 — 在列表中展開會改變設定頁排版高度，體驗與 Android 的 Dialog 形式不一致。
- `confirmationDialog` SHALL 包含一個 `role: .cancel` 的「取消」按鈕，讓使用者可以不選就關閉（等效於 Android `AlertDialog` 可點外部關閉的行為）。
- 目前選中的選項 SHALL NOT 在 `confirmationDialog` 的按鈕文字上額外標註（例如不加「（目前）」字樣）；`confirmationDialog` 按鈕本身不支援勾選狀態，硬加文字容易造成多語系拼接負擔且效果有限。目前值改為只在設定頁列項本身顯示（見 tasks.md 4.3），使用者開啟 dialog 前就已經看得到目前選項。

### 4. 語言模式的「系統偵測」語意如何落地

`SYSTEM_DEFAULT` 這個選項本身不需要 iOS 額外實作偵測邏輯——`DatastoreLanguageProvider` 已經在 `SYSTEM_DEFAULT` 時 fallback 呼叫 `currentSystemLanguageCode()`（`SystemLanguage.ios.kt` 讀 `NSLocale.currentLocale.languageCode`），這條路徑對 iOS 已經生效（因為 `datastoreModule` 是 cross-platform 註冊）。iOS 端只需要讓使用者能夠「選擇 SYSTEM_DEFAULT」這個選項並持久化，不需要另外寫偵測程式碼；使用者要求的「偵測 iOS 語言體系切換」在 shared 層已經免費具備，設計上只需驗證（手動測試：切換裝置語言 → 選 SYSTEM_DEFAULT → 確認 TMDB 內容語言跟著變）。

### 5. 文案管理：擴充既有 `Localizable.xcstrings`

新增 key 沿用 Android `strings.xml` 的語意但採 iOS 慣例命名（例如 `setting_theme_title`、`setting_theme_light`、`setting_theme_dark`、`setting_theme_system`、`setting_language_title`、`setting_language_traditional_chinese`、`setting_language_english`、`setting_language_system_default`、`setting_developer_title`、`setting_developer_name_label`、`setting_developer_tech_stack_label`、`setting_developer_github_label`），需要 `en` 與 `zh-Hant` 兩組翻譯。

### 6. 語言切換後主動刷新已載入清單（Home／Search），而非僅依賴「下次請求」

手動驗證時發現：雖然 `DatastoreLanguageProvider` 即時反映最新語言、Ktor `defaultRequest` 每次請求都會重新讀取，但 `HomeViewModel`／`SearchViewModel` 的 Paging presenter 一旦建立就終身快取，且 `MainView` 用 `TabView` + `ForEach` 一次建立所有分頁 View（不是切到才建立），導致「下次請求」在實務上只有強制關閉 App 重開才會觸發，沒有其他自然時機。

- 決定：`HomeViewModel`／`SearchViewModel` 各自新增 `observeLanguageMode() async`，監聽 `KoinHelper.shared.userDataRepository().userData` 的 `languageMode`，偵測到跟上一次觀察到的值不同時，主動觸發已快取 Paging presenter 的 refresh（Home 呼叫 `HomeMovieListPresenter.refresh()`；Search 呼叫既有的 `SearchViewModel.refresh()`，重新提交目前搜尋詞）。`HomeView`／`SearchView` 各自加一個並行的 `.task` 持續觀察。
- 範圍排除 Favorites／History：這兩頁顯示的是本地 DB 已收藏/已看過資料的快照，語言切換不會、也不應該回溯翻譯已存紀錄，此行為與 Android 版一致，不需要處理。
- 理由：對照主題套用（`IosApp` 監聽 `userData` 即時套用 `preferredColorScheme`），語言變更也應該有等效的「操作後立即看到效果」體驗，而不是讓使用者誤以為切換沒生效、得自己強制重啟 App 才能驗證。

## Risks / Trade-offs

- **[風險]** 兩份既有 spec（`ios-koin-bridge` vs `ios-movie-history`）對「是否該由組裝根轉送依賴」給出不同答案，未來若有人依字面讀舊的 `ios-koin-bridge` spec 可能誤解慣例。
  → **緩解**：本次不修改任何既有 spec（維持最小變更），但在 `ios-setting-module` 新 spec 中明確寫清楚「直接透過 KoinHelper 取得依賴」是本次採用的慣例並附理由；若使用者之後想徹底解決兩份 spec 矛盾，可另開一個小 change 專門修正 `ios-koin-bridge`。
- **[風險]** `.preferredColorScheme` 套在 `WindowGroup` 層級，若之後有其他 Scene 或多視窗需求，套用位置需要重新檢視。
  → **緩解**：目前 App 只有單一 `WindowGroup`，此風險現階段不存在；標記為 Open Question 供未來擴充時參考。
- **[取捨]** 語言設定範圍縮小為「僅內容語言」，與 Android「同時覆蓋 UI 顯示語言」不完全一致，是刻意的產品行為差異（已與使用者確認），需要在使用者可見文案上避免造成「選了英文結果畫面文字沒變」的困惑——建議語言選項文案聚焦於「電影內容語言」而非泛稱「語言」，降低誤解風險（tasks.md 會列出文案措辭要點）。
- **[取捨]** 語言切換時若使用者已瀏覽多個 Home genre 分類，每個已建立的 presenter 都會各自重新呼叫一次 TMDB API，請求次數隨已瀏覽分類數增加；目前規模可接受，未來 genre 數量大幅增加可再評估節流。

## Open Questions

- 未來若 App 支援多視窗（iPadOS Stage Manager 多開同一 App 的情境），`.preferredColorScheme` 是否需要改成每個 Scene 各自訂閱：目前非本次範圍，先記錄以待未來評估。
- 是否要在同一次 change 順便修正 `ios-koin-bridge` 舊規格與實際慣例不一致的問題：使用者尚未表態，先不處理，留待使用者決定是否另開 change。
