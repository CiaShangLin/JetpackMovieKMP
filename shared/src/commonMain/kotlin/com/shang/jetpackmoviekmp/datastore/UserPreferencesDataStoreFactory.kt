package com.shang.jetpackmoviekmp.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

internal const val USER_PREFERENCES_FILE_NAME = "user_preferences.preferences_pb"

/**
 * 依平台提供的檔案路徑建立使用者偏好設定 DataStore，讓平台檔案路徑邏輯不進入共用業務邏輯。
 */
internal fun createUserPreferencesDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })
