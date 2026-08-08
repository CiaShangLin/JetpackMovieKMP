# android-app-entry Specification

## Purpose
定義 `androidApp` 進入點（`MainActivity`）重新整合的驗收標準：涵蓋可編譯／可啟動性、啟動畫面（`androidx.core:core-splashscreen`）、Navigation3 導覽骨架，以及 `MainViewModel` 透過 Koin 注入的方式，確保移除舊有 UI 依賴後 App 仍可正常運作。

## Requirements

### Requirement: `androidApp` 進入點 MUST 可正常編譯並啟動

`androidApp` 模組 MUST 在沒有任何 feature module 畫面接回的情況下，仍可透過 `./gradlew :androidApp:assembleDebug` 成功建置，並在裝置／模擬器上啟動 `MainActivity` 而不發生編譯錯誤或執行期 crash。

#### Scenario: Debug 組建成功

- **WHEN** 執行 `./gradlew :androidApp:assembleDebug`
- **THEN** 建置 MUST 成功完成，不得因缺少 `installSplashScreen()`、導覽 API 或 `MainViewModel` 建構失敗而中止

#### Scenario: 冷啟動不 crash

- **WHEN** 使用者在裝置／模擬器上啟動 App
- **THEN** `MainActivity` MUST 成功進入 `onCreate()` 並渲染畫面（Loading／Error／Success 任一狀態），不得拋出未捕捉例外

### Requirement: `MainActivity` MUST 使用 `androidx.core:core-splashscreen` 顯示啟動畫面

`MainActivity` MUST 透過官方 `androidx.core:core-splashscreen` 的 `installSplashScreen()` API 顯示啟動畫面，並在 `MainViewModel.configuration`（型別為 `shared/common` 的 `UiState<ConfigurationBean>`）為 `UiState.Loading` 時持續顯示，直到設定載入完成。

#### Scenario: 依賴與 theme 設定齊備

- **WHEN** 檢查 `gradle/libs.versions.toml` 與 `androidApp/build.gradle.kts`
- **THEN** MUST 包含 `androidx.core:core-splashscreen` 對應的 catalog alias 與 `implementation` 依賴
- **AND** `AndroidManifest.xml` 的 `MainActivity` `<activity>` 元素 MUST 有 `android:theme` 屬性，指向繼承自 `Theme.SplashScreen` 的 style（例如 `Theme.App.Starting`）
- **AND** 該 style 內引用的所有 resource（例如 `windowSplashScreenBackground` 指向的顏色）MUST 在對應的 `values` resource 檔（例如 `colors.xml`）中已定義，建置時不得出現 resource linking 錯誤

#### Scenario: Splash 隨設定載入狀態收起

- **WHEN** `MainViewModel.configuration` 從 `UiState.Loading` 轉為 `UiState.Success` 或 `UiState.Error`
- **THEN** splash screen MUST 結束顯示，交由對應的 Success／Error 畫面接手

### Requirement: `MainActivity` 主要導覽骨架 MUST 使用 Navigation3

`MainActivity` 的主要導覽骨架 MUST 使用專案既定的 `androidx.navigation3:navigation3-runtime` 與 `androidx.navigation3:navigation3-ui`（`NavDisplay`／entry provider），MUST NOT 依賴 classic `androidx.navigation:navigation-compose`（`NavHostController`／`NavHost`／`rememberNavController`）。已遷移的首頁、收藏頁、歷史頁與設定頁 MUST 各自使用 typed `NavKey`，並由 `MainNavItem` 驅動底部導覽切換。底部導覽 Tab 切換 MUST 使用 `TopLevelBackStack`（每個 `MainNavItem` 對應的 `NavKey` 各自維護獨立 sub back stack，並將所有 Tab 的 sub back stack 攤平為單一 flat list 供 `NavDisplay` 消費）管理：切換到某個 Tab 時 MUST 呼叫 `TopLevelBackStack.addTopLevel()`，保留該 Tab 原本累積的 sub back stack 內容不被清除；不同 Tab 的 sub back stack 彼此互不影響、互不覆蓋。使用者按下返回鍵時 MUST 只影響目前所在 Tab 的 sub back stack（呼叫 `TopLevelBackStack.removeLast()`）：若目前 Tab 的 sub back stack 還有多筆項目，MUST 只移除最後一筆並停留在同一個 Tab；若目前 Tab 的 sub back stack 只剩該 Tab 本身這一筆（已在根畫面），MUST 將該 Tab 從 `TopLevelBackStack` 中移除，並切換到其餘仍存在的 Tab 中最後被加入的一個，直到只剩最後一個 Tab 時才交由系統預設行為結束 `MainActivity`。

