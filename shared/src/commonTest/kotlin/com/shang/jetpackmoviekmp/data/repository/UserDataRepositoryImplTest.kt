package com.shang.jetpackmoviekmp.data.repository

import com.shang.jetpackmoviekmp.datastore.InMemoryPreferencesDataStore
import com.shang.jetpackmoviekmp.datastore.UserPreferenceDataSource
import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.LanguageMode
import com.shang.jetpackmoviekmp.model.ThemeMode
import com.shang.jetpackmoviekmp.model.UserData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UserDataRepositoryImplTest {

    private fun repository() =
        UserDataRepositoryImpl(UserPreferenceDataSource(InMemoryPreferencesDataStore()))

    @Test
    fun userData_emits_default_when_nothing_persisted() = runTest {
        assertEquals(UserData.getDefault(), repository().userData.first())
    }

    @Test
    fun setConfiguration_reflected_in_userData() = runTest {
        val repository = repository()
        val configuration = ConfigurationBean(changeKeys = listOf("images"))

        repository.setConfiguration(configuration)

        assertEquals(configuration, repository.userData.first().configuration)
    }

    @Test
    fun setThemeMode_reflected_in_userData() = runTest {
        val repository = repository()

        repository.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, repository.userData.first().themeMode)
    }

    @Test
    fun setLanguageMode_reflected_in_userData() = runTest {
        val repository = repository()

        repository.setLanguageMode(LanguageMode.ENGLISH)

        assertEquals(LanguageMode.ENGLISH, repository.userData.first().languageMode)
    }
}
