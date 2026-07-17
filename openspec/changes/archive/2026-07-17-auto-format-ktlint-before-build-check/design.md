## Context

目前 root `build.gradle.kts` 已提供 `ktlintCheck` 與 `ktlintFormat`，並把 `ktlintCheck` 接到 root `check` 與 Android/KMP 的 `preBuild` / `androidPreBuild` lifecycle。這會讓手動編譯遇到格式問題時直接被 `ktlintCheck` 擋下，但同一輪 build 不會自動修正格式。

既有 spec 曾刻意要求一般 build 不自動執行 `ktlintFormat`，理由是避免 build 隱式改寫 source。現在使用者明確要求手動編譯時自動格式化以降低開發摩擦，因此需要同步調整 spec 契約與 Gradle task graph。

## Goals / Non-Goals

**Goals:**

- 手動執行 Android/KMP build lifecycle 時，先執行 `ktlintFormat`，再執行 `ktlintCheck`。
- root `check` 也套用相同順序，讓驗證流程不會因可自動修正的格式問題先失敗。
- 保留 `ktlintCheck` 單獨執行時不修改 source 的語意。
- 保留 `ktlintFormat` 單獨執行入口。

**Non-Goals:**

- 不導入 Spotless、Detekt 或 ktlint Gradle plugin。
- 不變更 ktlint CLI 版本與 version catalog 設定。
- 不調整 Kotlin、AGP、KMP source set 或 app 架構。
- 不新增 UI、domain、repository 或 use case 行為。

## Decisions

1. lifecycle task 同時依賴 `ktlintFormat` 與 `ktlintCheck`，並讓 `ktlintCheck` 排在 `ktlintFormat` 後。

   理由：這可滿足「build 時先 format 再 check」，又不需要改變 `ktlintCheck` 單獨執行時不修改 source 的既有語意。替代方案是讓 `ktlintCheck.dependsOn(ktlintFormat)`，但這會使開發者執行 `gradlew.bat ktlintCheck` 時也改寫檔案，違反既有驗證任務契約。

2. root `check` 套用與 build lifecycle 相同的 ktlint 順序。

   理由：目前 `check` 只依賴 `ktlintCheck`。若保留現狀，開發者執行 `gradlew.bat check` 仍會因格式問題先失敗。調整後 `check` 應依賴 `ktlintFormat` 與 `ktlintCheck`，且 `ktlintCheck` 必須在 `ktlintFormat` 後執行。

3. Android/KMP pre-build lifecycle 使用既有 `tasks.matching` 掛點擴充。

   理由：目前專案已透過 root `subprojects { tasks.matching { task.name == "preBuild" || task.name == "androidPreBuild" } }` 接上 ktlint。沿用這個掛點可縮小變更範圍，避免在 `androidApp` 或 `shared` 重複定義 ktlint task。

4. 不調整 MVVM / MVI / Repository / Use Case 模式。

   理由：本 change 只影響 Gradle build tooling 與 openspec 契約，不涉及 runtime 架構或 UI/domain/data 分層。

## Risks / Trade-offs

- [build 會修改 source，可能讓工作目錄在編譯後變髒] -> 這是本需求接受的取捨；tasks 需要求實作者驗證 format 後的 diff 並再執行 check。
- [CI 若使用 `check`，可能在 CI 環境改寫 source 但不提交] -> 若 CI 不希望改寫 source，後續可新增獨立 CI 驗證 task；本次先依使用者要求調整手動編譯與 `check` 的行為。
- [Gradle task ordering 設定錯誤會導致 format/check 並行或 check 先跑] -> 實作時需使用 `mustRunAfter` 或等效 ordering，並透過 `gradlew.bat :androidApp:assembleDebug --dry-run` 或實際 build 驗證順序。
- [Room migration] -> 本 change 不涉及資料庫 schema，不需要 Room migration。

## Migration Plan

1. 調整 root `build.gradle.kts`，讓 `check` 依賴 `ktlintFormat` 與 `ktlintCheck`，並保證 `ktlintCheck` 在 `ktlintFormat` 後執行。
2. 調整 Android/KMP `preBuild` / `androidPreBuild` lifecycle，讓 build 前同時排入 `ktlintFormat` 與 `ktlintCheck`。
3. 執行 dry-run 或實際 Gradle task 驗證順序。
4. 執行 `gradlew.bat ktlintFormat`、`gradlew.bat ktlintCheck` 與主要 build task。
5. 若造成不可接受的自動改檔行為，可回復 lifecycle 對 `ktlintFormat` 的依賴，保留手動 format 入口。

## Open Questions

- CI 是否也會使用 root `check`，且是否接受 `check` 自動改寫 source？目前先依需求將 `check` 與手動 build lifecycle 都改成 format 後 check。
