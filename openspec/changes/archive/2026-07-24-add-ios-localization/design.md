## Context

`iosApp` 目前沒有任何在地化基礎設施：

- `iosApp/iosApp.xcodeproj/project.pbxproj` 的 `developmentRegion = en`、
  `knownRegions = (en, Base)`，沒有註冊 `zh-Hant`。
- `iosApp/iosApp/Info.plist` 沒有 `CFBundleLocalizations` 設定。
- 寫死文案分佈在 `SplashView.swift`（畫面文字）與 `SplashViewModel.swift`
  （連錯誤訊息都寫死在 ViewModel 裡）。

shared 層已有獨立的語言設定機制（`shared/model` 的 `LanguageMode`、
`shared/datastore` 的 `DatastoreLanguageProvider`），並透過共用的 `initKoin()`
讓 TMDB API 呼叫自動帶上正確的 `language` query 參數。這套機制 Android 與 iOS
共用，且已經對 iOS 生效，本次 change **不需要**、也**不會**去動它。

本次 change 要解決的是另一個獨立問題：**iOS 畫面上顯示的文案本身沒有多語系能力**。

## Goals / Non-Goals

**Goals:**
- 導入 Xcode String Catalog（`.xcstrings`）作為 iOS UI 字串在地化機制。
- 讓 iOS 專案支援 `zh-Hant`（繁體中文）與 `en`（英文）兩個語系，畫面文案能
  依照 iOS 系統語言設定自動顯示對應翻譯（Xcode 原生行為，非自行實作邏輯）。
- 把目前已知的寫死文案（Splash 相關）抽成在地化字串。
- 調整 `SplashViewModel` 的錯誤狀態設計，讓 ViewModel 不再直接持有／回傳
  要顯示給使用者的文案字串，職責回歸「View 負責顯示文字、ViewModel 只回傳狀態」。

**Non-Goals:**
- **不**實作「App 內主動切換語言」（覆蓋系統語言）的功能。這需要額外處理 iOS
  per-app language override 機制（例如 `AppleLanguages` UserDefaults 技巧或
  `.environment(\.locale)` 手動注入），複雜度與風險都較高，留待未來有明確需求時
  另立 change 討論。
- **不**在此 change 內讓 iOS Swift 層讀取或訂閱 shared 層的 `LanguageMode` /
  `UserDataRepository.userData`。這是實作「App 內切換」時才需要的整合點，
  現階段範圍不含此需求。
- **不**修改 `shared/*` 任何模組、不修改 TMDB API 語言參數的組成方式。
- **不**支援 `zh-Hant`、`en` 以外的語系（暫不含簡體中文、日文等）。

## Decisions

### 決策 1：採用 String Catalog（`.xcstrings`）而非傳統 `Localizable.strings`

**選擇**：使用 Xcode 15+ 內建的 String Catalog 格式。

**理由**：
- Xcode 會自動偵測程式碼中出現的字串字面值並提示加入 Catalog，對初學者而言
  比手動維護多個 `Localizable.strings` 檔案更不容易漏掉未翻譯的字串。
- Apple 目前主推此格式，新專案的官方文件與範例大多以此為主。
- 單一檔案管理所有語言（而非每語言一個檔案），減少多檔案不同步的風險。

**考慮過的替代方案**：傳統 `Localizable.strings`（每語言各一檔案，`"key" = "value";`
格式）。優點是網路教學資源多、格式單純；缺點是需要自行確保每個語言檔案的 key
都同步存在，較容易在新增語言時遺漏翻譯。因為專案本來就是新導入、沒有既有
`.strings` 檔案包袱，選擇直接上 String Catalog。

### 決策 2：本次僅支援「跟隨系統語言」，不做 App 內語言切換

**選擇**：只設定 Xcode 專案支援 `zh-Hant` / `en` 兩語系，讓 iOS 依系統設定
（`設定 App` → `一般` → `語言與地區`）自動選字串，不做任何 App 內的語言覆蓋開關。

**理由**：
- iOS 沒有像 Android `Configuration.setLocale()` + `createConfigurationContext()`
  那樣「在 Activity/Context 層級覆蓋語言」的對應機制；要做到「App 內切換、
  不影響系統語言」通常需要额外的 per-app language override 處理，屬於中高複雜度
  工作，不適合跟「導入基礎在地化能力」放在同一個 change 內。
