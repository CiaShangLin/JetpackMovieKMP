## Context

目前 `JetpackMovieKMP` 是 Kotlin Multiplatform 專案，Gradle Kotlin DSL 入口包含 root `build.gradle.kts`、`androidApp/build.gradle.kts` 與 `shared/build.gradle.kts`。既有 `openspec/specs/kmp-dependency-catalog/spec.md` 已要求提交前通過 `ktlintCheck`，但專案尚未提供該任務。

參考專案 `JetpackMovieCompose` 的 `buildSrc/src/main/kotlin/plugs/ktlint-settings.gradle.kts` 採用 ktlint CLI：

- 建立 `ktlint` configuration。
- 加入 `com.pinterest:ktlint:0.49.0`。
- 註冊 `ktlintFormat` `JavaExec` task。
- 以 `preBuild.dependsOn("ktlintFormat")` 在 Android build 前自動格式化。

此設定可作為 CLI task 的基底，但本專案需要配合 KMP、version catalog 與可預期的驗證流程調整。

## Goals / Non-Goals

**Goals:**

- 導入 ktlint CLI dependency，並由 `gradle/libs.versions.toml` 集中管理版本與 artifact alias。
- 在 root Gradle 建立可跨 `androidApp` 與 `shared` 使用的 `ktlintCheck` 與 `ktlintFormat`。
- 讓 `ktlintCheck` 納入 `check` 驗證流程，符合既有 commit 前規範。
- 掃描 Kotlin source 與 Kotlin Script，排除 build output。
- 保持 Android 與 KMP module build scripts 只做必要接線，避免重複 task 定義。

**Non-Goals:**

- 不導入 Spotless、Detekt 或 ktlint Gradle plugin。
- 不在一般 `preBuild` 自動執行格式化。
- 不調整 Kotlin/AGP/KSP/Room 版本。
- 不修改 runtime 程式碼、架構模式、資料庫 schema 或 public API。

## Decisions

1. 使用 ktlint CLI task，而不是 ktlint Gradle plugin 或 Spotless。

   理由：舊專案已使用 ktlint CLI，搬遷成本低；目前需求是補齊 `ktlintCheck`，不是建立完整 formatting/lint plugin 架構。Spotless 功能較完整，但會新增另一層 plugin 行為與設定語意，超出本次需求。

2. 將 ktlint dependency 放入 version catalog。

   理由：本專案已把版本集中於 `gradle/libs.versions.toml`，直接在 build script 寫死 `com.pinterest:ktlint:0.49.0` 會偏離既有 catalog 規範。實作時應新增 `ktlint` version 與 `ktlint-cli` library alias，再由 root Gradle configuration 引用。

3. 建立 `ktlintCheck` 與 `ktlintFormat` 兩個任務。

   理由：舊腳本只有 `ktlintFormat` 且會自動修正，無法滿足 CI/commit 前「檢查但不改檔」的需求。`ktlintCheck` 應不帶 `-F`；`ktlintFormat` 才帶 `-F` 供開發者手動修正。

4. `check` 依賴 `ktlintCheck`，但 `preBuild` 不依賴 `ktlintFormat`。

   理由：build task 不應隱式修改 source，否則會讓編譯輸出與工作目錄狀態不可預期。`check` 接上 `ktlintCheck` 可維持驗證契約；需要格式化時由開發者明確執行 `ktlintFormat`。

5. root task 掃描整個 repo，而不是每個 module 重複定義 task。

   理由：目前只有 `androidApp` 與 `shared`，root-level JavaExec 可一次覆蓋 `**/src/**/*.kt`、`**/*.gradle.kts`、`settings.gradle.kts` 等檔案並排除 `**/build/**`。這也避免 Android application 與 KMP library 的 task graph 差異。

6. 不涉及 MVVM / MVI / Repository / Use Case 架構。

   理由：此 change 僅調整 build tooling，不改 UI、domain、data 或 state management。沒有偏離既有 app 架構。

## Risks / Trade-offs

- [ktlint 0.49.0 較舊，可能與 Kotlin 2.4.0 語法支援有落差] -> 實作時先沿用舊專案版本以降低差異；若 `ktlintCheck` 因 CLI 版本不支援目前 Kotlin 語法失敗，需在同一 change 內評估升級 ktlint CLI 版本並記錄原因。
- [root-level glob 可能掃到產生檔或不該檢查的檔案] -> 明確排除 `**/build/**`，必要時追加排除 `.gradle`、generated source 或 iOS build output。
- [把 `ktlintCheck` 接入 `check` 可能讓既有格式問題阻擋完整驗證] -> 提供 `ktlintFormat` 作為修正入口，tasks 中先單獨執行 `ktlintCheck` 再跑 `check`。
- [Windows shell glob 與 ktlint CLI glob 行為可能不同] -> 使用 ktlint CLI 接收 glob pattern，並以 `gradlew.bat ktlintCheck` 驗證 Windows 環境。
- [不使用 Gradle plugin 代表沒有 per-source-set 增量 task] -> 目前專案規模小，root CLI task 足以滿足需求；若未來 module 增長再評估 plugin 化。
- [Room migration] -> 不涉及資料庫 schema，無 Room migration 需求。

## Migration Plan

1. 新增 ktlint version catalog alias。
2. 在 root Gradle 建立 `ktlint` configuration 與 JavaExec tasks。
3. 將 `ktlintCheck` 接到 root `check`。
4. 執行 `gradlew.bat ktlintCheck`，若發現格式問題，執行 `gradlew.bat ktlintFormat` 後重跑。
5. 執行 `gradlew.bat check` 或至少執行主要 Gradle 驗證路徑。
6. 若導入造成建置阻塞，可移除 root Gradle ktlint task 與 catalog alias 回復原狀。

## Open Questions

- 是否必須嚴格沿用舊專案 `com.pinterest:ktlint:0.49.0`，或允許在實作階段因 Kotlin 2.4.0 相容性升級 ktlint CLI 版本？
