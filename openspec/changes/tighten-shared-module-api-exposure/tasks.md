## 1. 盤點 public ABI

- [ ] 1.1 檢查 `shared:data` public class/function/interface 簽名，確認哪些型別來自 `shared:network`、`shared:database`、`shared:datastore`。
- [ ] 1.2 檢查 `shared:app` public API 簽名，確認是否仍直接暴露 DataStore、Room 或底層 module 型別。
- [ ] 1.3 檢查 Android app 與 iOS app 是否直接使用底層 datasource，而非 repository / UseCase / facade。

## 2. 收斂 shared:data

- [ ] 2.1 將 `MovieRepositoryImpl` 改為 `internal class`。
- [ ] 2.2 將 `UserDataRepositoryImpl` 改為 `internal class`。
- [ ] 2.3 將 `MovieGenrePagingSource`、`MovieSearchPagingSource` 改為 `internal class`。
- [ ] 2.4 將 `MovieCardResult.asCollectEntity()`、`MovieCardResult.asHistoryEntity()` 改為 `internal fun`。
- [ ] 2.5 調整 `shared/data/build.gradle.kts`：`shared:network`、`shared:database`、`shared:datastore` 改為 `implementation`。
- [ ] 2.6 調整 `shared/data/build.gradle.kts`：`androidx-paging-common` 改為 `api`，因 `MovieRepository` public API 回傳 `PagingData`。
- [ ] 2.7 確認 `shared:data` 對 `shared:model` 保持 `api`，對 `shared:common` 使用 `implementation`。

## 3. 收斂 shared:app dependency policy

- [ ] 3.1 將 `shared/app/build.gradle.kts` 中僅供組裝 Koin 使用的 `shared:*` dependency 改為 `implementation`。
- [ ] 3.2 若 `shared:app` public API 仍暴露底層型別，決定是保留對應 `api` 並記錄理由，或新增平台 facade 讓 raw entry point 改為 internal。
- [ ] 3.3 確認 iOS framework export 只保留 Swift 端實際需要直接呼叫的 shared API。

## 4. 驗證

- [ ] 4.1 執行 `.\gradlew.bat :shared:data:compileKotlinMetadata`。
- [ ] 4.2 執行 `.\gradlew.bat :shared:domain:compileKotlinMetadata`。
- [ ] 4.3 執行 `.\gradlew.bat :shared:app:compileKotlinMetadata`。
- [ ] 4.4 執行 `.\gradlew.bat :shared:data:testAndroidHostTest` 或專案可用的 data 層測試任務。
- [ ] 4.5 執行 `.\gradlew.bat :androidApp:assembleDebug`，確認 app 端不依賴被隱藏的底層型別。