- 拆成兩個 change 可以讓本次 change 聚焦在「把寫死字串換成可翻譯字串」這個
  單一目標，降低風險與審查範圍；App 內切換功能待有明確需求時另外提案，
  屆時才需要決定如何跟 shared 層 `LanguageMode` 整合。

**考慮過的替代方案**：與 Android 的 `LanguageSettingUtils` 行為對齊，一次到位
支援 App 內切換。因為需要額外處理 iOS 特有的 per-app locale override 機制、
且目前 Android 端這個功能本身也還沒有正式的使用者 UI（`AppDiagnostics.setLanguage`
僅是驗證用途），評估後認為分階段導入風險更低。

### 決策 3：`SplashViewModel` 不再持有使用者可見的錯誤文案字串

**選擇**：`SplashUiState.failure(error:)` 的 `error` 不再是「要顯示給使用者看」
的字串；`SplashViewModel` 只回傳失敗狀態，實際顯示的在地化文案由 `SplashView`
決定並透過 String Catalog 取得。技術性錯誤內容（例如底層 exception message）
不直接顯示在畫面上。

**理由**：
- 職責分離：ViewModel 屬於畫面邏輯層，不應該持有 UI 顯示用的文字內容，
  這樣文案的語言與措辭都能集中由 View 層與 String Catalog 管理。
- 避免把來源不明的技術性錯誤訊息（可能是英文的 exception message、也可能是
  之前寫死的中文 fallback）直接曝露給終端使用者，這在使用者體驗與資安揭露
  兩方面都不理想。

**考慮過的替代方案**：保留 ViewModel 回傳字串、只是把字串內容換成在地化 key，
在 ViewModel 內部呼叫 `String(localized:)`。這個做法改動範圍最小，但會讓
ViewModel 直接依賴在地化 API，架構上職責稍微混合；經與使用者討論後，選擇
職責分離更乾淨的做法。

## Risks / Trade-offs

- **[風險] 之後若要支援 App 內切換語言，`SplashUiState` 或其他 ViewModel 的
  狀態設計可能需要再調整，屆時需要重新檢視「View 依 Locale 顯示文案」的實作
  是否要改成主動監聽 `LanguageMode` 而非只依賴系統設定。**
  → 緩解：本次 Decisions 已明確記錄「View 負責翻譯」的分工，未來只需要新增
  「View 依 `LanguageMode` 決定 locale」這一段，不需要重構現有的狀態流轉邏輯。

- **[風險] Xcode String Catalog 是相對新的功能，若團隊未來需要支援較舊版本
  Xcode 可能不相容。**
  → 緩解：專案已使用 Xcode 15+（`ios-skie-interop` 等既有 spec 顯示專案採用
  現代 Swift/Xcode 工具鏈），此風險發生機率低，暫不特別處理。

- **[風險] 目前只處理 Splash 相關文案，`MainView` 等其他畫面之後新增文案時，
  若沒有遵循「透過 String Catalog」的慣例，容易重新出現寫死字串。**
  → 緩解：tasks.md 會包含「確認建置流程／文件提醒後續畫面比照辦理」的收尾項目，
  但實際的团队規範落地仍需仰賴 code review 把關，非本 change 能完全保證。

## Migration Plan

1. Xcode 專案設定新增 `zh-Hant` 語系、更新 `knownRegions`。
2. 建立 `Localizable.xcstrings` String Catalog 檔案，並將現有 Splash 相關寫死
   字串登錄進去（中文與英文兩個翻譯）。
3. 調整 `SplashUiState` / `SplashViewModel` 的錯誤狀態設計，改由 `SplashView`
   負責顯示在地化文案。
4. 更新 `SplashView.swift` 改用 String Catalog 字串。
5. 手動在模擬器切換系統語言（中文／英文）驗證畫面文案正確切換。

不涉及資料庫或正式環境資料遷移，屬於純前端（iOS UI 層）變更，無需 rollback
以外的特殊回退機制；如需回退，直接 revert 此 change 的 commit 即可。

## Open Questions

- 無（本次範圍內的技術決策已與使用者確認完畢；App 內語言切換功能的技術方案
  留待未來該 change 啟動時再討論）。
