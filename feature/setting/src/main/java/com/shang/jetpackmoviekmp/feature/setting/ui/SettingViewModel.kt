package com.shang.jetpackmoviekmp.feature.setting.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
import com.shang.jetpackmoviekmp.model.LanguageMode
import com.shang.jetpackmoviekmp.model.ThemeMode
import com.shang.jetpackmoviekmp.model.UserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 管理設定頁使用者偏好設定的 ViewModel。
 *
 * @property userDataRepository 使用者偏好設定的存取來源。
 */
class SettingViewModel(
    private val userDataRepository: UserDataRepository,
) : ViewModel() {

    /** 目前使用者偏好設定對應的可觀察畫面狀態。 */
    val userData = userDataRepository.userData.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserData.getDefault(),
    )

    /**
     * 持久化使用者選擇的主題模式。
     *
     * @param themeMode 使用者選擇的主題模式。
     */
    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch(Dispatchers.IO) {
            userDataRepository.setThemeMode(themeMode)
        }
    }

    /**
     * 持久化使用者選擇的語言模式。
     *
     * @param languageMode 使用者選擇的語言模式。
     */
    fun setLanguageMode(languageMode: LanguageMode) {
        viewModelScope.launch(Dispatchers.IO) {
            userDataRepository.setLanguageMode(languageMode)
        }
    }
}
