## 1. shared:datastore 平台語言代碼

- [ ] 1.1 調整 Android `SystemLanguage` actual，從目前 Locale 取得含可用地區碼的 BCP 47 語言代碼。
- [ ] 1.2 調整 iOS `SystemLanguage` actual，組合目前 Locale 的語言與可用 country code，產出與 Android 一致的 BCP 47 格式。
- [ ] 1.3 保留 `DatastoreLanguageProvider` 對無法取得系統語言的 `zh-TW` fallback，並補齊 SYSTEM_DEFAULT 的 provider 測試覆蓋。

## 2. shared:network DI 契約

- [ ] 2.1 移除 `DefaultLanguageProvider` 與 `networkModule` 的 `provideDefaultLanguageProvider` 參數，使 `LanguageProvider` 成為外部必填依賴。
- [ ] 2.2 更新 network module 的 Koin 測試，明確註冊測試用 `LanguageProvider`，並驗證缺少該依賴時解析失敗。
- [ ] 2.3 更新各模組的 Koin 組裝與規格相符，確認正式 App 仍由 `DatastoreLanguageProvider` 提供語言。

## 3. androidApp 語言介面同步

- [ ] 3.1 以 Android 官方 application locale 機制取代直接更新 `Resources` 的作法，讓 `LanguageMode` 切換驅動 configuration 與 Compose resources 更新。
- [ ] 3.2 補 Android UI／Activity 測試，驗證切換繁體中文與英文後 `stringResource()` 文案會同步更新。

## 4. shared:data 網路請求驗證

- [ ] 4.1 擴充 datastore-backed language request 測試，驗證系統繁體中文台灣代碼會送入 TMDB `language=zh-TW`。

## 5. 驗證

- [ ] 5.1 執行 `./gradlew :shared:datastore:testAndroidHostTest :shared:network:testAndroidHostTest :shared:data:testAndroidHostTest`。
- [ ] 5.2 執行 Android 語言切換的 UI／Activity 測試與 `./gradlew :androidApp:assembleDebug`。
- [ ] 5.3 執行 `./gradlew :shared:datastore:iosSimulatorArm64Test`，確認 iOS platform actual 可編譯並通過測試。
