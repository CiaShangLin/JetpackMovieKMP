## Why

iOS SwiftUI 畫面目前在 `padding`、`spacing`、`cornerRadius`、固定高度與 grid 尺寸上直接使用數字，導致常用版面尺寸分散在多個 View 中，後續調整與 review 都需要逐檔比對。

此變更將先建立 iOS 端共用 design tokens，讓常用 UI 數值有一致命名與集中管理位置，降低 magic number 擴散。

## What Changes

- 新增 iOS 共用 design token 定義，集中管理常用 spacing、radius、size、opacity 與 ratio。
- 採用數值型命名，例如 `spacing8`、`radius12`、`size44`，符合目前從 hard-coded number 收斂的需求。
- 第一階段替換既有 SwiftUI layout 中明確屬於版面常數的數字。
- 不改變既有 UI 行為、資料流、圖片載入、navigation 或 ViewModel 職責。
- 不新增第三方依賴。

## Capabilities

### New Capabilities

- `ios-design-tokens`: 定義 iOS SwiftUI 共用 design tokens 的放置位置、命名規則、涵蓋範圍與使用情境。

### Modified Capabilities

- 無。

## Impact

- 受影響 module：`iosApp`
- 主要影響檔案區域：
  - `iosApp/iosApp/Common/DesignSystem/`
  - `iosApp/iosApp/Common/*`
  - `iosApp/iosApp/Home/*`
  - `iosApp/iosApp/Splash/*`
- 不影響 module：
  - `shared/*`
  - `androidApp`
  - `core/*`
- API / ABI：不影響 shared Kotlin framework 對外 API。
- Dependencies：不新增依賴，不修改 Swift Package、Gradle version catalog 或 Xcode package 設定。
