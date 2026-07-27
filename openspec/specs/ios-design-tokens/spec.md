# ios-design-tokens Specification

## Purpose

定義 iOS SwiftUI 共用 design tokens 的放置位置、命名規則、平台邊界與 migration 原則，讓 `iosApp` 常用 spacing、padding、radius、size、opacity 與 ratio 能集中管理並避免 magic number 擴散。

## Requirements

### Requirement: iOS SHALL 提供共用 design token 定義

iOS App SHALL 在 `iosApp` 的共用 UI 區域提供 design token 定義，用於集中管理 SwiftUI 常用 layout 數值，避免相同 spacing、padding、radius 或固定尺寸在多個 View 中重複硬編碼。

#### Scenario: 新增共用 token 檔案

- **WHEN** 開發者檢查 iOS 共用 UI 程式碼
- **THEN** 系統 SHALL 在 `iosApp/iosApp/Common/DesignSystem/` 提供 Swift design token 定義檔

#### Scenario: View 使用共用 token

- **WHEN** iOS SwiftUI View 需要使用已定義的常用 spacing、radius 或 size
- **THEN** View SHALL 引用共用 design token，而非直接寫入相同裸數字

### Requirement: iOS design token SHALL 採用數值型 Swift 命名

iOS design token SHALL 使用數值型 camelCase 命名，讓 token 名稱能直接反映原始尺寸值並符合 Swift API 慣例。

#### Scenario: spacing token 命名

- **WHEN** 定義數值為 8 的 spacing token
- **THEN** token SHALL 命名為 `spacing8`

#### Scenario: radius token 命名

- **WHEN** 定義數值為 12 的 radius token
- **THEN** token SHALL 命名為 `radius12`

#### Scenario: size token 命名

- **WHEN** 定義數值為 44 的固定尺寸 token
- **THEN** token SHALL 命名為 `size44` 或具備明確用途的數值型名稱，例如 `movieGridMinWidth`

### Requirement: iOS design token SHALL 保持平台專屬

iOS design token SHALL 定義於 `iosApp`，不得為了 SwiftUI layout 數值而新增 shared Kotlin API 或改動 Android design system。

#### Scenario: 不修改 shared module

- **WHEN** 實作 iOS design token
- **THEN** 系統 SHALL 不新增 shared Kotlin design token model、expect/actual API 或跨平台 UI token contract

#### Scenario: 不修改 Android UI

- **WHEN** 實作 iOS design token
- **THEN** 系統 SHALL 不修改 `androidApp` 或 `core/*` 的 Android UI token / theme

### Requirement: iOS design token migration SHALL 不改變畫面行為

將既有 hard-coded layout value 替換成 design token 時，系統 SHALL 保持替換前後的實際數值一致，不得改變畫面排列、資料流、載入流程或互動行為。

#### Scenario: 替換 spacing 後數值一致

- **WHEN** 將 `padding(16)` 或 `HStack(spacing: 16)` 替換成共用 token
- **THEN** token 的實際數值 SHALL 等於 `16`

#### Scenario: 替換 radius 後數值一致

- **WHEN** 將 `RoundedRectangle(cornerRadius: 12)` 替換成共用 token
- **THEN** token 的實際數值 SHALL 等於 `12`

#### Scenario: 不改動非 layout 行為

- **WHEN** 執行 design token migration
- **THEN** 系統 SHALL 不改動 ViewModel、Repository、UseCase、Koin DI、navigation 或圖片載入邏輯
