package com.shang.jetpackmoviekmp.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val userDataRepository: UserDataRepository,
    private val movieRepository: MovieRepository,
) : ViewModel() {

    private val _refreshTrigger = MutableStateFlow(0)

    val movieGenres =
        _refreshTrigger.flatMapLatest {
            movieRepository.getMovieGenres()
                .map {
                    it.fold(
                        onSuccess = {
                            HomeUiState.Success(it)
                        },
                        onFailure = {
                            HomeUiState.Error(it.cause)
                        },
                    )
                }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Companion.WhileSubscribed(5000),
                initialValue = HomeUiState.Loading,
            )

    fun retry() {
        viewModelScope.launch {
            _refreshTrigger.value += 1
        }
    }
}