#### Scenario: 不存在 classic Navigation Compose 依賴

- **WHEN** 檢查 `androidApp/build.gradle.kts` 與 `MainActivity.kt` 的 import
- **THEN** MUST NOT 出現 `androidx.navigation:navigation-compose` 依賴或 `androidx.navigation.NavHostController`／`androidx.navigation.compose.NavHost`／`androidx.navigation.compose.rememberNavController` 的 import

#### Scenario: 使用 Navigation3 API 建立導覽骨架

- **WHEN** 檢查 `MainActivity.kt` 的導覽相關實作
- **THEN** MUST 使用 `TopLevelBackStack` 建立每個 Tab 各自的 sub back stack，並將其攤平後的 `backStack` 交給 `NavDisplay` 渲染
- **AND** `MainNavItem` 驅動的導覽列點擊事件 MUST 呼叫 `TopLevelBackStack.addTopLevel()`（而非字串路由 `navigate()`）切換畫面

#### Scenario: 導覽列可進入收藏頁

- **WHEN** 使用者點擊底部導覽列的收藏項目
- **THEN** `MainNavItem.COLLECT` 對應的 `CollectKey` MUST 成為 `TopLevelBackStack` 目前的 `topLevelKey`
- **AND** `NavDisplay` 的 entryProvider MUST 回傳渲染 `CollectScreen` 的 collect `NavEntry`，不得回退為 `PlaceholderScreen`

#### Scenario: 導覽列可進入歷史頁

- **WHEN** 使用者點擊底部導覽列的歷史項目
- **THEN** `MainNavItem.HISTORY` 對應的 `HistoryKey` MUST 成為 `TopLevelBackStack` 目前的 `topLevelKey`
- **AND** `NavDisplay` 的 entryProvider MUST 回傳渲染 `HistoryScreen` 的 history `NavEntry`，不得回退為 `PlaceholderScreen`

#### Scenario: 導覽列可進入設定頁

- **WHEN** 使用者點擊底部導覽列的設定項目
- **THEN** `MainNavItem.SETTING` 對應的 `SettingKey` MUST 成為 `TopLevelBackStack` 目前的 `topLevelKey`
- **AND** `NavDisplay` 的 entryProvider MUST 回傳渲染 `SettingScreen` 的 setting `NavEntry`，不得回退為 `PlaceholderScreen`

#### Scenario: 切換 Tab 保留該 Tab 原有的 sub back stack

- **WHEN** 使用者在 History Tab 內瀏覽至某個子路由後，切換到 Home Tab，再切換回 History Tab
- **THEN** `TopLevelBackStack` MUST 仍顯示 History Tab 先前瀏覽到的子路由畫面，MUST NOT 重置回 History Tab 的根畫面

#### Scenario: 不同 Tab 的 back stack 互不影響

- **WHEN** 使用者依序點擊底部導覽列從 Home 切到 Collect、再從 Collect 切到 Search
- **THEN** Home、Collect、Search 三個 Tab 各自的 sub back stack MUST 保持獨立存在於 `TopLevelBackStack` 中，彼此不互相清除或覆蓋內容

