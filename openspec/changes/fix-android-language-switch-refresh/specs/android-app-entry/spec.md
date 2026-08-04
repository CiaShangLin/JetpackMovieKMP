## ADDED Requirements

### Requirement: 切換語言後 `MainActivity` MUST 重建畫面並保留原有導覽位置

當使用者透過設定頁變更語言（`userData.languageMode` 發出新值）時，`MainActivity` MUST 在套用新 Locale 後呼叫 `activity.recreate()` 重建整個 Activity，以確保系統字串（`stringResource`）與 Navigation3 畫面內容正確反映新語言。`MainActivity` MUST NOT 使用 `key(languageMode)` 包裝 `rememberNavBackStack()`，避免語言切換強制重置導覽 backstack；重建後 MUST 保留使用者原本所在的畫面與導覽層級。

#### Scenario: 語言改變時整個畫面以新語言重建

- **WHEN** `userData.languageMode` 發出與前一次不同的值
- **THEN** `MainActivity` MUST 套用對應新 Locale 的 `Configuration`
- **AND** MUST 呼叫 `activity.recreate()` 重建 Activity

#### Scenario: 重建後停留在原本的畫面

- **WHEN** 使用者在非首頁的畫面（例如 Setting 頁或 Detail 頁）觸發語言切換
- **THEN** `activity.recreate()` 完成後，Navigation3 的 backstack MUST 還原成語言切換前的內容
- **AND** 使用者 MUST 停留在切換前所在的畫面，不被導回首頁

### Requirement: 底部導覽列字串資源 MUST 具備繁體中文與英文版本

`androidApp` 的 `res/values/strings.xml`（底部導覽列文字：`app_name`、`nav_home`、`nav_favor`、`nav_history`、`nav_search`、`nav_setting`）MUST 同時提供繁體中文（預設 `values/strings.xml`）與英文（`values-en-rUS/strings.xml`）兩份翻譯，確保切換到英文語系時底部導覽列文字正確顯示英文，而非 fallback 回中文。

#### Scenario: 語言模式為英文時底部導覽列顯示英文

- **WHEN** `languageMode` 為 `LanguageMode.ENGLISH`
- **THEN** 底部導覽列（首頁／收藏／歷史／搜尋／設定）MUST 顯示 `values-en-rUS/strings.xml` 定義的英文文案
