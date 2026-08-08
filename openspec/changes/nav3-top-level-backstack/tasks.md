## 1. androidApp：新增 TopLevelBackStack

- [ ] 1.1 改寫 `androidApp/src/main/kotlin/com/shang/jetpackmoviekmp/ui/MainNavBackStack.kt`：移除 `switchTab()`，改為官方 Common UI recipe 版本的 `TopLevelBackStack<T: Any>` class（`topLevelStacks: LinkedHashMap<T, SnapshotStateList<T>>`、`topLevelKey`、`backStack`、`addTopLevel()`、`add()`、`removeLast()`），泛型 `T` 綁定為 `androidx.navigation3.runtime.NavKey`。

## 2. androidApp：整合 MainActivity

- [ ] 2.1 `MainActivity.kt` 將 `rememberNavBackStack(HomeKey)` 改為 `remember { TopLevelBackStack(HomeKey) }`（或對應的 `rememberSaveable`／`remember` 包裝方式，維持既有語言切換 `key(userData.languageMode)` 不包住此建立呼叫的既有規則）。
- [ ] 2.2 `SuccessScreen()`：`NavDisplay(backStack = ...)` 改傳入 `topLevelBackStack.backStack`；`onBack` 改呼叫 `topLevelBackStack.removeLast()`；`currentKey is MovieDetailKey` 的隱藏 Navigation Suite 判斷改用 `topLevelBackStack.backStack.lastOrNull() is MovieDetailKey`。
- [ ] 2.3 底部導覽 `JMNavigationSuiteScaffold` 的 `item(...)`：`selected` 判斷改用 `topLevelBackStack.topLevelKey == item.key`；`onClick` 改呼叫 `topLevelBackStack.addTopLevel(item.key)`（移除對 `switchTab()` 的呼叫）。
- [ ] 2.4 `mainEntry()`：所有 `backStack.add(MovieDetailKey(...))` 改為 `topLevelBackStack.add(MovieDetailKey(...))`；`movieDetailEntry` 的 `onBackClick = { backStack.removeLastOrNull() }` 改為 `{ topLevelBackStack.removeLast() }`。

## 3. androidApp：測試調整

- [ ] 3.1 改寫 `androidApp/src/test/kotlin/com/shang/jetpackmoviekmp/ui/MainNavBackStackTest.kt`：移除針對 `switchTab()`（MRU reorder）的既有測試案例，改為驗證 `TopLevelBackStack` 的行為：`addTopLevel()` 切換 Tab 後保留該 Tab 原有 sub back stack、不同 Tab 的 sub back stack 互不影響、`add()`／`removeLast()` 只影響目前 Tab 的 sub back stack、Tab 根畫面按返回鍵切到其餘 Tab 中最後加入的一個、只剩最後一個 Tab 時 `backStack` 只剩 1 筆。
- [ ] 3.2 執行 `./gradlew :androidApp:testDebugUnitTest`（或專案既有對應 test task）確認新測試全數通過。

## 4. 最終驗證

- [ ] 4.1 執行 `./gradlew ktlintCheck` 確認格式與命名規範通過。
- [ ] 4.2 執行 `./gradlew :androidApp:assembleDebug` 確認可正常編譯。
- [ ] 4.3 執行 `openspec validate nav3-top-level-backstack --type change --strict --no-interactive` 確認 proposal／design／specs／tasks 全數通過驗證。
