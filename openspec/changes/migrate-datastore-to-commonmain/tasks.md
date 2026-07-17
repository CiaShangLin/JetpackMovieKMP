## 1. shared/commonMain datastore

- [ ] 1.1 Create `shared/src/commonMain/kotlin/com/shang/jetpackmoviekmp/datastore/`.
- [ ] 1.2 Implement `UserPreferenceDataSource` with `userData: Flow<UserData>`.
- [ ] 1.3 Implement setters for `ConfigurationBean`, `ThemeMode`, and `LanguageMode`.
- [ ] 1.4 Add mapping between persisted values and `UserData`, preserving defaults from `UserData.getDefault()`.
- [ ] 1.5 Decide and implement KMP-safe storage format: proto if KMP generation is already available, otherwise Preferences DataStore.

## 2. shared platform datastore creation

- [ ] 2.1 Add common API for creating/opening the user preferences DataStore.
- [ ] 2.2 Add Android implementation using app context/file path.
- [ ] 2.3 Add iOS implementation using a stable app document/cache path.
- [ ] 2.4 Keep file name stable as `user_preferences` or `user_preferences.pb` depending on chosen storage format.

## 3. LanguageProvider integration

- [ ] 3.1 Add datastore-backed `LanguageProvider` implementation.
- [ ] 3.2 Cache language values collected from `UserPreferenceDataSource.userData`.
- [ ] 3.3 Map English to `en-US`, Traditional Chinese to `zh-TW`, and system default to system language fallback.
- [ ] 3.4 Remove or stop binding `DefaultLanguageProvider` in production DI.

## 4. Koin DI modules

- [ ] 4.1 Create `datastoreModule`.
- [ ] 4.2 Provide `UserPreferenceDataSource`.
- [ ] 4.3 Bind `LanguageProvider` to the datastore-backed implementation.
- [ ] 4.4 Update `JetpackMovieApplication` to install datastore and network modules.
- [ ] 4.5 Ensure `networkModule(isDebug)` consumes the DI-provided `LanguageProvider`.

## 5. androidApp verification UI

- [ ] 5.1 Add a simple button to toggle or set language.
- [ ] 5.2 On button click, persist the selected language through `UserPreferenceDataSource`.
- [ ] 5.3 Trigger a network call after the language update.
- [ ] 5.4 Display compact status text showing selected language and network call result/error.
- [ ] 5.5 Keep the UI temporary/simple and avoid building a full settings page.

## 6. Tests

- [ ] 6.1 Add common tests for `UserPreferenceDataSource` default `UserData`.
- [ ] 6.2 Add common tests for theme and language persistence mapping.
- [ ] 6.3 Add tests for datastore-backed `LanguageProvider` language code mapping.
- [ ] 6.4 Update network tests so request `language` comes from datastore-backed provider.
- [ ] 6.5 Add Koin test verifying datastore + network modules resolve `LanguageProvider`, `UserPreferenceDataSource`, and `MovieDataSource`.

## 7. Verification

- [ ] 7.1 Run `.\gradlew.bat :shared:testAndroidHostTest`.
- [ ] 7.2 Run `.\gradlew.bat :androidApp:assembleDebug`.
- [ ] 7.3 If feasible on the current machine, install/run Android app and verify the button can update language and call network.
- [ ] 7.4 Document any environment limitation, especially iOS simulator tests on Windows.
