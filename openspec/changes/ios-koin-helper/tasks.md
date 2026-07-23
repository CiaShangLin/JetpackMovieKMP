## 1. shared/app（iosMain）

- [ ] 1.1 在 `shared/app/src/iosMain/kotlin/com/shang/jetpackmoviekmp/` 新增
      `KoinHelper.kt`，定義 `object KoinHelper : KoinComponent`
- [ ] 1.2 於 `KoinHelper` 新增 `fun userDataRepository(): UserDataRepository =
      getKoin().get()` 作為示範 accessor
- [ ] 1.3 為 `KoinHelper` 補上 KDoc（繁體中文），說明：僅能於 `doInitKoinIos`
      啟動 Koin 之後呼叫、日後新增 accessor 的命名慣例、消費端應以建構子注入
      方式取得並往下傳遞實例（不得在物件內部直接呼叫 `KoinHelper`）

## 2. shared/app（iOS 測試）

- [ ] 2.1 於對應的 iOS 測試 source set（`iosTest` 或
      `iosSimulatorArm64Test`，依現有 `shared/app` 測試慣例決定）新增測試檔
- [ ] 2.2 測試以 AAA 模式撰寫：Arrange 啟動含必要 module（至少
      `datastoreModule()`、`commonModule()` 等 `userDataRepository()` 解析
      所需的 module）的 Koin 容器；Act 呼叫 `KoinHelper.userDataRepository()`；
      Assert 回傳值非 null 且型別為 `UserDataRepository`
- [ ] 2.3 測試結束後停止 Koin（`stopKoin()` 或等效方式），避免影響同一測試
      進程中其他測試案例

## 3. 驗證

- [ ] 3.1 執行 `./gradlew :shared:app:iosSimulatorArm64Test`（或對應的 iOS 測試
      task）確認新測試通過
- [ ] 3.2 執行 `./gradlew ktlintCheck` 確認格式通過
- [ ] 3.3 確認 `iosApp` 目錄與其餘 `shared/*` 模組無任何檔案變更
