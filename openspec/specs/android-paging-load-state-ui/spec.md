# android-paging-load-state-ui Specification

## Purpose

定義 `core/ui` 共用的 Paging LoadState UI 元件（整頁 refresh wrapper 與行內 append/prepend Footer），以及 `feature/home`、`feature/search` 套用這套共用元件後應呈現的 Loading／Error／到底提示行為，取代各模組各自手刻或未被使用的舊有 Paging 狀態顯示邏輯。

## Requirements

### Requirement: `core/ui` MUST 提供整頁 Paging refresh 顯示元件

`core/ui` MUST 提供一個 Composable，依 `LazyPagingItems.loadState.refresh` 判斷並整頁顯示 Loading、Error 或實際內容三者其一：`LoadState.Loading` 時 SHALL 顯示整頁 Loading 畫面（重用既有 `LoadingScreen()`）；`LoadState.Error` 時 SHALL 顯示整頁 Error 畫面並提供重試操作（重用既有 `ErrorScreen(onRetry = ...)`）；其餘狀態 SHALL 顯示呼叫端傳入的實際內容。

#### Scenario: First load 顯示整頁 Loading

- **WHEN** 畫面首次載入 Paging 資料，`loadState.refresh` 為 `LoadState.Loading` 且尚無任何已顯示項目
- **THEN** 元件 MUST 顯示整頁 Loading 畫面，MUST NOT 顯示底層的實際內容（如空的 Grid）

#### Scenario: First load 失敗顯示整頁 Error 並可重試

- **WHEN** `loadState.refresh` 為 `LoadState.Error`
- **THEN** 元件 MUST 顯示整頁 Error 畫面，並提供重試按鈕；使用者觸發重試 SHALL 呼叫呼叫端提供的 `onRetry` callback

#### Scenario: 載入完成後顯示實際內容

- **WHEN** `loadState.refresh` 為 `LoadState.NotLoading`
- **THEN** 元件 MUST 顯示呼叫端傳入的實際內容（例如 Grid 清單）

### Requirement: `core/ui` MUST 提供分頁載入更多的行內 Footer 元件

`core/ui` MUST 提供一個可放入 `LazyGridScope`（或等效 Lazy 容器 scope）`item {}` 的 Footer Composable，接受單一 `LoadState` 參數（由呼叫端傳入 `append` 或 `prepend` 方向的 `LoadState`），依狀態顯示行內 Loading、行內 Error（含重試）、或到底提示，且 MUST NOT 遮蔽或清空已顯示的清單內容。

#### Scenario: 載入更多時顯示行內 Loading，不影響既有內容

- **WHEN** 傳入的 `LoadState` 為 `LoadState.Loading`
- **THEN** Footer MUST 顯示行內 Loading 指示（非整頁遮蔽），已顯示的清單項目 MUST 維持可見

#### Scenario: 載入更多失敗顯示行內 Error 並可重試

- **WHEN** 傳入的 `LoadState` 為 `LoadState.Error`
- **THEN** Footer MUST 顯示行內錯誤提示與重試操作；使用者觸發重試 SHALL 呼叫呼叫端提供的 `onRetry` callback

#### Scenario: 已無更多資料時顯示到底提示

- **WHEN** 傳入的 `LoadState` 為 `LoadState.NotLoading` 且 `endOfPaginationReached` 為 `true`
- **THEN** Footer MUST 顯示「沒有更多資料」提示文字

#### Scenario: 尚有更多資料且未在載入時不顯示任何提示

- **WHEN** 傳入的 `LoadState` 為 `LoadState.NotLoading` 且 `endOfPaginationReached` 為 `false`
- **THEN** Footer MUST NOT 顯示 Loading、Error 或到底提示（維持空白，避免多餘視覺干擾）

### Requirement: `feature/home` 的分類片單清單 MUST 顯示 first-load Loading 與載入更多提示

`feature/home` 的 `HomeScreenPager` MUST 套用整頁 refresh 元件與行內 Footer 元件，使首頁各分類分頁清單在 first load、載入更多、以及對應失敗情境下皆有對應的載入／錯誤提示，MUST NOT 沿用「without loadState 判斷、first load 直接顯示空白 Grid」的既有行為。

#### Scenario: 首頁分類清單 first load 顯示整頁 Loading

- **WHEN** 使用者切換到某個尚未載入過的首頁分類分頁
- **THEN** 該分頁 MUST 顯示整頁 Loading，直到資料載入完成或失敗

#### Scenario: 首頁分類清單捲動到底部觸發載入更多

- **WHEN** 使用者將首頁分類清單捲動至接近底部，觸發 Paging `append` 載入
- **THEN** Grid 尾端 MUST 顯示行內 Loading 提示，既有已顯示的電影卡片 MUST 維持可見與可互動

#### Scenario: 首頁分類清單載入更多失敗可重試

- **WHEN** 首頁分類清單的 `append` 載入失敗
- **THEN** Grid 尾端 MUST 顯示行內錯誤提示與重試操作，觸發重試 SHALL 重新嘗試載入下一頁

### Requirement: `feature/search` 的搜尋結果清單 MUST 改用共用 Paging LoadState 元件且對外行為維持不變

`feature/search` 的 `SearchScreen.kt` MUST 改為呼叫 `core/ui` 提供的整頁 refresh 元件與行內 Footer 元件，取代既有手刻的 `SearchLoadingScreen`／`SearchErrorScreen`／inline `append` 判斷邏輯；MUST NOT 改變搜尋結果的 first-load Loading、Error 重試、載入更多提示與「沒有更多了」文字對使用者呈現的行為。

#### Scenario: 搜尋 first load 行為與改動前一致

- **WHEN** 使用者提交搜尋關鍵字，`movieSearchPager.loadState.refresh` 進入 `LoadState.Loading`
- **THEN** 畫面 MUST 顯示整頁 Loading，行為與改動前的 `SearchLoadingScreen` 一致

#### Scenario: 搜尋結果載入更多與到底提示行為與改動前一致

- **WHEN** 使用者捲動搜尋結果清單並觸發 `append` 載入，或已到達 `endOfPaginationReached`
- **THEN** 畫面 MUST 顯示與改動前相同的行內 Loading／錯誤重試／「沒有更多了」提示，不得有行為退化
