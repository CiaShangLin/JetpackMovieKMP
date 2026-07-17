package com.shang.jetpackmoviekmp

import com.shang.jetpackmoviekmp.datastore.createUserPreferencesDataStore

/**
 * `iosApp` 啟動時呼叫的進入點。單獨放在不含 `.ios.` 修飾的檔名下，
 * 確保 Kotlin/Native 匯出到 Swift 的 facade class 名稱穩定（`InitKoinIosKt`），
 * 讓 Swift 端不需要自行組出 `DataStore`（`createUserPreferencesDataStore()` 只在
 * Kotlin 端呼叫，不會出現在 Swift 匯出介面上）。
 *
 * 這裡刻意不靠 `@ObjCName` 釘住 facade 名稱——該 annotation 在本專案的 Kotlin
 * 版本不支援 `FILE` target（僅支援 class/property/value parameter/function），
 * 因此改用「檔名不含 `.ios.`」這個慣例；若之後要重新命名此檔案，需同步更新
 * `iosApp/iosApp/iOSApp.swift` 呼叫處引用的 facade class 名稱。
 *
 * @param isDebug 為 `true` 時啟用 network request logging。
 */
fun doInitKoinIos(isDebug: Boolean) {
    initKoin(dataStore = createUserPreferencesDataStore(), isDebug = isDebug)
}
