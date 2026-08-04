## Why

Android 語言設定變更後，TMDB 內容語言與 App 介面文案無法保證同步；其中跟隨系統的繁體中文 Locale 還會被截斷為 `zh`。同一個使用者操作必須同時更新請求與 Android resources，並移除仍可能覆寫設定的過渡預設 provider。

## What Changes

- 修正 Android 與 iOS 的系統語言代碼取得邏輯，保留 TMDB 所需的語言與地區資訊，例如 `zh-TW`。
- 在 Android 套用使用者選擇的 `LanguageMode` 時，同步更新 App locale，讓 Compose／Android resource 文案隨切換重新取得正確翻譯。
- 移除 `DefaultLanguageProvider` 與 `networkModule` 的 `provideDefaultLanguageProvider` 開關；network module 改為強制使用外部註冊的 `LanguageProvider`。
- 維持明確選擇繁體中文時既有的 `zh-TW` 行為，以及英文與無法取得系統語言時的既有 fallback，並補足各層測試。

## Capabilities

### New Capabilities

無。

### Modified Capabilities

- `kmp-user-preferences-datastore`: 語言偏好設定必須驅動含地區碼的內容語言，並作為 network module 唯一的 production `LanguageProvider` 來源。

## Impact

- `shared:datastore`: 調整 Android／iOS 的 `SystemLanguage` platform actual 實作與 provider 測試。
- `shared:network`: 移除過渡預設 provider 與可選 DI 開關，更新獨立 module 測試。
- `androidApp`: 以 Android locale 組態更新機制同步設定變更與 resource／Compose UI，並補對應測試。
- `shared:data`: 補強 datastore-backed 語言值傳遞到 TMDB 請求的整合測試。
- 不新增依賴；移除 `networkModule` 的可選參數屬於內部 DI API 調整。
