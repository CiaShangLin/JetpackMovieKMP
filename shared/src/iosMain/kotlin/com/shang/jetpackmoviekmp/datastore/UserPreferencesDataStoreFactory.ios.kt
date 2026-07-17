package com.shang.jetpackmoviekmp.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.shang.jetpackmoviekmp.common.resolveIosDocumentDirectoryPath

/**
 * 建立以穩定 app document 路徑為後盾的使用者偏好設定 DataStore，確保 app 重啟後仍可讀取。
 *
 * @return 可讀寫使用者偏好設定的 [DataStore]。
 * @throws IllegalArgumentException 當無法解析 iOS document directory 時拋出。
 */
fun createUserPreferencesDataStore(): DataStore<Preferences> =
    createUserPreferencesDataStore(
        producePath = { resolveIosDocumentDirectoryPath(USER_PREFERENCES_FILE_NAME) },
    )
