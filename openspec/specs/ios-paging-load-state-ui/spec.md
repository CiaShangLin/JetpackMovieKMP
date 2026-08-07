# ios-paging-load-state-ui

## Purpose

定義 iOS 端分頁清單（Home、Search）append LoadState 的共用型別與行內 Footer UI，避免各畫面各自重複實作 loading／error／retry 呈現邏輯。

## Requirements

### Requirement: iOS `Common/Paging` MUST 提供共用的 Append LoadState 轉接型別

`iosApp` `Common/Paging` MUST 提供一個與 shared 層 Kotlin 型別無關的純 Swift `AppendLoadState`（`idle` / `loading` / `error(message: String)` 三態），供任一分頁清單畫面將其各自的 shared 層 append LoadState（例如 `HomeMovieListLoadState`、`SearchMovieListLoadState`）映射後使用；MUST NOT 要求呼叫端變更既有 shared 層 Presenter 的 public API。

#### Scenario: Home 將自身 LoadState 映射為共用型別

- **WHEN** `HomeContentView` 的 `HomeMovieListLoadState` 為 `Idle`／`Loading`／`Error(message)`
- **THEN** 對應映射為共用 `AppendLoadState` 的 `idle`／`loading`／`error(message:)`，訊息內容不遺失

#### Scenario: Search 將自身 LoadState 映射為共用型別

- **WHEN** `SearchView` 的 `SearchMovieListLoadState` 為 `Idle`／`Loading`／`Error(message)`
- **THEN** 對應映射為共用 `AppendLoadState` 的 `idle`／`loading`／`error(message:)`，訊息內容不遺失

### Requirement: iOS `Common/Paging` MUST 提供共用的行內 Append Footer View

`iosApp` `Common/Paging` MUST 提供一個 SwiftUI View（`PagingAppendFooterView`），接受單一 `AppendLoadState` 與一個 `onRetry` callback，依狀態顯示空白、行內 Loading、或行內 Error 含重試按鈕，MUST NOT 遮蔽或清空清單中已顯示的項目。

#### Scenario: idle 狀態不顯示任何提示

- **WHEN** 傳入的 `AppendLoadState` 為 `idle`
- **THEN** Footer MUST 不顯示任何內容（`EmptyView`），既有清單項目維持可見

#### Scenario: loading 狀態顯示行內載入指示

- **WHEN** 傳入的 `AppendLoadState` 為 `loading`
- **THEN** Footer MUST 顯示行內 `ProgressView` 載入指示，既有清單項目維持可見與可互動

#### Scenario: error 狀態顯示行內錯誤與可重試按鈕

- **WHEN** 傳入的 `AppendLoadState` 為 `error(message:)`
- **THEN** Footer MUST 顯示可點擊的重試按鈕；使用者點擊時 SHALL 呼叫呼叫端傳入的 `onRetry` callback，既有清單項目維持可見

### Requirement: Home 與 Search 的 append Footer MUST 改用共用元件且對外行為維持不變

`HomeContentView.swift` 與 `SearchView.swift` MUST 移除各自本地重複的 `appendFooter` 渲染邏輯，改為呼叫共用的 `PagingAppendFooterView`；MUST NOT 改變 loading／error／retry 的觸發時機與判斷邏輯（沿用既有 `onAppear { prefetch(index:) }` 與 Paging 3 `prefetchDistance` 機制、既有 `retry()` 呼叫對象）。

#### Scenario: Home 載入更多失敗後重試行為不變

- **WHEN** Home 的 append 載入失敗，使用者點擊共用 Footer 的重試按鈕
- **THEN** 系統 SHALL 呼叫 `HomeContentViewModel.retry()`，行為與改動前一致

#### Scenario: Search 載入更多失敗後重試行為不變

- **WHEN** Search 的 append 載入失敗，使用者點擊共用 Footer 的重試按鈕
- **THEN** 系統 SHALL 呼叫 `SearchViewModel.retry()`，行為與改動前一致

### Requirement: Append Footer 的重試按鈕 MUST 使用中性命名的共用 localization key

Home 與 Search 的 append Footer 重試按鈕 MUST 共用同一個中性命名的 localization key（`paging_append_retry_button`），MUST NOT 讓 Search 端繼續借用語意限定於首頁的既有 key；顯示給使用者的文字內容 MUST 與改動前一致。

#### Scenario: 兩畫面重試按鈕文字一致且不語意錯位

- **WHEN** Home 或 Search 顯示 append 錯誤的重試按鈕
- **THEN** 按鈕文字皆讀取 `paging_append_retry_button`，且顯示文字與改動前相同
