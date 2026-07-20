package com.shang.jetpackmoviekmp.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 記憶體內的 [DataStore] 測試替身，避免 commonTest 依賴平台檔案 I/O。
 */
class InMemoryPreferencesDataStore : DataStore<Preferences> {

    private val state = MutableStateFlow<Preferences>(emptyPreferences())

    override val data = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
