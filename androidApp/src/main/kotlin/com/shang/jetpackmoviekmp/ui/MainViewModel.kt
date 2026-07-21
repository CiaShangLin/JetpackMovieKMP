package com.shang.jetpackmoviekmp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
import com.shang.jetpackmoviekmp.domain.usecase.GetConfigurationUseCase
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val configuration: StateFlow<MainUiState> = _retryTrigger
        .onStart { emit(Unit) } // 初始載入
        .flatMapLatest {
            getConfigurationUseCase()
                .map { result ->
                    result.fold(
                        onSuccess = { MainUiState.Success(it) },
                        onFailure = { MainUiState.Error(it) },
                    )
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
}