#### Scenario: 返回鍵只影響目前 Tab 的 sub back stack

- **WHEN** 目前所在 Tab 的 sub back stack 有多於 1 筆項目，使用者按下返回鍵
- **THEN** `TopLevelBackStack.removeLast()` MUST 只移除該 Tab sub back stack 的最後一筆
- **AND** 畫面 MUST 停留在同一個 Tab

#### Scenario: 在 Tab 根畫面按返回鍵切換到其他仍存在的 Tab

- **WHEN** 目前所在 Tab 的 sub back stack 只剩下該 Tab 本身這一筆（已在根畫面），且 `TopLevelBackStack` 中仍有其他 Tab 存在，使用者按下返回鍵
- **THEN** 該 Tab MUST 從 `TopLevelBackStack` 中移除
- **AND** 畫面 MUST 切換到其餘 Tab 中最後被加入的一個

#### Scenario: 只剩最後一個 Tab 時返回鍵維持既有退出行為

- **WHEN** `TopLevelBackStack` 中只剩下 1 個 Tab，且其 sub back stack 只剩該 Tab 本身這一筆，使用者按下返回鍵
- **THEN** 系統 MUST 交由預設行為結束 `MainActivity`（行為與變更前一致，不受本次修改影響）

### Requirement: `MainViewModel` MUST 透過 Koin 注入

`MainViewModel` MUST 由 Koin module 提供並透過 Koin API 注入到 `MainActivity`，MUST NOT 使用預設 `by viewModels()`（`SavedStateViewModelFactory`）建立。

#### Scenario: Koin module 提供 MainViewModel

- **WHEN** 檢查 `androidApp` 的 Koin module 定義
- **THEN** MUST 存在提供 `MainViewModel`（含其 `GetConfigurationUseCase`／`UserDataRepository` 依賴）的 module
- **AND** 該 module MUST 在 App 啟動流程中被載入（例如 `JetpackMovieApplication.onCreate()` 呼叫 `loadKoinModules`）

#### Scenario: MainActivity 不使用預設 ViewModelProvider Factory 取得 MainViewModel

- **WHEN** 檢查 `MainActivity.kt` 取得 `MainViewModel` 實例的方式
- **THEN** MUST NOT 使用 `by viewModels()`
- **AND** MUST 使用 Koin 提供的注入方式（例如 `koinViewModel()`）取得實例

### Requirement: Android App MUST 支援電影詳情 Navigation3 目的地

`androidApp` MUST 以可序列化的 typed `MovieDetailKey(movieId)` 將電影詳情加入既有 Navigation3 `NavBackStack`，並由 `NavDisplay` entry provider 對應至 `feature:detail` 的 entry。首頁、搜尋、收藏、觀看紀錄與推薦電影的卡片點擊 MUST 導向此目的地。實際掛載在 `NavDisplay` 的 entry provider（`mainEntry()`）MUST 直接處理 `MovieDetailKey`，MUST NOT 只存在於未被任何呼叫端使用的替代 entry provider 中。

#### Scenario: 從既有電影卡開啟 detail

- **WHEN** 使用者點擊首頁、搜尋、收藏或觀看紀錄中的電影卡
- **THEN** 系統 MUST 將包含該電影 id 的 `MovieDetailKey` 加入 back stack
- **AND** `NavDisplay` MUST 顯示對應的 detail entry，而非 `PlaceholderScreen`

#### Scenario: 從推薦電影開啟另一部 detail

- **WHEN** 使用者在 detail 頁點擊推薦電影卡
- **THEN** 系統 MUST 將推薦電影的 `MovieDetailKey` 加入目前 back stack 最後方

#### Scenario: 從巢狀 detail 返回

- **WHEN** 使用者在 detail 頁使用畫面返回按鈕或 Android 系統返回操作
- **THEN** 系統 MUST 只移除 back stack 最後一個 `MovieDetailKey`
- **AND** MUST 回到前一個 detail 或原本的入口頁

