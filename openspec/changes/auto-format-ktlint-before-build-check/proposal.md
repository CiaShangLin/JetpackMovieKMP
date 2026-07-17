## Why

目前手動執行 Android/KMP build 時會先觸發 `ktlintCheck`，但不會自動執行 `ktlintFormat`。當只有格式問題時，開發者必須中斷編譯、手動跑 format、再重新編譯，造成日常開發流程不必要的摩擦。

## What Changes

- 調整 Gradle build lifecycle 中 ktlint 的觸發順序，讓手動編譯會先執行 `ktlintFormat`，再執行 `ktlintCheck`。
- 保留 `ktlintCheck` 作為驗證任務，確保 format 後仍能檢查格式結果。
- 讓 `check` 或 Android/KMP pre-build lifecycle 不再只依賴 `ktlintCheck`，而是透過任務依賴或排序確保 format 先發生。
- 更新既有 ktlint spec，移除「build lifecycle 不得隱式執行 `ktlintFormat`」的舊契約，改為描述手動編譯時的自動格式化行為。

## Capabilities

### New Capabilities

- 無。

### Modified Capabilities

- `kmp-dependency-catalog`: 調整 ktlint format/check 在 Gradle build lifecycle 的要求，讓手動編譯先自動 format 再 check。

## Impact

- 受影響 module：root Gradle build tooling、`androidApp` build lifecycle、`shared` KMP/Android build lifecycle。
- 受影響檔案預期包含 `build.gradle.kts` 與 `openspec/specs/kmp-dependency-catalog/spec.md` 的 ktlint 需求。
- 不新增外部 dependency，不需要修改 `gradle/libs.versions.toml` 或 buildSrc。
