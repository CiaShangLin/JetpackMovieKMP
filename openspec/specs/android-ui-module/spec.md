## ADDED Requirements

### Requirement: Android UI components MUST live in an Android-only core module

專案 MUST 將 Android-only 可重用 UI components 放在 `:core:ui` module，實體路徑 MUST 為 `core/ui`。此 module MUST 使用 Android library 形態承載 Android Compose UI、Android resources、Coil Android wiring 與 Compose preview。

#### Scenario: Gradle settings includes core ui

- **WHEN** 檢查 `settings.gradle.kts`
- **THEN** MUST 包含 `include(":core:ui")`
- **AND** MUST NOT 包含 `include(":core:UI")`
- **AND** MUST NOT 包含 `include(":androidUi")`

#### Scenario: Module directory uses lowercase naming

- **WHEN** 檢查 UI module 實體路徑
- **THEN** MUST 使用 `core/ui`
- **AND** MUST NOT 使用 `core/UI`

### Requirement: Android UI module MUST stay outside shared KMP modules

`:core:ui` MUST NOT 被放入 `shared` 目錄，MUST NOT 由 `shared:*` modules 依賴，MUST NOT 被 iOS framework export。Android app 與 Android-only feature modules MAY depend on it directly.

#### Scenario: shared modules do not depend on Android UI module

- **WHEN** 檢查 `shared/*/build.gradle.kts`
- **THEN** MUST NOT 出現 `projects.core.ui`
- **AND** MUST NOT 出現 `project(":core:ui")`

#### Scenario: Android entry points may depend on UI module

- **WHEN** 檢查 Android-only app 或 feature module 的 `build.gradle.kts`
- **THEN** dependencies MAY 包含 `implementation(projects.core.ui)`

### Requirement: Android UI module namespace MUST use project package

`:core:ui` MUST 使用 KMP 專案 package namespace，避免沿用舊專案 `com.shang.ui` namespace。

#### Scenario: Namespace is project scoped

- **WHEN** 檢查 `core/ui/build.gradle.kts`
- **THEN** `android.namespace` MUST be `com.shang.jetpackmoviekmp.core.ui`

#### Scenario: Source package is project scoped

- **WHEN** 檢查 `core/ui/src/main/kotlin`
- **THEN** Kotlin source MUST use package `com.shang.jetpackmoviekmp.core.ui` or its subpackages
- **AND** MUST NOT use package `com.shang.ui`

### Requirement: Android UI module MUST depend on design system and shared model/common in one direction

`:core:ui` MUST reuse `:core:designsystem` for design system components and MAY depend on `:shared:model` and `:shared:common` for cross-platform data contracts and common providers. `:core:designsystem` and `shared:*` modules MUST NOT depend on `:core:ui`.

#### Scenario: UI module declares allowed dependencies

- **WHEN** 檢查 `core/ui/build.gradle.kts`
- **THEN** dependencies SHOULD 包含 `implementation(projects.core.designsystem)`
- **AND** dependencies MAY 包含 `implementation(projects.shared.model)`
- **AND** dependencies MAY 包含 `implementation(projects.shared.common)`

#### Scenario: Design system does not depend on UI module

- **WHEN** 檢查 `core/designsystem/build.gradle.kts`
- **THEN** MUST NOT 出現 `projects.core.ui`
- **AND** MUST NOT 出現 `project(":core:ui")`

### Requirement: Android UI module MUST NOT introduce Hilt

`:core:ui` MUST NOT introduce Hilt or Dagger as a new DI framework. Any migrated image loading or interceptor wiring MUST use the project's Koin-based composition direction or explicit Android app wiring.

#### Scenario: No Hilt dependency in UI module

- **WHEN** 檢查 `core/ui/build.gradle.kts`
- **THEN** MUST NOT include Hilt or Dagger plugin/dependencies

#### Scenario: Migrated DI avoids Hilt annotations

- **WHEN** 檢查 `core/ui/src/main/kotlin`
- **THEN** MUST NOT contain `@Module`
- **AND** MUST NOT contain `@InstallIn`
- **AND** MUST NOT contain `@HiltViewModel`
