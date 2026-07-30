package com.shang.jetpackmoviekmp.presenter

import androidx.paging.LoadState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SearchMovieListLoadStateTest {

    @Test
    fun loadStates_retainsRefreshAndAppendStates() {
        // Act
        val states = SearchMovieListLoadStates(
            refresh = SearchMovieListLoadState.Loading,
            append = SearchMovieListLoadState.Idle,
        )

        // Assert
        assertEquals(SearchMovieListLoadState.Loading, states.refresh)
        assertEquals(SearchMovieListLoadState.Idle, states.append)
    }

    @Test
    fun toSearchMovieListLoadState_withLoading_returnsLoading() {
        // Act
        val state = LoadState.Loading.toSearchMovieListLoadState()

        // Assert
        assertEquals(SearchMovieListLoadState.Loading, state)
    }

    @Test
    fun toSearchMovieListLoadState_withNotLoading_returnsIdle() {
        // Act
        val state = LoadState.NotLoading(endOfPaginationReached = false).toSearchMovieListLoadState()

        // Assert
        assertEquals(SearchMovieListLoadState.Idle, state)
    }

    @Test
    fun toSearchMovieListLoadState_withError_returnsErrorMessage() {
        // Act
        val state = LoadState.Error(IllegalStateException("Network unavailable")).toSearchMovieListLoadState()

        // Assert
        assertIs<SearchMovieListLoadState.Error>(state)
        assertEquals("Network unavailable", state.message)
    }
}
