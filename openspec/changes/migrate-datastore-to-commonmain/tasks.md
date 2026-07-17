## 1. shared/commonMain datastore 遷移

- [ ] 1.1 建立 `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/datastore/`。
- [ ] 1.2 實作含有 `userData: Flow<UserData>` 的 `UserPreferenceDataSource`。
- [ ] 1.3 實作 `ConfigurationBean`、`ThemeMode`、`LanguageMode` setters。
- [ ] 1.4 新增 persisted values 與 `UserData` 的 mapping，並保留 `UserData.getDefault()` 預設值。
- [ ] 1.5 決定並實作 KMP-safe storage format：若 KMP generation 已可用則使用 proto，否則使用 Preferences DataStore。

## 2. shared 平台 datastore 建立

- [ ] 2.1 新增建立/開啟 user preferences DataStore 的 common API。
- [ ] 2.2 新增 Android implementation，使用 app context/file path。
- [ ] 2.3 新增 iOS implementation，使用穩定的 app document/cache path。
- [ ] 2.4 依選定 storage format，維持穩定 file name：`user_preferences` 或 `user_preferences.pb`。

## 3. LanguageProvider 整合

- [ ] 3.1 新增 datastore-backed `LanguageProvider` implementation。
- [ ] 3.2 快取從 `UserPreferenceDataSource.userData` 收集到的 language values。
- [ ] 3.3 將 English map 到 `en-US`、Traditional Chinese map 到 `zh-TW`，system default map 到 system language fallback。
- [ ] 3.4 移除或停止在 production DI 綁定 `DefaultLanguageProvider`。

## 4. Koin DI modules

- [ ] 4.1 建立 `datastoreModule`。
- [ ] 4.2 提供 `UserPreferenceDataSource`。
- [ ] 4.3 將 `LanguageProvider` 綁定到 datastore-backed implementation。
- [ ] 4.4 更新 `JetpackMovieApplication`，同時安裝 datastore 與 network modules。
- [ ] 4.5 確認 `networkModule(isDebug)` 使用 DI 提供的 `LanguageProvider`。

## 5. androidApp 驗證 UI

- [ ] 5.1 新增簡易 button，用於 toggle 或設定語言。
- [ ] 5.2 button click 時，透過 `UserPreferenceDataSource` 持久化選取語言。
- [ ] 5.3 語言更新後觸發 network call。
- [ ] 5.4 顯示精簡 status text，包含選取語言與 network call result/error。
- [ ] 5.5 UI 維持暫時且簡單，不建立完整 settings page。

## 6. Tests

- [ ] 6.1 新增 `UserPreferenceDataSource` 預設 `UserData` 的 common tests。
- [ ] 6.2 新增 theme 與 language persistence mapping 的 common tests。
- [ ] 6.3 新增 datastore-backed `LanguageProvider` language code mapping tests。
- [ ] 6.4 更新 network tests，確認 request `language` 來自 datastore-backed provider。
- [ ] 6.5 新增 Koin test，確認 datastore + network modules 可 resolve `LanguageProvider`、`UserPreferenceDataSource`、`MovieDataSource`。

## 7. Verification

- [ ] 7.1 執行 `.\gradlew.bat :shared:testAndroidHostTest`。
- [ ] 7.2 執行 `.\gradlew.bat :androidApp:assembleDebug`。
- [ ] 7.3 若目前機器環境允許，安裝/啟動 Android app，確認 button 能更新語言並呼叫 network。
- [ ] 7.4 記錄任何環境限制，特別是 Windows 上無法執行 iOS simulator tests。
