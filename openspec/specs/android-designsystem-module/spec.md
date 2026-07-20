# android-designsystem-module Specification

## Purpose
定義 Android-only design system module 的位置、命名、平台邊界與依賴方向，確保 Android Compose UI 基礎層可被 Android app 重用，同時不污染 `shared:*` KMP modules 與 iOS framework export。

## Requirements

### Requirement: Android design system MUST live in an Android-only core module
專案 MUST 將 Android-only design system 放在 `:core:designsystem` module，實體路徑 MUST 為 `core/designsystem`。此 module MUST 使用 Android library 形態承載 Android Compose UI、theme 與 Android resources。

#### Scenario: Gradle settings includes core designsystem
- **WHEN** 檢查 `settings.gradle.kts`
- **THEN** MUST 包含 `include(":core:designsystem")`
- **AND** MUST NOT 包含 `include(":core:designSystem")`
- **AND** MUST NOT 包含 `include(":androidDesignSystem")`

#### Scenario: Module directory uses lowercase naming
- **WHEN** 檢查 design system module 實體路徑
- **THEN** MUST 使用 `core/designsystem`
- **AND** MUST NOT 使用 `core/designSystem`

### Requirement: Android design system MUST stay outside shared KMP modules
`:core:designsystem` MUST NOT 被放入 `shared` 目錄，MUST NOT 由 `shared:*` modules 依賴，MUST NOT 被 iOS framework export。Android app MAY depend on it directly。

#### Scenario: shared modules do not depend on Android design system
- **WHEN** 檢查 `shared/*/build.gradle.kts`
- **THEN** MUST NOT 出現 `projects.core.designsystem`
- **AND** MUST NOT 出現 `project(":core:designsystem")`

#### Scenario: androidApp depends on design system
- **WHEN** 檢查 `androidApp/build.gradle.kts`
- **THEN** dependencies SHOULD 包含 `implementation(projects.core.designsystem)`

### Requirement: Android design system namespace MUST use project package
`:core:designsystem` MUST 使用 KMP 專案 package namespace，避免沿用舊專案 `com.shang.designsystem` namespace。

#### Scenario: Namespace is project scoped
- **WHEN** 檢查 `core/designsystem/build.gradle.kts`
- **THEN** `android.namespace` MUST be `com.shang.jetpackmoviekmp.core.designsystem`
