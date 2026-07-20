package com.shang.jetpackmoviekmp.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [DataStore] implementation for unit tests.
 *
 * This keeps preference state inside the test process so datastore consumers can
 * be exercised without touching platform storage.
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
