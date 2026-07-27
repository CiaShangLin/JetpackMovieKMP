## 1. iosApp

- [ ] 1.1 檢查目前工作區狀態與 iOS 相關 diff，確認實作時只碰 design token migration 需要的檔案，不回退既有未提交變更。
- [ ] 1.2 新增 `iosApp/iosApp/Common/DesignSystem/JMDesignTokens.swift`，定義 `JMSpacing`、`JMRadius`、`JMSize`、`JMOpacity`、`JMRatio` 第一批常用數值 token。
- [ ] 1.3 掃描 `iosApp/iosApp` 中 `padding`、`spacing`、`cornerRadius`、`frame`、`aspectRatio`、簡單 opacity 的裸數字，列出可替換與應保留項目。
- [ ] 1.4 替換既有 SwiftUI View 中明確屬於共用 layout token 的數字，保持替換前後實際數值一致。
- [ ] 1.5 確認 migration 不改動 ViewModel、Repository、UseCase、Koin DI、navigation 或圖片載入邏輯。
- [ ] 1.6 執行 `rg` 檢查 token 使用與剩餘 layout magic number，確認剩餘裸數字皆有保留理由。
- [ ] 1.7 執行 iOS Swift 格式 / lint 驗證；若本機缺少 SwiftFormat 或 SwiftLint，需明確記錄無法執行的原因。

## 2. openspec

- [ ] 2.1 檢查 `ios-design-tokens` spec requirement 是否與實作一致。
- [ ] 2.2 執行 openspec 狀態檢查，確認 change 已達 apply-ready 狀態。
