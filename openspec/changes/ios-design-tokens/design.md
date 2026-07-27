## Context

`iosApp` 目前多個 SwiftUI View 直接使用 layout 數字，例如 `padding(16)`、`HStack(spacing: 8)`、`RoundedRectangle(cornerRadius: 12)`、`frame(height: 44)` 與 grid minimum width。這些數字目前分散在 `Common`、`Home`、`Splash` 等畫面，缺少 iOS 端集中管理位置。

Android 端可透過 XML resources 或 Compose design system 管理尺寸；iOS SwiftUI 則較適合以 Swift constants 建立輕量 design tokens。此變更只處理 iOS UI 層的 layout token，不改動 shared business logic。

## Goals / Non-Goals

**Goals:**

- 在 `iosApp` 建立 iOS 專用 design token 定義，集中管理常用 layout 數值。
- 採用數值型命名，讓 hard-coded number 替換時可直接對應，例如 `spacing8` 對應 `8`。
- 讓後續 iOS SwiftUI View 優先使用 token，而非新增裸數字。
- 第一階段替換既有 View 中明確屬於共用版面語彙的數字。

**Non-Goals:**

- 不建立跨平台 shared design token API。
- 不調整 Android design system 或 Compose token。
- 不改變 iOS UI 視覺結果、資料流、圖片載入、navigation 或 ViewModel。
- 不新增 Swift Package 或 Gradle 依賴。
- 不要求所有數字都 token 化；資料值、動畫比例、測試 fixture、Preview 專用尺寸可視情境保留。

## Decisions

### Decision: Design tokens 放在 `iosApp/iosApp/Common/DesignSystem`

新增 `JMDesignTokens.swift` 作為第一階段集中入口，定義 `JMSpacing`、`JMRadius`、`JMSize`、`JMOpacity`、`JMRatio`。

替代方案：

- 放在 `shared`：不採用。SwiftUI layout 使用 `CGFloat`，Android Compose 使用 `Dp`，若放進 shared 會把平台 UI 表示細節帶入共用層。
- 每個 View 自己定義 private constants：不採用。雖可局部降低 magic number，但無法形成跨 iOS View 的共用規範。

### Decision: 採用數值型 camelCase 命名

token 命名採 `spacing8`、`radius12`、`size44`，不採 `xs` / `sm` / `md`，也不採 Swift 風格外的 `spacing_8`。

理由：

- 目前需求是把 hard-coded number 收斂，數值型命名最容易 review 與搜尋。
- Swift API 慣例偏好 camelCase，因此使用 `spacing8` 而非 `spacing_8`。
- 未來若設計語意成熟，可再新增 alias，例如 `screenHorizontalPadding = spacing16`，但第一階段不先引入語意命名。

### Decision: 第一階段只替換明確 layout token

替換範圍包含：

- `padding` / `spacing`
- `cornerRadius`
- 固定 UI size，例如 tab 高度、icon size、border width、shadow radius
- grid minimum width
- poster aspect ratio
- 簡單 opacity 常數

不替換範圍包含：

- 與資料或 business rule 相關的數字
- animation duration / scale 等互動調校數字，除非後續明確納入 animation token
- Preview-only 尺寸，除非它與 production layout 共用

### Decision: 不影響既有 MVVM / Repository / UseCase 模式

此變更只整理 SwiftUI View layer 的 UI constants，不改動 ViewModel、Repository、UseCase 或 Koin DI。既有架構模式維持不變。

## Risks / Trade-offs

- [Risk] 過度 token 化會讓程式碼可讀性下降。 → Mitigation：第一階段只處理重複出現且屬於共用 layout 語彙的數字，保留明確有局部語意的數字。
- [Risk] 數值型命名語意較弱。 → Mitigation：先以 `spacing8` 收斂 magic number；未來設計系統成熟後再加語意 alias。
- [Risk] 替換時可能誤改使用者工作區既有未提交變更。 → Mitigation：實作前先檢查 `git status --short` 與相關檔案 diff，只在既有內容上做最小替換，不回退無關變更。
- [Risk] SwiftFormat / SwiftLint 在本機缺工具時無法驗證。 → Mitigation：若工具缺失，明確回報無法執行；至少以檔案檢查與 targeted diff 確認替換範圍。

## Migration Plan

1. 新增 `iosApp/iosApp/Common/DesignSystem/JMDesignTokens.swift`。
2. 定義第一批 `JMSpacing`、`JMRadius`、`JMSize`、`JMOpacity`、`JMRatio`。
3. 掃描 `iosApp/iosApp` 中 `padding`、`spacing`、`cornerRadius`、`frame`、`aspectRatio` 的裸數字。
4. 只替換明確屬於共用 layout token 的數字。
5. 執行 Swift format / lint 檢查；若本機工具缺失則記錄限制。
6. 若需要 rollback，移除 `JMDesignTokens.swift` 並還原本 change 修改過的 SwiftUI token 引用即可；不涉及資料庫 schema 或 migration。

## Open Questions

- 是否要在第一階段納入 animation token，例如 splash 動畫 duration 與 scale。
- 是否要在後續新增語意 alias，例如 `screenHorizontalPadding`、`cardCornerRadius`。
