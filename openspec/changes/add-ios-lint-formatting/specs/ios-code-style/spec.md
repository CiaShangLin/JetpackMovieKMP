## ADDED Requirements

### Requirement: iOS Swift 自動排版

專案 SHALL 使用 SwiftFormat 作為 `iosApp` Swift 原始碼的主要自動排版工具，並提供從 repository 根目錄執行的格式化入口。

#### Scenario: 開發者格式化 iOS Swift 程式碼

- **WHEN** 開發者或 AI 從 repository 根目錄執行 iOS Swift 格式化指令
- **THEN** 系統 SHALL 只格式化 `iosApp/iosApp` 範圍內的 Swift 原始碼，且不得格式化 Xcode project、asset catalog、build output 或 generated framework

#### Scenario: 工具缺失時格式化失敗

- **WHEN** 開發者或 AI 執行 iOS Swift 格式化指令但環境未安裝 SwiftFormat
- **THEN** 系統 SHALL 以清楚錯誤訊息失敗，並提示需要安裝 `swiftformat`

### Requirement: iOS Swift 格式檢查

專案 SHALL 提供不修改檔案的 iOS Swift 格式檢查入口，使用與自動排版相同的 SwiftFormat 規則。

#### Scenario: Swift 格式符合規則

- **WHEN** 開發者或 AI 從 repository 根目錄執行 iOS Swift 格式檢查指令，且所有 Swift 檔案已符合 SwiftFormat 規則
- **THEN** 系統 SHALL 成功結束且不得修改任何檔案

#### Scenario: Swift 格式不符合規則

- **WHEN** 開發者或 AI 從 repository 根目錄執行 iOS Swift 格式檢查指令，且至少一個 Swift 檔案不符合 SwiftFormat 規則
- **THEN** 系統 SHALL 失敗並回報需要格式化的檔案或位置，且不得修改任何檔案

### Requirement: iOS Swift lint

專案 SHALL 使用 SwiftLint 作為 `iosApp` Swift 原始碼的 lint 工具，檢查格式以外的 Swift style / conventions 問題。

#### Scenario: Swift lint 符合規則

- **WHEN** 開發者或 AI 從 repository 根目錄執行 iOS Swift lint 指令，且所有 Swift 檔案符合 SwiftLint 規則
- **THEN** 系統 SHALL 成功結束且不得修改任何檔案

#### Scenario: Swift lint 發現違規

- **WHEN** 開發者或 AI 從 repository 根目錄執行 iOS Swift lint 指令，且至少一個 Swift 檔案違反 SwiftLint 規則
- **THEN** 系統 SHALL 失敗並回報違規內容，且不得修改任何檔案

#### Scenario: 工具缺失時 lint 失敗

- **WHEN** 開發者或 AI 執行 iOS Swift lint 指令但環境未安裝 SwiftLint
- **THEN** 系統 SHALL 以清楚錯誤訊息失敗，並提示需要安裝 `swiftlint`

### Requirement: iOS code style Gradle 入口

專案 SHALL 提供 root Gradle tasks 作為 iOS Swift format / lint 的標準入口，讓 AI、CI 與開發者可用和 Android ktlint 類似的方式執行檢查。

#### Scenario: 執行聚合檢查

- **WHEN** 開發者或 AI 從 repository 根目錄執行 iOS code style 聚合檢查 task
- **THEN** 系統 SHALL 依序執行 SwiftFormat 格式檢查與 SwiftLint lint，並在任一檢查失敗時讓 task 失敗

#### Scenario: Android-only 檢查不受 iOS 工具影響

- **WHEN** 開發者或 AI 執行既有 `ktlintCheck`、`ktlintFormat` 或 Android build task
- **THEN** 系統 SHALL 不要求 SwiftFormat 或 SwiftLint 必須存在，除非該次執行明確包含 iOS code style task

### Requirement: iOS code style 文件化

專案 SHALL 在文件中記錄 iOS Swift format / lint 指令、工具前提與平台限制。

#### Scenario: 開發者查找常用指令

- **WHEN** 開發者或 AI 閱讀專案常用指令文件
- **THEN** 文件 SHALL 說明如何執行 iOS Swift 格式化、格式檢查、lint 與聚合檢查

#### Scenario: 開發者查找工具前提

- **WHEN** 開發者或 AI 閱讀專案 code style 文件
- **THEN** 文件 SHALL 說明需要安裝 SwiftFormat 與 SwiftLint，並標明 Windows / macOS 環境可能無法具備相同 iOS 驗證能力
