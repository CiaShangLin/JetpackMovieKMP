package com.shang.jetpackmoviekmp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shang.jetpackmoviekmp.common.AppResult
import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
import com.shang.jetpackmoviekmp.domain.usecase.GetConfigurationUseCase
import com.shang.jetpackmoviekmp.model.LanguageMode
import com.shang.jetpackmoviekmp.model.UserData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val getConfigurationUseCase: GetConfigurationUseCase,
    private val userDataRepository: UserDataRepository,
) : ViewModel() {

    private val _retryTrigger = MutableSharedFlow<Unit>()
    private var lastAppliedLanguageMode: LanguageMode? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val configuration: StateFlow<MainUiState> = _retryTrigger
        .onStart { emit(Unit) } // 初始載入
        .flatMapLatest {
            getConfigurationUseCase()
                .map { result ->
                    when (result) {
                        is AppResult.Success -> MainUiState.Success(result.data)
                        is AppResult.Failure -> MainUiState.Error(result.error)
                    }
                }
                .onStart { emit(MainUiState.Loading) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainUiState.Loading,
        )

    val userData = userDataRepository.userData.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserData.getDefault(),
    )

    /**
     * 重試載入配置
     */
    fun retryConfiguration() {
        viewModelScope.launch {
            _retryTrigger.emit(Unit)
        }
    }

    /**
     * 決定目前 Locale 套用後是否需要重建 Activity。
     *
     * [MainViewModel] 會在 configuration change 時保留，因此能避免同一個語言在重建後再次觸發
     * `recreate()`，導致 Splash 無法離開。
     */
    internal fun shouldRecreateForLanguage(languageMode: LanguageMode, localeChanged: Boolean): Boolean {
        val shouldRecreate = lastAppliedLanguageMode?.let { it != languageMode } ?: localeChanged
        lastAppliedLanguageMode = languageMode
        return shouldRecreate
    }
}
