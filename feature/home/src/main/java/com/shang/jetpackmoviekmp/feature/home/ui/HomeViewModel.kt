package com.shang.jetpackmoviekmp.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shang.jetpackmoviekmp.common.AppResult
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 管理首頁電影分類清單的 UI 狀態。
 *
 * @property userDataRepository 提供語言設定變化訊號，語言切換時重新載入分類清單。
 * @property movieRepository 電影分類資料來源。
 */
class HomeViewModel(
    private val userDataRepository: UserDataRepository,
    private val movieRepository: MovieRepository,
) : ViewModel() {

    private val _refreshTrigger = MutableStateFlow(0)

    /** 電影分類清單的載入狀態；`languageMode` 變化或呼叫 [retry] 時重新載入，主題變化不影響。 */
    @OptIn(ExperimentalCoroutinesApi::class)
    val movieGenres =
        combine(
            _refreshTrigger,
            userDataRepository.userData.map { it.languageMode }.distinctUntilChanged(),
        ) { _, _ -> Unit }
            .flatMapLatest {
                movieRepository.getMovieGenres()
                    .map { result ->
                        when (result) {
                            is AppResult.Success -> HomeUiState.Success(result.data)
                            is AppResult.Failure -> HomeUiState.Error(result.error)
                        }
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Companion.WhileSubscribed(5000),
                initialValue = HomeUiState.Loading,
            )

    /** 觸發電影分類清單重新載入。 */
    fun retry() {
        viewModelScope.launch {
            _refreshTrigger.value += 1
        }
    }
}
