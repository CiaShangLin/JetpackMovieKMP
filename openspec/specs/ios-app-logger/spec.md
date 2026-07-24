# ios-app-logger Specification

## Purpose
定義 iOS 端統一 App Log 機制：可替換注入的 `AppLogger` protocol、依 `isDebug`
控制輸出範圍的預設實作，以及與既有網路 log 開關共用同一份 `isDebug` 判斷來源。

## Requirements

### Requirement: 可替換注入的 AppLogger protocol

`iosApp` MUST 提供一個 `AppLogger` protocol，定義 debug/info/warning/error 四種 log level 的方法，讓呼叫端可以在不修改呼叫程式碼的前提下替換不同實作。

#### Scenario: 呼叫端透過 protocol 輸出 log

- **WHEN** 程式碼透過 `AppLogger` protocol 呼叫 `debug`/`info`/`warning`/`error` 其中一個方法並帶入訊息與 category
- **THEN** 目前注入的實作 MUST 收到該訊息與 category，並依自身邏輯決定是否輸出

#### Scenario: 替換為不同的 Logger 實作

- **WHEN** 呼叫端將全域注入點（`AppLog.logger`）指定為另一個符合 `AppLogger` protocol 的實作
- **THEN** 後續所有透過 `AppLog.logger` 發出的 log 呼叫 MUST 改由新指定的實作處理，不需修改任何呼叫端程式碼

### Requirement: 預設 Logger 實作依 isDebug 控制輸出層級

`iosApp` MUST 提供一個預設的 `AppLogger` 實作（`OSAppLogger`），包裝系統原生 `os.Logger`，並依 `isDebug` 欄位決定輸出行為：`isDebug == true` 時輸出全部四種 level；`isDebug == false` 時僅輸出 warning 與 error，不得輸出 debug 與 info 內容。

#### Scenario: isDebug 為 true 時輸出全部 level

- **WHEN** `OSAppLogger` 的 `isDebug` 為 `true`，且呼叫任一 `debug`/`info`/`warning`/`error` 方法
- **THEN** 對應層級的訊息 MUST 透過 `os.Logger` 輸出

#### Scenario: isDebug 為 false 時抑制 debug 與 info

- **WHEN** `OSAppLogger` 的 `isDebug` 為 `false`，且呼叫 `debug` 或 `info` 方法
- **THEN** 系統 MUST NOT 透過 `os.Logger` 輸出該訊息

#### Scenario: isDebug 為 false 時仍輸出 warning 與 error

- **WHEN** `OSAppLogger` 的 `isDebug` 為 `false`，且呼叫 `warning` 或 `error` 方法
- **THEN** 對應訊息 MUST 透過 `os.Logger` 輸出

### Requirement: isDebug 判斷邏輯與既有網路 log 開關共用同一來源

`iosApp` 用來初始化 `OSAppLogger` 的 `isDebug` 值，MUST 與 `IosApp.init()` 傳給 `doInitKoinIos(isDebug:)` 的判斷邏輯共用同一個判斷函式（依 `#if DEBUG` 編譯旗標，並允許以 `JM_DEBUG_NETWORK` 環境變數覆寫），不得各自維護一份獨立、可能不同步的判斷邏輯。

#### Scenario: 兩處消費同一個 isDebug 判斷結果

- **WHEN** app 啟動時分別初始化 Koin（`doInitKoinIos(isDebug:)`）與預設 `AppLogger`（`OSAppLogger`）
- **THEN** 兩者使用的 `isDebug` 值 MUST 來自同一個共用判斷函式，在相同執行環境下必為相同結果

#### Scenario: 環境變數覆寫同時影響兩者

- **WHEN** 執行環境設定 `JM_DEBUG_NETWORK` 環境變數為可辨識的布林字串（如 `"true"`／`"false"`）
- **THEN** 該覆寫結果 MUST 同時反映在 Koin 初始化的 `isDebug` 與 `OSAppLogger` 的 `isDebug` 上
