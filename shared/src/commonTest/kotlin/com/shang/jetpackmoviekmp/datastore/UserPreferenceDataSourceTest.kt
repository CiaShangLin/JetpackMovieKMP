package com.shang.jetpackmoviekmp.datastore

import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.LanguageMode
import com.shang.jetpackmoviekmp.model.ThemeMode
import com.shang.jetpackmoviekmp.model.UserData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UserPreferenceDataSourceTest {

    @Test
    fun userData_emits_default_when_nothing_persisted() = runTest {
        val dataSource = UserPreferenceDataSource(InMemoryPreferencesDataStore())

        assertEquals(UserData.getDefault(), dataSource.userData.first())
    }

    @Test
    fun setThemeMode_persists_theme_mode() = runTest {
        val dataSource = UserPreferenceDataSource(InMemoryPreferencesDataStore())

        dataSource.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, dataSource.userData.first().themeMode)
    }

    @Test
    fun setLanguageMode_persists_language_mode() = runTest {
        val dataSource = UserPreferenceDataSource(InMemoryPreferencesDataStore())

        dataSource.setLanguageMode(LanguageMode.ENGLISH)

        assertEquals(LanguageMode.ENGLISH, dataSource.userData.first().languageMode)
    }

    @Test
    fun setConfiguration_persists_configuration() = runTest {
        val dataSource = UserPreferenceDataSource(InMemoryPreferencesDataStore())
        val configuration = ConfigurationBean(
            changeKeys = listOf("adult_names"),
            images = ConfigurationBean.Images(baseUrl = "https://image.tmdb.org/t/p/"),
        )

        dataSource.setConfiguration(configuration)

        assertEquals(configuration, dataSource.userData.first().configuration)
    }
}
