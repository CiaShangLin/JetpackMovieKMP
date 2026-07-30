package com.shang.jetpackmoviekmp.presenter

import androidx.paging.PagingData
import com.shang.jetpackmoviekmp.model.MovieCardResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchMoviePagingDataPresenterTest {

    @Test
    fun collectFrom_withMoviePagingData_exposesMovieInSnapshot() = runTest {
        // Arrange
        val movie = MovieCardResult(id = 1, title = "Dune")
        val presenter = SearchMoviePagingDataPresenter()

        // Act
        presenter.collectFrom(PagingData.from(listOf(movie)))

        // Assert
        assertEquals(listOf(movie), presenter.snapshot().items)
    }
}
