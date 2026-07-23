## Why

目前專案已有 Kotlin / Android 端的 `ktlintCheck` 與 `ktlintFormat`，但 iOS Swift 程式碼尚未建立對應的 lint 與自動排版流程。隨著 `iosApp` 開始補齊 SwiftUI 實作，需要用主流工具固定 Swift 格式與風格，並提供 AI 與開發者都能從 repository 根目錄執行的驗證入口。

## What Changes

- 導入 SwiftFormat 作為 iOS Swift 自動排版工具，提供可改檔的格式化指令與不改檔的格式檢查指令。
- 導入 SwiftLint 作為 iOS Swift lint 工具，負責格式以外的 Swift style / conventions 檢查。
- 在 root Gradle build configuration 新增 iOS 相關 wrapper tasks，讓 AI 實作與本機開發都能像 `ktlintFormat` / `ktlintCheck` 一樣執行固定入口。
- 新增 SwiftFormat 與 SwiftLint 設定檔，限制檢查範圍以 `iosApp/iosApp` Swift 原始碼為主，排除 Xcode project、asset、build output 與 generated framework。
- 更新專案文件，補上 iOS format / lint 常用指令、工具安裝前提與 Windows / macOS 驗證限制。
- 不在 Xcode build phase 中自動執行會改檔的 formatter，避免一般 build 靜默修改 working tree；是否加入只讀 lint warning build phase 留作實作時評估。

## Capabilities

### New Capabilities

- `ios-code-style`: 定義 iOS Swift 程式碼必須具備可自動排版與可 lint 驗證的流程，並提供 root Gradle task 作為標準入口。

### Modified Capabilities

- 無。

## Impact

- 受影響 module / 區域：
  - root Gradle build configuration（`build.gradle.kts`）：新增 iOS format / lint wrapper tasks。
  - `iosApp`：新增 SwiftFormat / SwiftLint 規則檔作用於 `iosApp/iosApp/**/*.swift`。
  - 專案文件（`README.md`、`AGENTS.md` 視需要）：補上 iOS lint / format 指令。
  - `openspec/backlog.md`：完成或移除既有「iOS 端 lint 與自動排版流程」待辦。
- 新增工具依賴：
  - SwiftFormat CLI。
  - SwiftLint CLI。
- 不涉及 `buildSrc`；本專案目前以 `gradle/libs.versions.toml` 管理 Gradle 依賴，SwiftFormat / SwiftLint 以外部 CLI 方式整合，不新增 Gradle dependency catalog 條目。
- 不影響 app runtime 行為、TMDB API、KMP shared module public API、資料庫 schema 或 iOS framework 匯出設定。
