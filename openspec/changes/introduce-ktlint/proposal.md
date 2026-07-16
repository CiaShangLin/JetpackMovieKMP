## Why

目前專案規範已要求 commit 前通過 `ktlintCheck`，但 Gradle 尚未提供 ktlint 任務，導致格式驗證契約無法執行。這次導入 ktlint 會補齊 KMP 與 Android Gradle 的一致格式檢查入口，並沿用既有 Android 專案可行的設定。

## What Changes

- 從 `JetpackMovieCompose` 的 `buildSrc/src/main/kotlin/plugs/ktlint-settings.gradle.kts` 研究可複用的 ktlint CLI task 設定。
- 在目前 KMP 專案建立 ktlint Gradle 設定，提供至少 `ktlintCheck` 與 `ktlintFormat` 任務。
- 讓 ktlint 覆蓋 root、`androidApp`、`shared` 的 Kotlin 與 Kotlin Script 檔案，並排除 build output。
- 將 `ktlintCheck` 接入 Gradle 驗證流程，使 `check` 或專案驗證命令可以執行格式檢查。
- 保留格式化任務為手動執行，不讓一般 build 隱式修改 source。

## Capabilities

### New Capabilities

- 無

### Modified Capabilities

- `kmp-dependency-catalog`: 補齊 ktlint 驗證任務作為專案 build tooling 與提交前驗證契約。

## Impact

- Affected modules: root Gradle build, `androidApp`, `shared`。
- Affected files likely include:
  - `build.gradle.kts`
  - `androidApp/build.gradle.kts`
  - `shared/build.gradle.kts`
  - `gradle/libs.versions.toml`
  - optional copied/adapted convention script path if the project adopts one, such as `buildSrc/src/main/kotlin/plugs/ktlint-settings.gradle.kts`
- 新增 dependency: ktlint CLI artifact。舊專案使用 `com.pinterest:ktlint:0.49.0`；本專案需評估是否沿用，或放入 version catalog 管理。
- 不影響 app runtime API、KMP public API、database schema 或 iOS app entrypoint。
