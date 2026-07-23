## ADDED Requirements

### Requirement: SKIE 編譯器 plugin catalog 契約
Catalog（`gradle/libs.versions.toml`）MUST 提供與專案 Kotlin 版本相容的 SKIE
Gradle plugin id 與 version alias；SKIE 為純編譯期 Kotlin/Native compiler plugin，
不需額外的 runtime library alias。

#### Scenario: SKIE 版本集中於 version catalog
- **WHEN** 檢查 `gradle/libs.versions.toml`
- **THEN** 存在對應 SKIE 的 `[versions]` 條目與 `[plugins]` alias，且該版本與專案
  當時使用的 Kotlin 版本相容

#### Scenario: shared/app 透過 alias 套用 SKIE
- **WHEN** `shared/app/build.gradle.kts` 套用 SKIE plugin
- **THEN** 透過 `alias(libs.plugins.<skie-alias>)` 套用，不得在 build script 內
  硬編版本字串
