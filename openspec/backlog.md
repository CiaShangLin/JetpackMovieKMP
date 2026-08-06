# 開發備忘錄（Backlog）

開發途中發現、留待之後建立新 change 處理的項目，由 /flow-note 維護。

## 評估抽出共用的 Paging Append Footer 元件

- 類型: refactor
- 記錄日期: 2026-07-27
- 來源: add-ios-home-movie-list（實作中發現）
- 前置依賴: 無
- 狀態: 待處理

### 背景

`iosApp/iosApp/Home/page/HomeContentView.swift` 的 `appendFooter`（顯示 Paging append
載入中／失敗重試按鈕的清單尾端元件）目前做成私有的 `@ViewBuilder` computed property，
沒有抽成共用元件。

原因：目前只有 Home 這一個消費者，`FavoritesView`／`HistoryView` 都還是空的 placeholder
畫面（`Text("main_favorite_placeholder")` 等），沒有分頁需求；且 `appendFooter` 目前綁定的
型別是 `HomeMovieListLoadState`（Home 專屬的 Kotlin sealed interface，透過 `onEnum(of:)`
橋接），若之後 Favorites／History 也要做分頁，八成會比照現有模式各自有自己的 LoadState
型別，屆時才能看清楚共用介面該怎麼切（例如改用 `isLoading: Bool`／`hasError: Bool`／
`onRetry: () -> Void` 這種跟具體 Kotlin 型別解耦的參數）。

### 後續調整

等之後真的有第二個分頁畫面（例如 Favorites 或 History 導入分頁）時，再回頭評估是否要把
兩邊共通的 append footer 邏輯抽成共用 View，並設計跟 Kotlin sealed interface 解耦的介面。


## 整理 iOS 資料夾分類與跨平台 i18n 時間設定方式

- 類型: refactor
- 記錄日期: 2026-08-02
- 來源: master
- 前置依賴: 無
- 狀態: 待處理

規劃 iOS 端資料夾分類，並整理 Android 與 iOS 的國際化時間設定方法，確認各平台的 locale、時區與日期時間格式處理方式可一致對應。

## 優化 iOS 語言切換即時刷新的實作方式

- 類型: refactor
- 記錄日期: 2026-08-02
- 來源: add-ios-setting-module（實作中發現）
- 前置依賴: 無
- 狀態: 待處理

目前 `HomeViewModel.swift`／`SearchViewModel.swift` 各自獨立訂閱
`KoinHelper.shared.userDataRepository().userData`，各自維護 `lastLanguageMode`
判斷語言是否變化，變化時分別觸發自己的 refresh（commit 0287cdc）。

討論時曾考慮改成跟 `IosApp.swift` 的 `themeMode` 對稱的寫法：由 root 統一訂閱一次，
透過 SwiftUI `.environment(\.appLanguageMode, ...)` 往下傳遞語言值，各畫面改用
`.onChange(of: languageMode)` 反應並呼叫各自的 refresh 邏輯，藉此去除 Home／Search
兩邊重複的訂閱與 diff 程式碼。使用者當下決定先維持現狀，不在這次 change 內變更。

之後若有相關的 iOS change，可參考這個方向評估是否要重構成 Environment-based 寫法。

## 簡化 iOS ViewModel 收藏操作邏輯，減少重複程式碼

- 類型: refactor
- 記錄日期: 2026-08-03
- 來源: add-ios-movie-detail（實作中發現）
- 前置依賴: 無
- 狀態: 待處理

`MovieRepository` 的收藏相關操作（`insertMovieCollect`／`deleteMovieCollect`／
`getCollectedMovieIds`／`getMovieCollectEntityById`）目前預期會在多個 iOS ViewModel
各自重複實作類似邏輯：`HomeViewModel`／`HomeContentViewModel`、`FavoritesViewModel`、
`SearchViewModel`、`HistoryViewModel`，以及規劃中的 `MovieDetailViewModel`（觀察單一
電影收藏狀態、切換收藏／取消收藏，皆需透過 `onEnum(of:)` 處理 `AppResult`／橋接型別）。

待 `MovieDetailViewModel` 的收藏功能實作完成、重複模式更明確之後，評估抽出共用的
收藏操作邏輯（例如一個共用的 collect helper／service，封裝 toggle 與觀察單一電影收藏
狀態的邏輯），減少各 ViewModel 之間的重複程式碼。

## 討論簡化 iOS 與 Android 端 AppResult 消費方式，看能否減少重複樣板

- 類型: refactor
- 記錄日期: 2026-08-03
- 來源: add-ios-movie-detail（實作中發現）
- 前置依賴: 無
- 狀態: 待處理

目前 iOS 端每個消費 `AppResult<T>` 的地方（`HomeViewModel.loadHome()`、
`MovieDetailViewModel.fetchMovieDetail()` 等）都要重複同一套樣板：
`switch onEnum(of: result) { .success: success.data as? T cast; .failure: switch
onEnum(of: failure.error) { .network / .unknown } }`。Android 端則是
`when (result) { is AppResult.Success -> ...; is AppResult.Failure -> ... }`
搭配各自的 `result.error` 處理，也有類似的重複判斷邏輯散落在多個 ViewModel。

討論中也連帶提到 `DetailSectionState<T>`（Android `feature/detail` 既有的區塊級
Loading/Success/Error 狀態）要不要放進 `shared/common` 讓 iOS 共用泛型型別；當下
結論是先不共用（`shared/common` 定位為跨層共用型別而非畫面呈現狀態，且透過 Kotlin
generic 匯出到 iOS 需要 `onEnum(of:)` 橋接，比原生 Swift generic enum 更麻煩），
iOS 端另外寫原生 Swift enum 因應。

之後有餘裕時，評估是否能在 iOS 端封裝一個共用 helper（例如把 `onEnum(of:)` 兩層
switch 包成一個回傳 `Result<T, AppError>`或類似結構的 extension function），
以及 Android 端是否也有等效的重複判斷可以抽共用 mapper，減少兩平台個別消費
`AppResult` 時的重複樣板程式碼。

## 設定 Android Release 的 GitHub Actions CI/CD 流程

- 類型: feature
- 記錄日期: 2026-08-06
- 來源: master
- 前置依賴: 無
- 狀態: 待處理

需要建立/調整 GitHub Actions workflow，串接 androidApp 現有的 release 簽章與 R8
混淆設定（參考 ce2673e），自動化 Android release 的建置、簽章與發布流程。