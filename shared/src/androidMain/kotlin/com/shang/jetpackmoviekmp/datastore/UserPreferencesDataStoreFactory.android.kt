package com.shang.jetpackmoviekmp.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * 建立以 Android app-owned storage 為後盾的使用者偏好設定 DataStore。
 *
 * 檔案位於 [Context.filesDir]，呼叫端不需自行處理檔案路徑。
 *
 * @param context Android context，用以取得 app-owned 檔案目錄。
 * @return 可讀寫使用者偏好設定的 [DataStore]。
 */
fun createUserPreferencesDataStore(context: Context): DataStore<Preferences> =
    createUserPreferencesDataStore(
        producePath = { context.filesDir.resolve(USER_PREFERENCES_FILE_NAME).absolutePath },
    )
