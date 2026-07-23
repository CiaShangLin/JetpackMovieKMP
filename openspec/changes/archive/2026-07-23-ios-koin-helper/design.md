## Context

`shared` 模組的 DI 全部透過 Koin 管理，`InitKoinIos.kt` 已負責在 iOS 端啟動 Koin
容器（`doInitKoinIos(isDebug:)`），但啟動之後沒有任何機制讓 Swift 端取得容器裡的
實例：Koin 官方提供的 `get<T>()` 是 Kotlin reified generic function，Swift/Obj-C
互通層無法呼叫這種簽章。

`openspec/backlog.md` 記錄了兩則相關但範圍不同的備忘錄：

1. 「在 shared iosMain 層新增 KoinHelper 供 iOS 端使用」——本次要處理的通用橋接機制。
2. 「首頁 feature 層消費 domain UseCase 的架構設計」——`HomeScreenModel` 實際透過
   `KoinHelper` 曝露給 Swift 消費的具體案例，**不在本次範圍**，留待該 change 處理。

目前 `iosApp` 唯一存在的 Swift ViewModel（`MainViewModel`）是空的，尚未呼叫任何
Koin 依賴，因此本次沒有現成、非做不可的 Swift 消費端可以串接。

## Goals / Non-Goals

**Goals:**
- 在 `shared/app` 的 `iosMain` 建立 `KoinHelper : KoinComponent`，提供具名（非 reified
  generic）方法給 Swift 端解析 Koin 依賴。
- 建立日後新增 accessor 的命名慣例：以一個示範用 accessor（`userDataRepository()`）
  為範本。
- 用自動化測試（iOS target）驗證 `KoinComponent` 在 iOS 上確實能在 `doInitKoinIos`
  啟動 Koin 之後正確解析出實例，不依賴人工在 Swift App 裡點一遍才發現問題。
- 記錄消費端取得 `KoinHelper` 解析出的實例後的注入慣例（見 Decisions），供未來
  各 Swift 消費端 change 遵循一致模式。

**Non-Goals:**
- 不串接 `HomeScreenModel` 或任何 `domain` UseCase 的實際 Swift 消費邏輯（留給
  「首頁 feature 層」相關 change）。
- 不修改 `iosApp` 任何 Swift 檔案，不變更 `MainViewModel`／`MainView`。
- 不在本次導入 SKIE（獨立 backlog 項目，需另外評估與 Kotlin 2.4.0 的相容性）。
- 不引入 Koin 以外或專屬 iOS 的 DI 框架（例如 Factory、Swinject）。

## Decisions

### 1. 用具名 `KoinComponent` object，而非嘗試曝露 reified generic

Koin 官方針對 KMP + Swift 互通的建議模式就是額外包一層具名 facade：

```kotlin
object KoinHelper : KoinComponent {
    fun userDataRepository(): UserDataRepository = getKoin().get()
}
```

`getKoin().get()` 内部呼叫仍是 reified generic，但因為呼叫端（Swift）看到的是
`KoinHelper` 上一個具體回傳型別的具名方法，Kotlin/Native 匯出的 Obj-C header
會產生正常可呼叫的方法簽章，沒有 reified generic 直接曝露的問題。

**替代方案考慮**：直接讓 Swift 端呼叫 `Koin_androidKt` 之類的低階 API——會需要
Swift 端自行处理型別轉換與 nullability，且不符合「集中一處管理 iOS 曝露介面」
的可維護性目標，故不採用。

### 2. 本次只做通用橋接機制，不含 `HomeScreenModel` 實際串接

雖然 backlog 兩則記錄互相關聯，但把「機制」跟「第一個具體業務案例」分開有兩個
好處：範圍收斂、易於審查；且 `HomeScreenModel` 串接還牽涉 Android/iOS
`ViewModel` 生命週期收尾（`onCleared()` / SwiftUI `.onDisappear`）等額外設計
決策，值得獨立一個 change 討論，不與「Swift 能否拿到 Koin 實例」這個更底層的
問題混在一起。

### 3. 選用不含 Flow 的 `UserDataRepository` 作為示範 accessor，並用 iOS 測試驗證（而非接真實 Swift 消費端）

由於決定不接任何 Swift 消費端，若 `KoinHelper` 完全不曝露任何方法，就沒有東西
能證明「Koin 在 iOS target 上真的能透過這層物件解析成功」——`KoinComponent`
在 iOS Native target 的行為、`getKoin()` 是否要求 Koin 已在 `doInitKoinIos`
啟動之後才能安全呼叫，這些都是有風險、值得現在就驗證的假設。

