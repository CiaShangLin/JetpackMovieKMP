package com.shang.jetpackmoviekmp.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * 建立以穩定 app document 路徑為後盾的使用者偏好設定 DataStore，確保 app 重啟後仍可讀取。
 *
 * @return 可讀寫使用者偏好設定的 [DataStore]。
 * @throws IllegalArgumentException 當無法解析 iOS document directory 時拋出。
 */
@OptIn(ExperimentalForeignApi::class)
fun createUserPreferencesDataStore(): DataStore<Preferences> =
    createUserPreferencesDataStore(
        producePath = {
            val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null,
            )
            requireNotNull(documentDirectory?.path) { "Unable to resolve iOS document directory" } +
                "/$USER_PREFERENCES_FILE_NAME"
        },
    )
