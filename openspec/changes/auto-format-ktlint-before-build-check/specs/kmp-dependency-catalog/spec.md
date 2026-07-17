## MODIFIED Requirements

### Requirement: Ktlint 格式化任務
專案 MUST 提供 `ktlintFormat` 任務，用來格式化 Kotlin source 與 Kotlin Script；此任務 MUST 可由開發者手動執行，且 MUST 在手動 Android/KMP build lifecycle 的 ktlint 檢查前自動執行。

#### Scenario: 手動格式化
- **WHEN** 開發者從 repository root 執行 `gradlew.bat ktlintFormat`
- **THEN** Gradle MUST 使用 ktlint CLI 的 format 模式修正符合掃描範圍的 Kotlin 檔案

#### Scenario: 一般建置先自動格式化
- **WHEN** 開發者執行 Android 或 KMP 的一般 build task，且該 task 會觸發 `preBuild`、`androidPreBuild` 或等效 build lifecycle task
- **THEN** Gradle MUST 在 `ktlintCheck` 前執行或依賴 `ktlintFormat`
- **THEN** 可由 ktlint 自動修正的 Kotlin 格式問題 MUST 在同一輪 build 的 `ktlintCheck` 前被格式化

### Requirement: Ktlint 納入專案驗證流程
專案 MUST 將 `ktlintFormat` 與 `ktlintCheck` 納入 root `check` 驗證流程，使提交前驗證可以先自動格式化，再一致執行格式檢查。

#### Scenario: check 先格式化再檢查
- **WHEN** 開發者從 repository root 執行 `gradlew.bat check`
- **THEN** Gradle MUST 執行或依賴 `ktlintFormat`
- **THEN** Gradle MUST 在 `ktlintFormat` 後執行或依賴 `ktlintCheck`
