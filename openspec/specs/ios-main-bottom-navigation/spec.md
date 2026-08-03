# ios-main-bottom-navigation Specification

## Purpose

定義 iOS 主畫面的底部導覽列、五個固定 tab 入口，以及第一階段 placeholder
頁面行為。
## Requirements
### Requirement: iOS 主畫面使用原生底部導覽

`iosApp` 的 `MainView` SHALL 使用 SwiftUI 原生 `TabView` 呈現底部導覽列，作為 Splash 成功後的主要畫面入口。

#### Scenario: Splash 成功後顯示主畫面底部導覽
- **WHEN** `SplashView` 載入 configuration 成功並切換到 `MainView`
- **THEN** `MainView` SHALL 顯示 SwiftUI 原生底部導覽列

### Requirement: 底部導覽包含五個固定頁面

iOS 主畫面底部導覽 SHALL 包含五個固定 tab，依序為首頁、收藏、搜尋、歷史、設定。

#### Scenario: 使用者進入 MainView
- **WHEN** 使用者看到 `MainView`
- **THEN** 底部導覽 SHALL 顯示首頁、收藏、搜尋、歷史、設定五個 tab

#### Scenario: 預設選取首頁
- **WHEN** `MainView` 首次顯示
- **THEN** 預設選取的 tab SHALL 為首頁

### Requirement: 每個 tab 顯示對應內容

首頁 tab SHALL 呈現既有首頁電影內容；收藏 tab SHALL 呈現 `ios-movie-collection` 定義的
可用收藏畫面；歷史 tab SHALL 呈現 `ios-movie-history` 定義的可用觀看歷史畫面。搜尋與
設定 tab 在尚未實作前 SHALL 顯示可辨識的 placeholder text。

#### Scenario: 切換到首頁 tab

- **WHEN** 使用者選取首頁 tab
- **THEN** 畫面 SHALL 顯示既有首頁電影內容

#### Scenario: 切換到收藏 tab

- **WHEN** 使用者選取收藏 tab
- **THEN** 畫面 SHALL 顯示 `FavoritesView` 的收藏電影格線或收藏空狀態

#### Scenario: 切換到搜尋 tab

- **WHEN** 使用者選取搜尋 tab
- **THEN** 畫面 SHALL 顯示搜尋 placeholder text

#### Scenario: 切換到歷史 tab

- **WHEN** 使用者選取歷史 tab
- **THEN** 畫面 SHALL 顯示 `HistoryView` 的觀看紀錄格線或歷史空狀態，而非 placeholder text

#### Scenario: 切換到設定 tab

- **WHEN** 使用者選取設定 tab
- **THEN** 畫面 SHALL 顯示設定 placeholder text

### Requirement: 導覽與 placeholder 文案使用 String Catalog

底部導覽 tab 標題與 placeholder text SHALL 透過 `Localizable.xcstrings` 取得，不得以顯示用字面字串寫死在 Swift view 中。

#### Scenario: 系統語言為繁體中文
- **WHEN** 使用者裝置系統語言為 `zh-Hant`
- **THEN** 底部導覽與 placeholder text SHALL 顯示繁體中文文案

#### Scenario: 系統語言為英文
- **WHEN** 使用者裝置系統語言為 `en`
- **THEN** 底部導覽與 placeholder text SHALL 顯示英文文案

### Requirement: 已支援電影卡片的 tab SHALL 提供推入電影詳情頁的導覽

首頁、搜尋、收藏、歷史四個 tab 的根內容 SHALL 各自以 `NavigationStack` 承載，並以 `.navigationDestination(for: Int32.self)` 定義電影詳情頁目的地。既有 `MovieCardView.onMovieTap` callback SHALL 接上對應 tab 的 `NavigationPath`，以電影的 `movieCardId`（`Int32`）推入電影詳情頁；四個 tab SHALL 使用一致的 path element 型別，且此導覽目的地 SHALL 直接掛在各 tab 實際渲染的根 View（`HomeContentView`／`SearchView`／`FavoritesView`／`HistoryView`）上，不得只存在於未被實際渲染路徑使用的 wrapper 或替代 View 中。

#### Scenario: 從首頁電影卡進入詳情頁

- **WHEN** 使用者在首頁 tab 點擊一張電影卡
- **THEN** 首頁的 `NavigationStack` SHALL 推入以該電影 `movieCardId` 建立的電影詳情頁

#### Scenario: 從搜尋結果電影卡進入詳情頁

- **WHEN** 使用者在搜尋 tab 點擊一張電影卡
- **THEN** 搜尋 tab 的 `NavigationStack` SHALL 推入以該電影 `movieCardId` 建立的電影詳情頁

#### Scenario: 從收藏電影卡進入詳情頁

- **WHEN** 使用者在收藏 tab 點擊一張電影卡
- **THEN** 收藏 tab 的 `NavigationStack` SHALL 推入以該電影 `movieCardId` 建立的電影詳情頁

#### Scenario: 從歷史電影卡進入詳情頁

- **WHEN** 使用者在歷史 tab 點擊一張電影卡
- **THEN** 歷史 tab 的 `NavigationStack` SHALL 推入以該電影 `movieCardId` 建立的電影詳情頁

#### Scenario: 進入詳情頁時底部導覽列維持可見

- **WHEN** 任一 tab 推入電影詳情頁
- **THEN** 底部 Tab Bar SHALL 保持可見，不比照 Android 版本隱藏主導覽
- **AND** 返回操作 SHALL 一次僅 pop 一層，不影響其他 tab 各自的 `NavigationStack` 狀態
