package com.shang.jetpackmoviekmp.data.paging

import androidx.paging.PagingSource
import com.shang.jetpackmoviekmp.common.NetworkException
import com.shang.jetpackmoviekmp.data.repository.FakeMovieDataSource
import com.shang.jetpackmoviekmp.model.MovieCardResult
import com.shang.jetpackmoviekmp.model.MovieSearchBean
import com.shang.jetpackmoviekmp.network.model.NetworkResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class MovieSearchPagingSourceTest {

    private fun refreshParams(page: Int?) = PagingSource.LoadParams.Refresh(
        key = page,
        loadSize = 20,
        placeholdersEnabled = false,
    )

    @Test
    fun load_returns_page_matching_query_results() = runTest {
        val dataSource = FakeMovieDataSource().apply {
            movieSearchResponse = NetworkResponse(
                code = 200,
                data = MovieSearchBean(
                    page = 1,
                    results = listOf(MovieCardResult(id = 9, title = "Found")),
                    totalPages = 1,
                ),
            )
        }
        val pagingSource = MovieSearchPagingSource(dataSource, query = "matrix")

        val result = pagingSource.load(refreshParams(page = 1))

        val page = assertIs<PagingSource.LoadResult.Page<Int, MovieCardResult>>(result)
        assertEquals(listOf(9), page.data.map { it.id })
    }

    @Test
    fun load_returns_error_uses_networkException_directly_not_its_cause() = runTest {
        // HttpError.cause is always null; falling back to it would swallow the real error
        // (see MovieSearchPagingSource fix note in tasks.md 2.3).
        val httpError = NetworkException.HttpError(httpCode = 404)
        val dataSource = FakeMovieDataSource().apply {
            movieSearchResponse = NetworkResponse(code = 404, error = httpError)
        }
        val pagingSource = MovieSearchPagingSource(dataSource, query = "matrix")

        val result = pagingSource.load(refreshParams(page = 1))

        val error = assertIs<PagingSource.LoadResult.Error<Int, MovieCardResult>>(result)
        assertEquals(httpError, error.throwable)
    }

    @Test
    fun load_returns_null_nextKey_when_totalPages_is_zero() = runTest {
        // totalPages = 0 表示沒有任何搜尋結果；若沿用「page == totalPages 才停止」的舊邏輯，
        // page(1) != totalPages(0) 會誤判成「還有下一頁」，造成無限分頁載入。
        val dataSource = FakeMovieDataSource().apply {
            movieSearchResponse = NetworkResponse(
                code = 200,
                data = MovieSearchBean(page = 1, results = emptyList(), totalPages = 0),
            )
        }
        val pagingSource = MovieSearchPagingSource(dataSource, query = "no-such-movie")

        val result = pagingSource.load(refreshParams(page = 1))

        val page = assertIs<PagingSource.LoadResult.Page<Int, MovieCardResult>>(result)
        assertNull(page.nextKey)
    }

    @Test
    fun load_rethrows_cancellationException_instead_of_wrapping_as_error() = runTest {
        val cancellingDataSource = object : FakeMovieDataSource() {
            override suspend fun getMovieSearch(query: String, page: Int) =
                throw CancellationException("cancelled")
        }
        val pagingSource = MovieSearchPagingSource(cancellingDataSource, query = "matrix")

        assertFailsWith<CancellationException> {
            pagingSource.load(refreshParams(page = 1))
        }
    }
}
