## Context

`DatastoreLanguageProvider` 會將目前的 `LanguageMode` 快取為同步可讀的 TMDB 語言代碼，Ktor `defaultRequest` 於每一次請求取用此值。明確選擇 `TRADITIONAL_CHINESE` 時 mapper 已回傳 `zh-TW`；但 `SYSTEM_DEFAULT` 會委派給 platform `currentSystemLanguageCode()`，Android 與 iOS actual 都只取得 Locale 的 language 欄位，因此繁體中文台灣 Locale 被截斷為 `zh`。

正式 App 已透過 `initKoin()` 安裝 `datastoreModule()` 並以 `provideDefaultLanguageProvider = false` 組裝 network module；`DefaultLanguageProvider` 僅供 network module 獨立使用與其測試。另一方面，`MainActivity` 在 Compose 建立後以 `resources.updateConfiguration()` 更新 Activity 資源，但這不保證 Compose 的 `LocalConfiguration`／`stringResource()` 會收到新的組態並重新取得字串。

## Goals / Non-Goals

**Goals:**

- 系統 Locale 含有地區資訊時，保留並以 TMDB 相容的 BCP 47 語言代碼提供給請求層。
- 確保繁體中文台灣系統設定在 `SYSTEM_DEFAULT` 下送出 `zh-TW`。
- 在 Android 切換 `LanguageMode` 後，使資源與 Compose 文案使用相同 Locale。
- 讓 network module 的 `LanguageProvider` 依賴為必填，並由 datastore-backed provider 統一滿足。
- 維持明確選擇繁體中文、英文及無法讀取系統語言時的既有行為。

**Non-Goals:**

- 不變更設定頁提供的語言選項或新增語言／地區偏好設定。
- 不新增語言／地區偏好設定，也不改動 TMDB endpoint 與 Ktor interceptor。
- 不正規化或強制指定所有未帶地區碼的系統 Locale；平台未提供地區時維持既有語言 fallback 行為。

## Decisions

### 1. 在 platform `SystemLanguage` 保留 Locale 的語言與地區

Android 使用 Locale 的 language tag 表示法；iOS 從目前 Locale 分別讀取 language 與 country，並在 country 存在時以 `language-country` 組成代碼。這能讓 `zh`／`TW` 明確成為 `zh-TW`，同時避免把 iOS locale identifier 的 script 或底線格式直接外洩到 API。

替代方案是把 `zh` 一律映射為 `zh-TW`。此作法會錯誤覆蓋簡體中文或沒有地區資訊的系統設定，且將平台 Locale 解讀規則混入共用 `LanguageMode` mapper，故不採用。

### 2. 測試以 provider 到請求參數的既有路徑為主

擴充 datastore provider 與 mock HTTP 請求測試，覆蓋 `SYSTEM_DEFAULT` 產出的完整代碼與其進入 `language` parameter 的結果。明確選擇繁體中文的既有測試保留，確保此次修正不回歸。

替代方案是只對 platform actual 寫單元測試；但 common test 無法可靠地改寫裝置的實際 Locale，且單獨測試無法證明代碼確實被 Ktor 帶出，故不採用。

### 3. Android 使用官方 application locale 機制驅動 resource 更新

Android 由單一 language-mode side effect 將選擇轉為 Locale，透過官方支援的 application locale API 套用，讓系統處理 Activity configuration change 與 Compose resource 更新；不再直接呼叫 `Resources.updateConfiguration()`。`LanguageProvider` 的 `key` 可繼續用於重建需要重置的 Compose state，但不作為 locale 更新機制。

替代方案是只保留 `updateConfiguration()` 並加強 `key(languageMode)`。這只能重新組合既有 composition，無法保證 `stringResource()` 讀取的 Android configuration 已替換，因此不採用。

### 4. 移除 network module 的預設 provider fallback

`networkModule` 不再建立 `DefaultLanguageProvider`，並移除 `provideDefaultLanguageProvider` 參數；所有組裝點必須明確安裝提供 `LanguageProvider` 的 module。正式 App 沿用現有 `datastoreModule()`；network module 的獨立測試則安裝測試用 provider。

替代方案是保留參數並把預設值改為 `false`。此作法仍允許新呼叫端啟用固定語言，讓 production 設定可被靜默覆寫，故不採用。

## Risks / Trade-offs

- [Risk] iOS Foundation API 在 Kotlin/Native 的 nullability 或屬性名稱可能與預期不同。→ Mitigation：實作時先以 iOS target 編譯驗證，並維持既有 `runCatching` fallback。
- [Risk] 部分系統 Locale 沒有 country。→ Mitigation：只有 country 非空時才加入連字號與地區碼，否則回傳原本的 language；無法取得語言時繼續使用 `zh-TW` 預設值。
- [Risk] Locale tag 大小寫因平台而異。→ Mitigation：依 BCP 47 慣例正規化 language 為小寫、country 為大寫，讓 TMDB 請求與既有預期一致。
- [Risk] application locale 套用可能重建 Activity，導覽或暫存 UI state 若未正確保存可能遺失。→ Mitigation：沿用 Navigation3 state 的既有保存機制，並以 Android UI 測試驗證切換後文案與主要入口正常顯示。
- [Risk] 移除預設 provider 會讓遺漏依賴的獨立組裝立即失敗。→ Mitigation：保留並擴充 Koin 測試，明確註冊測試 provider；此 fail-fast 行為是預期的防呆結果。
