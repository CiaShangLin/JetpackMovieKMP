## 1. root Gradle ktlint task graph

- [x] 1.1 在 root `build.gradle.kts` 建立 `ktlintCheck` 對 `ktlintFormat` 的排序約束，確保同一個 task graph 中 `ktlintCheck` 於 `ktlintFormat` 後執行
- [x] 1.2 調整 root `check`，讓它依賴 `ktlintFormat` 與 `ktlintCheck`
- [x] 1.3 調整 subprojects 的 `preBuild` / `androidPreBuild` 掛點，讓 Android/KMP build lifecycle 同時依賴 root `ktlintFormat` 與 `ktlintCheck`
- [x] 1.4 確認單獨執行 `gradlew.bat ktlintCheck` 時不會因 task dependency 自動執行 `ktlintFormat`

## 2. openspec contract sync

- [x] 2.1 確認 `openspec/specs/kmp-dependency-catalog/spec.md` 在 archive 後會將一般 build 自動 format 的需求取代舊的「一般建置不自動格式化」契約
- [x] 2.2 確認 root `check` 的 spec 描述為先執行 `ktlintFormat` 再執行 `ktlintCheck`

## 3. validation

- [x] 3.1 執行 `gradlew.bat :androidApp:assembleDebug --dry-run` 或等效 dry-run，確認 `ktlintFormat` 排在 `ktlintCheck` 前
- [x] 3.2 執行 `gradlew.bat check --dry-run`，確認 `check` 的 ktlint task 順序為 `ktlintFormat` 先於 `ktlintCheck`
- [x] 3.3 執行 `gradlew.bat ktlintFormat`
- [x] 3.4 執行 `gradlew.bat ktlintCheck`
- [x] 3.5 執行 `gradlew.bat :androidApp:assembleDebug`，確認手動 build 可觸發 format 後再 check
- [x] 3.6 若 `check` 或 assemble 因非 ktlint 問題失敗，記錄失敗原因與已完成的 ktlint 驗證結果
