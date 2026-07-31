package com.shang.jetpackmoviekmp.feature.setting.ui

import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.LanguageMode
import com.shang.jetpackmoviekmp.model.ThemeMode
import com.shang.jetpackmoviekmp.model.UserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `userData 直接反映 repository 的資料流`() = runTest(dispatcher) {
        // Arrange
        val userData = UserData(ConfigurationBean(), ThemeMode.DARK, LanguageMode.ENGLISH)
        val repository = FakeUserDataRepository(initialUserData = userData)
        val viewModel = SettingViewModel(repository)

        // Act
        val state = viewModel.userData.first()

        // Assert
        assertEquals(userData, state, "userData 應與 repository 資料一致")
    }

    @Test
    fun `setThemeMode 會轉發至 repository 一次`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeUserDataRepository()
        val viewModel = SettingViewModel(repository)

        // Act
        viewModel.setThemeMode(ThemeMode.DARK)

        // Assert
        assertTrue(
            repository.setThemeModeLatch.await(1, TimeUnit.SECONDS),
            "setThemeMode 應觸發 repository 呼叫",
        )
        assertEquals(1, repository.setThemeModeCallCount, "setThemeMode 應只呼叫一次")
        assertEquals(ThemeMode.DARK, repository.lastThemeMode, "應傳入使用者選擇的 ThemeMode")
    }

    @Test
    fun `setLanguageMode 會轉發至 repository 一次`() = runTest(dispatcher) {
        // Arrange
        val repository = FakeUserDataRepository()
        val viewModel = SettingViewModel(repository)

        // Act
        viewModel.setLanguageMode(LanguageMode.ENGLISH)

        // Assert
        assertTrue(
            repository.setLanguageModeLatch.await(1, TimeUnit.SECONDS),
            "setLanguageMode 應觸發 repository 呼叫",
        )
        assertEquals(1, repository.setLanguageModeCallCount, "setLanguageMode 應只呼叫一次")
        assertEquals(LanguageMode.ENGLISH, repository.lastLanguageMode, "應傳入使用者選擇的 LanguageMode")
    }
}
