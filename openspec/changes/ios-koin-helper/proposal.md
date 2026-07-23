## Why

`shared` 模組的 Koin 依賴目前只能透過 Kotlin 的 reified generic `get<T>()` 解析，Swift 端完全無法呼叫這種簽章。`iosApp` 至今沒有任何從 Koin 容器取得依賴的程式碼（`MainViewModel` 目前是空的），一旦未來要讓 Swift 端消費 `shared` 提供的 Repository／UseCase，就會卡在這個語言互通的缺口上。需要先在 `shared` 的 iOS 層補上一個明確的橋接物件，讓 Swift 端能以具名方法取得 Koin 注入的實例。

## What Changes

- 在 `shared/app` 的 `iosMain` 新增 `KoinHelper : KoinComponent`，以具名（非 reified generic）方法暴露 Koin 依賴，供 Swift 端以 `KoinHelper.shared.xxx()` 呼叫。
- 暴露一個示範用 accessor（`userDataRepository()`），做為日後新增 accessor 的命名慣例範本，其餘依賴（例如 `HomeScreenModel`）留待各自的消費端 change 再新增對應方法。
- 在 `shared/app` 補一支 iOS 測試（`iosTest` / `iosSimulatorArm64Test`），啟動 Koin 後呼叫該 accessor，驗證 `KoinComponent` 在 iOS target 上能正確解析出實例。
- 不修改 `iosApp` 任何 Swift 檔案，本次不接任何實際 Swift 消費端（`MainViewModel` 維持現狀）。
- 不在本次導入 SKIE（留給獨立的 `openspec/backlog.md` 項目處理），也不引入完整的 iOS 專用 DI 框架——消費端取得 `KoinHelper` 解析出的實例後，一律以建構子參數（constructor injection）往下傳遞，作為過渡期的手動注入慣例。

## Capabilities

### New Capabilities
- `ios-koin-bridge`：定義 `shared` 模組在 iOS 層透過具名 `KoinComponent` object 對 Swift 曝露 Koin 依賴解析能力的規格，包含 accessor 命名慣例與消費端取得實例後應以建構子注入方式往下傳遞的慣例。

### Modified Capabilities
（無，本次不變更既有 capability 的需求）

## Impact

- `shared/app`（iosMain）：新增 `KoinHelper.kt`。
- `shared/app`（iosTest 或 iosSimulatorArm64Test）：新增驗證 `KoinHelper` 解析成功的測試。
- 不影響 `iosApp`、`androidApp`、`core/*` 及其餘 `shared/*` 模組的既有程式碼。
- 不新增第三方依賴，不涉及 `buildSrc` 變更。
