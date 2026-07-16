## ADDED Requirements

### Requirement: Ktlint Gradle 驗證任務
專案 MUST 提供可由 Gradle 執行的 `ktlintCheck` 任務，用來檢查 root、`androidApp` 與 `shared` 的 Kotlin source 與 Kotlin Script 格式，且此任務 MUST 不修改 source。

#### Scenario: 執行 ktlintCheck
- **WHEN** 開發者從 repository root 執行 `gradlew.bat ktlintCheck`
- **THEN** Gradle MUST 使用專案宣告的 ktlint CLI dependency 檢查 Kotlin source 與 Kotlin Script
- **THEN** 任務 MUST 排除 build output

#### Scenario: ktlintCheck 不修改檔案
- **WHEN** `ktlintCheck` 發現格式不符合規範的 Kotlin 檔案
- **THEN** 任務 MUST 回報失敗
- **THEN** 任務 MUST NOT 自動改寫該檔案

### Requirement: Ktlint 格式化任務
專案 MUST 提供 `ktlintFormat` 任務，用來在開發者明確要求時格式化 Kotlin source 與 Kotlin Script。

#### Scenario: 手動格式化
- **WHEN** 開發者從 repository root 執行 `gradlew.bat ktlintFormat`
- **THEN** Gradle MUST 使用 ktlint CLI 的 format 模式修正符合掃描範圍的 Kotlin 檔案

#### Scenario: 一般建置不自動格式化
- **WHEN** 開發者執行 Android 或 KMP 的一般 build task
- **THEN** Gradle MUST NOT 因 `preBuild` 或等效 lifecycle task 隱式執行 `ktlintFormat`

### Requirement: Ktlint dependency catalog 管理
專案 MUST 透過 `gradle/libs.versions.toml` 管理 ktlint CLI 版本與 library alias，Gradle build script MUST NOT 直接硬編碼 ktlint artifact 座標與版本。

#### Scenario: ktlint artifact 由 catalog 解析
- **WHEN** Gradle 建立 ktlint configuration
- **THEN** ktlint dependency MUST 使用 `libs` version catalog alias 宣告

### Requirement: Ktlint 納入專案驗證流程
專案 MUST 將 `ktlintCheck` 納入 root `check` 驗證流程，使提交前驗證可以一致執行格式檢查。

#### Scenario: check 執行格式檢查
- **WHEN** 開發者從 repository root 執行 `gradlew.bat check`
- **THEN** Gradle MUST 執行或依賴 `ktlintCheck`
