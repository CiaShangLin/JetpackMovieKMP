package com.shang.jetpackmoviekmp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shang.jetpackmoviekmp.common.UiState
import com.shang.jetpackmoviekmp.common.toUiState
import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
import com.shang.jetpackmoviekmp.domain.usecase.GetConfigurationUseCase
import com.shang.jetpackmoviekmp.model.ConfigurationBean
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

/**
 * 管理 App 啟動設定與使用者資料的 UI 狀態。
 *
 * @property getConfigurationUseCase 取得遠端設定的用例。
 * @property userDataRepository 使用者偏好設定資料來源。
 */
class MainViewModel(
    private val getConfigurationUseCase: GetConfigurationUseCase,
    private val userDataRepository: UserDataRepository,
) : ViewModel() {

    private val _retryTrigger = MutableSharedFlow<Unit>()

    /** 設定的載入狀態；失敗後可由 [retryConfiguration] 重新載入。 */
    @OptIn(ExperimentalCoroutinesApi::class)
    val configuration: StateFlow<UiState<ConfigurationBean>> = _retryTrigger
        .onStart { emit(Unit) } // 初始載入
        .flatMapLatest {
            getConfigurationUseCase()
                .map { result -> result.toUiState() }
                .onStart { emit(UiState.Loading) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading,
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
}
