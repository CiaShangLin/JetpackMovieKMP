package com.shang.jetpackmoviekmp

import android.content.Context
import com.shang.jetpackmoviekmp.database.getDatabaseBuilder
import com.shang.jetpackmoviekmp.datastore.createUserPreferencesDataStore
import org.koin.android.ext.koin.androidContext

/**
 * Android app 啟動時呼叫的 Koin facade。
 *
 * 呼叫端只需提供 [Context] 與 debug 狀態，不需要自行建立 DataStore 或 Room database builder。
 *
 * @param context Android context，用於建立平台儲存層並設定 Koin Android context。
 * @param isDebug 為 `true` 時啟用 network request logging。
 */
fun initKoinAndroid(
    context: Context,
    isDebug: Boolean,
) {
    initKoin(
        dataStore = createUserPreferencesDataStore(context),
        databaseBuilder = { getDatabaseBuilder(context) },
        isDebug = isDebug,
    ) {
        androidContext(context.applicationContext)
    }
}