#### Scenario: 實際生效的 entry provider 直接處理 MovieDetailKey

- **WHEN** 檢查 `MainActivity.kt` 中實際傳給 `SuccessScreen` 內 `NavDisplay` 的 `entryProvider`
- **THEN** 該 entry provider 的實作（`mainEntry()`）MUST 包含 `MovieDetailKey` 分支並回傳 `feature:detail` 的 entry
- **AND** repo 內 MUST NOT 存在其他處理 `MovieDetailKey` 但未被任何 `NavDisplay` 呼叫的孤立 entry provider（例如未被使用的重複 Composable）

### Requirement: detail 顯示時 MUST 隱藏主 Navigation Suite

當 back stack 最後一個 destination 為 `MovieDetailKey` 時，`MainActivity` MUST 顯示 detail 內容而不包裝 `JMNavigationSuiteScaffold`；其他 root destination MUST 保持既有 Navigation Suite 行為。

#### Scenario: 開啟 detail 時不顯示主導航

- **WHEN** `MovieDetailKey` 為目前 back stack 最後一個 key
- **THEN** 畫面 MUST 不顯示 bottom navigation、navigation rail 或 navigation drawer

#### Scenario: 返回 root destination 後恢復主導航

- **WHEN** 使用者從 detail 返回且最後一個 key 為 Home、Search、Collect 或 History key
- **THEN** 系統 MUST 恢復既有 `JMNavigationSuiteScaffold` 與選取狀態

### Requirement: 切換語言後 `MainActivity` MUST 局部重組畫面並保留原有導覽位置

當使用者透過設定頁變更語言（`userData.languageMode` 發出新值）時，`MainActivity` MUST 同步套用新 Locale（不得延遲到下一次重組之後才套用），並 MUST 用 `key(languageMode)` 只包住畫面內容（不含 `rememberNavBackStack()`），使系統字串（`stringResource`）與 Navigation3 畫面內容正確反映新語言。`MainActivity` MUST NOT 呼叫 `activity.recreate()`，避免語言切換造成 Splash 畫面卡住無法消失；`key(languageMode)` MUST NOT 包住 `rememberNavBackStack()`，避免語言切換強制重置導覽 backstack。

#### Scenario: 語言改變時畫面內容以新語言重組

- **WHEN** `userData.languageMode` 發出與前一次不同的值
- **THEN** `MainActivity` MUST 在畫面內容重組之前，同步套用對應新 Locale 的 `Configuration`
- **AND** MUST 用 `key(languageMode)` 觸發畫面內容（不含 backStack）重新組合，以新語言重新讀取字串資源
- **AND** MUST NOT 呼叫 `activity.recreate()`

#### Scenario: 語言切換後停留在原本的畫面

- **WHEN** 使用者在非首頁的畫面（例如 Setting 頁或 Detail 頁）觸發語言切換
- **THEN** Navigation3 的 backstack MUST 維持語言切換前的內容不被重建
- **AND** 使用者 MUST 停留在切換前所在的畫面，不被導回首頁

### Requirement: 底部導覽列字串資源 MUST 具備繁體中文與英文版本

`androidApp` 的 `res/values/strings.xml`（底部導覽列文字：`app_name`、`nav_home`、`nav_favor`、`nav_history`、`nav_search`、`nav_setting`）MUST 同時提供繁體中文（預設 `values/strings.xml`）與英文（`values-en-rUS/strings.xml`）兩份翻譯，確保切換到英文語系時底部導覽列文字正確顯示英文，而非 fallback 回中文。

#### Scenario: 語言模式為英文時底部導覽列顯示英文

- **WHEN** `languageMode` 為 `LanguageMode.ENGLISH`
- **THEN** 底部導覽列（首頁／收藏／歷史／搜尋／設定）MUST 顯示 `values-en-rUS/strings.xml` 定義的英文文案
