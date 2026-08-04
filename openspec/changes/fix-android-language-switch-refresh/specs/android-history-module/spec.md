## ADDED Requirements

### Requirement: 歷史頁字串資源 MUST 具備繁體中文與英文版本

`feature:history` 的 `res/values/strings.xml` MUST 同時提供繁體中文（預設）與英文（`values-en-rUS/strings.xml`）兩份翻譯，確保切換到英文語系時歷史頁文字正確顯示英文，而非 fallback 回中文。

#### Scenario: 語言模式為英文時歷史頁顯示英文

- **WHEN** `languageMode` 為 `LanguageMode.ENGLISH`
- **THEN** 歷史頁 MUST 顯示 `values-en-rUS/strings.xml` 定義的英文文案