因此本次會：
- 曝露一個真實存在、依賴關係單純的 accessor：`fun userDataRepository(): UserDataRepository`。
- 在 `shared/app` 的 `iosTest`（或 `iosSimulatorArm64Test`）啟動 Koin 後呼叫這個
  accessor，斷言能成功解析出非 null 實例。

這個驗證是用 **Kotlin 測試**呼叫，不涉及 Swift 端如何消費 `UserDataRepository`
內部的 `Flow` 屬性（`userData: Flow<UserData>`）——那是 SKIE 或手動 Flow wrapper
要解決的問題，跟本次「Swift 能否拿到實例」的驗證目標無關，不影響本次範圍判斷。

**替代方案考慮**：完全空殼 object（不曝露任何 accessor、不寫測試）——範圍最小，
但沒有任何東西能證明機制可用，风险留到下一個消費端 change 才會被發現，故不採用。

### 4. 消費端注入慣例：建構值（constructor injection）手動傳遞，不做完整 iOS DI

目前 iOS 端還沒有專屬 DI 框架。約定消費端取得 `KoinHelper` 解析出的實例後，
**由呼叫端（例如 SwiftUI App 的 `init` 或畫面組裝處）取得實例，再以建構子參數
往下傳遞**給需要的 `ViewModel`／物件，而不是讓每個物件內部各自呼叫
`KoinHelper.shared.xxx()`（避免依賴散落各處、難以在測試時替換假物件）。

例如（示意，非本次實際變更）：

```swift
let repo = KoinHelper.shared.userDataRepository()
let viewModel = MainViewModel(userDataRepository: repo)
```

**日後遷移路徑**：若之後決定導入 iOS 專用 DI 框架，只需要把「取得實例」那一行
（`KoinHelper.shared.xxx()`）換成該框架的解析語法，`ViewModel` 以建構子接收依賴
的介面設計不需要更動——因為呼叫端一開始就是把依賴當一般建構子參數傳遞，而非讓
`ViewModel` 內部耦合特定 DI API。這讓「先手動注入、之後換框架」的路徑成本降到
最低。

**替代方案考慮**：讓每個消費物件內部直接呼叫 `KoinHelper.shared.xxx()`（service
locator 風格）——實作起來更少的中間傳遞，但會讓依賴來源分散在各處、不利於之後
抽換 DI 方案或在測試中替換假物件，故不採用；仍以建構子注入為慣例，`KoinHelper`
只在「組裝根」（composition root，如 App 進入點）呼叫。

### 5. 不在本次導入 SKIE

SKIE 解決的是「Swift 消費 Kotlin suspend function／Flow」的體驗問題，跟本次
「Swift 能否解析出 Koin 實例」是兩個獨立關注點。本次不接任何 Swift 消費端，
用不到 SKIE 帶來的 async/await、AsyncSequence 互通改善；且 SKIE 涉及編譯器
plugin 相容性評估（需確認與專案 Kotlin 2.4.0 的相容性），風險與驗收方式都跟
本次不同，混在一起會讓本次 change 難以聚焦與驗收。SKIE 維持獨立 backlog 項目，
待有實際 Swift 消費 Flow/suspend function 的 change 出現時再評估導入。

## Risks / Trade-offs

- **[風險] 沒有真實 Swift 消費端，機制的「Swift 端實際呼叫體驗」本次無法驗證**
  → 緩解：用 Kotlin 端的 iOS 測試驗證 `KoinComponent` 解析行為本身正確；真正的
  Swift 呼叫體驗留待第一個消費端 change（例如 `HomeScreenModel` 串接）驗證，
  屆時若發現匯出簽章有問題，只需調整 `KoinHelper` 內的 accessor 寫法，不影響
  本次已驗證的「Koin 解析」核心邏輯。
- **[風險] `KoinComponent` 依賴全域 `GlobalContext`，若呼叫 `KoinHelper` 時 Koin
  尚未透過 `doInitKoinIos` 啟動會拋例外** → 緩解：測試會在啟動 Koin 之後才呼叫
  accessor，並在設計文件中明確記錄「`KoinHelper` 僅能在 `doInitKoinIos` 呼叫
  之後使用」這個前置條件，供未來消費端開發者知悉。
- **[取捨] 只曝露一個示範 accessor，未來每新增一個 Swift 消費端都要手動在
  `KoinHelper` 加一個對應方法** → 這是刻意的取捨：reified generic 無法匯出給
  Swift，具名方法本來就無法自動化产生，此成本無法避免，只能靠命名慣例降低
  維護負擔。
