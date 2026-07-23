## Context

`JetpackMovieKMP` 目前在 root `build.gradle.kts` 已有 Kotlin 端格式化與檢查入口：

- `ktlintFormat`
- `ktlintCheck`
- `check` 依賴 ktlint tasks
- Android `preBuild` / `androidPreBuild` 會先執行 ktlint

iOS 端目前是 `iosApp/iosApp` 底下的原生 SwiftUI 程式碼，尚未有 `.swiftformat`、`.swiftlint.yml` 或任何 Swift code style task。這代表 AI 或開發者在修改 Swift 檔案後，沒有一個和 Android 端同等穩定的 repo-root 驗證入口。

本次變更是開發流程與程式碼品質設定，不涉及 MVVM / MVI / Repository / Use Case 架構調整；既有 App 架構不變。

## Goals / Non-Goals

**Goals:**

- 使用 Swift 社群主流工具補齊 iOS Swift 自動排版與 lint。
- 提供 root Gradle tasks，讓 AI、CI 與開發者可以從 repository 根目錄執行 iOS code style 指令。
- 格式化與檢查分離：格式化 task 可修改 Swift 檔案，檢查 task 不修改檔案並在不符合規則時失敗。
- 將指令與工具前提寫入專案文件，降低後續使用成本。

**Non-Goals:**

- 不修改 SwiftUI 畫面、ViewModel、KMP shared module 或 app runtime 行為。
- 不導入 iOS unit test framework。
- 不在第一版強制把 iOS lint 掛入 root `check` 或 Android `preBuild`，避免沒有 Swift 工具的 Windows / Android-only 開發環境被阻塞。
- 不在 Xcode build phase 內自動執行會改檔的 formatter。
- 不處理 Swift generated code、`Shared.framework`、Xcode DerivedData 或 asset catalog。

## Decisions

### 1. 使用 SwiftFormat 負責自動排版

選擇 SwiftFormat 作為 formatter，因為它是 Swift 專案常用的主流 formatter，能直接改寫 Swift 檔案，也支援 `--lint` 模式用相同規則做只讀檢查。這符合本需求「最主要是自動排版」的目標。

替代方案：

- Apple `swift-format`：官方工具，但在一般 iOS app 專案中的既有採用度與規則生態不如 SwiftFormat / SwiftLint 組合直覺；本次優先選擇更常見的工具鏈。
- 只用 SwiftLint `--fix`：SwiftLint 的 autocorrect 可修部分規則，但定位仍是 lint，不適合作為主要 formatter。

### 2. 使用 SwiftLint 負責格式以外的 Swift lint

選擇 SwiftLint 作為 lint 工具，負責 Swift style / conventions、命名、複雜度、強制 unwrap 等格式以外的檢查。SwiftFormat 與 SwiftLint 分工清楚，可避免把 lint 工具當 formatter 使用。

替代方案：

- 只用 SwiftFormat `--lint`：可檢查格式，但無法涵蓋 SwiftLint 擅長的 style / conventions 規則。
- Xcode compiler warning：只能檢查編譯層級問題，無法取代團隊風格規則。

### 3. root Gradle tasks 是標準入口

新增 root tasks 作為穩定入口，命名建議如下：

- `iosFormat`：執行 SwiftFormat 並修改 `iosApp/iosApp` 下的 Swift 檔案。
- `iosFormatCheck`：執行 SwiftFormat `--lint`，只檢查不改檔。
- `iosLint`：執行 SwiftLint lint，只檢查不改檔。
- `iosCodeStyleCheck`：依賴 `iosFormatCheck` 與 `iosLint`，作為 iOS code style 聚合檢查。

這個設計讓 AI 實作 Swift 任務時能直接跑：

```bash
./gradlew iosFormat iosCodeStyleCheck
```

Windows PowerShell 對應：

```powershell
.\gradlew.bat iosFormat iosCodeStyleCheck
```

Gradle tasks 應在工具不存在時給出清楚錯誤訊息，提示安裝 `swiftformat` / `swiftlint`，而不是靜默跳過。

### 4. 第一版不把 iOS tasks 掛進 root `check`

目前專案主要開發環境包含 Windows，且 iOS build / Swift toolchain 常需要 macOS 或額外 CLI 安裝。若第一版直接讓 `check` 依賴 `iosCodeStyleCheck`，會讓 Android / KMP 驗證在沒有 Swift 工具時失敗。

因此第一版先提供顯式 tasks；後續若 CI 環境固定為 macOS 且工具安裝穩定，再評估把 `iosCodeStyleCheck` 加進 CI workflow 或 root `check`。

### 5. 設定檔放在 repository root

設定檔放在 root：

- `.swiftformat`
- `.swiftlint.yml`

規則檔以 `iosApp/iosApp` 為主要檢查範圍，排除：

- `iosApp/iosApp.xcodeproj`
- `iosApp/**/Assets.xcassets`
- `iosApp/**/Preview Content`
- `**/build/**`
- `**/.gradle/**`
- generated framework / DerivedData 類型輸出

root 設定檔可讓 CLI、Gradle task、CI 與 Xcode 使用相同規則。

## Risks / Trade-offs

- [Risk] SwiftFormat 初次套用可能造成大量純格式 diff → Mitigation：實作時先建立規則檔，再以單次 `iosFormat` 統一現有 Swift 檔案，commit 範圍保持聚焦。
- [Risk] SwiftFormat 與 SwiftLint 規則重疊造成衝突 → Mitigation：格式相關規則以 SwiftFormat 為準，SwiftLint 中關閉或放寬會和 formatter 打架的規則。
- [Risk] Windows 環境沒有 SwiftLint / SwiftFormat → Mitigation：Gradle tasks 提供清楚錯誤訊息；文件標明工具安裝前提與 macOS / Windows 限制。
- [Risk] Xcode build phase 自動格式化會改動 working tree → Mitigation：第一版不加入會改檔的 formatter build phase；只保留 CLI / Gradle 主動執行。
- [Risk] 變更不涉及資料庫 schema → Mitigation：不需要 Room migration。

## Migration Plan

1. 新增 SwiftFormat / SwiftLint 設定檔。
2. 新增 root Gradle iOS code style tasks。
3. 執行 `iosFormat` 套用現有 Swift 檔案格式。
4. 執行 `iosCodeStyleCheck` 驗證格式與 lint。
5. 更新 README / AGENTS 常用指令。
6. 移除或標記 `openspec/backlog.md` 中對應待辦為已處理。

Rollback 方式：移除新增設定檔、Gradle tasks 與文件段落，並還原 SwiftFormat 對現有 Swift 檔案造成的格式 diff。

## Open Questions

- 是否要在後續 CI workflow 中加入 `iosCodeStyleCheck`，需等 CI 環境與 Swift 工具安裝方式確認。
- 是否要加入 Xcode SwiftLint warning build phase，第一版可不做，避免讓本次範圍擴大到 Xcode project phase 調整。
