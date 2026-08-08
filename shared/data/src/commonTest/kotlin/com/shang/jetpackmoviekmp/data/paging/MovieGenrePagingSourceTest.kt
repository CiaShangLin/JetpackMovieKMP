package com.shang.jetpackmoviekmp.data.paging

import androidx.paging.PagingSource
import com.shang.jetpackmoviekmp.data.repository.FakeMovieDataSource
import com.shang.jetpackmoviekmp.data.repository.failureResponse
import com.shang.jetpackmoviekmp.model.MovieCardResult
import com.shang.jetpackmoviekmp.model.MovieListBean
import com.shang.jetpackmoviekmp.network.model.NetworkResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class MovieGenrePagingSourceTest {

    private fun refreshParams(page: Int?) = PagingSource.LoadParams.Refresh(
        key = page,
        loadSize = 20,
        placeholdersEnabled = false,
    )

    @Test
    fun load_returns_page_with_next_key_when_more_pages_remain() = runTest {
        val dataSource = FakeMovieDataSource().apply {
            discoverMovieResponse = NetworkResponse(
                code = 200,
                data = MovieListBean(
                    page = 1,
                    results = listOf(MovieCardResult(id = 1, title = "A")),
                    totalPages = 3,
                ),
            )
        }
        val pagingSource = MovieGenrePagingSource(dataSource, withGenres = "28")

        val result = pagingSource.load(refreshParams(page = 1))

        val page = assertIs<PagingSource.LoadResult.Page<Int, MovieCardResult>>(result)
        assertEquals(listOf(1), page.data.map { it.id })
        assertNull(page.prevKey)
        assertEquals(2, page.nextKey)
    }

    @Test
    fun load_returns_null_nextKey_on_last_page() = runTest {
        val dataSource = FakeMovieDataSource().apply {
            discoverMovieResponse = NetworkResponse(
                code = 200,
                data = MovieListBean(page = 3, results = emptyList(), totalPages = 3),
            )
        }
        val pagingSource = MovieGenrePagingSource(dataSource, withGenres = "28")

        val result = pagingSource.load(refreshParams(page = 3))

        val page = assertIs<PagingSource.LoadResult.Page<Int, MovieCardResult>>(result)
        assertNull(page.nextKey)
        assertEquals(2, page.prevKey)
    }

    @Test
    fun load_returns_error_when_response_fails() = runTest {
        val dataSource = FakeMovieDataSource().apply {
            discoverMovieResponse = NetworkResponse(code = 500, error = failureResponse())
        }
        val pagingSource = MovieGenrePagingSource(dataSource, withGenres = "28")

        val result = pagingSource.load(refreshParams(page = 1))

        assertIs<PagingSource.LoadResult.Error<Int, MovieCardResult>>(result)
    }

    @Test
    fun load_returns_null_nextKey_when_totalPages_is_zero() = runTest {
        // totalPages = 0 表示沒有任何結果；若沿用「page == totalPages 才停止」的舊邏輯，
        // page(1) != totalPages(0) 會誤判成「還有下一頁」，造成無限分頁載入。
        val dataSource = FakeMovieDataSource().apply {
            discoverMovieResponse = NetworkResponse(
                code = 200,
                data = MovieListBean(page = 1, results = emptyList(), totalPages = 0),
            )
        }
        val pagingSource = MovieGenrePagingSource(dataSource, withGenres = "28")

        val result = pagingSource.load(refreshParams(page = 1))

        val page = assertIs<PagingSource.LoadResult.Page<Int, MovieCardResult>>(result)
        assertNull(page.nextKey)
    }

    @Test
    fun load_filters_out_ids_already_returned_by_previous_page() = runTest {
        val dataSource = object : FakeMovieDataSource() {
            override suspend fun getDiscoverMovie(withGenres: String, page: Int) =
                if (page == 1) {
                    NetworkResponse(
                        code = 200,
                        data = MovieListBean(
                            page = 1,
                            results = listOf(MovieCardResult(id = 1, title = "A"), MovieCardResult(id = 2, title = "B")),
                            totalPages = 3,
                        ),
                    )
                } else {
                    NetworkResponse(
                        code = 200,
                        data = MovieListBean(
                            page = 2,
                            results = listOf(MovieCardResult(id = 2, title = "B"), MovieCardResult(id = 3, title = "C")),
                            totalPages = 3,
                        ),
                    )
                }
        }
        val pagingSource = MovieGenrePagingSource(dataSource, withGenres = "28")

        pagingSource.load(refreshParams(page = 1))
        val secondResult = pagingSource.load(refreshParams(page = 2))

        val page = assertIs<PagingSource.LoadResult.Page<Int, MovieCardResult>>(secondResult)
        assertEquals(listOf(3), page.data.map { it.id })
    }

    @Test
    fun load_keeps_nextKey_from_totalPages_when_all_items_are_deduped() = runTest {
        val dataSource = object : FakeMovieDataSource() {
            override suspend fun getDiscoverMovie(withGenres: String, page: Int) =
                if (page == 1) {
                    NetworkResponse(
                        code = 200,
                        data = MovieListBean(page = 1, results = listOf(MovieCardResult(id = 1, title = "A")), totalPages = 3),
                    )
                } else {
                    NetworkResponse(
                        code = 200,
                        data = MovieListBean(page = 2, results = listOf(MovieCardResult(id = 1, title = "A")), totalPages = 3),
                    )
                }
        }
        val pagingSource = MovieGenrePagingSource(dataSource, withGenres = "28")

        pagingSource.load(refreshParams(page = 1))
        val secondResult = pagingSource.load(refreshParams(page = 2))

        val page = assertIs<PagingSource.LoadResult.Page<Int, MovieCardResult>>(secondResult)
        assertEquals(emptyList(), page.data)
        assertEquals(3, page.nextKey)
    }

    @Test
    fun load_does_not_treat_ids_from_a_different_instance_as_duplicates() = runTest {
        val dataSource = FakeMovieDataSource().apply {
            discoverMovieResponse = NetworkResponse(
                code = 200,
                data = MovieListBean(page = 1, results = listOf(MovieCardResult(id = 1, title = "A")), totalPages = 3),
            )
        }
        val firstPagingSource = MovieGenrePagingSource(dataSource, withGenres = "28")
        firstPagingSource.load(refreshParams(page = 1))

        val secondPagingSource = MovieGenrePagingSource(dataSource, withGenres = "28")
        val result = secondPagingSource.load(refreshParams(page = 1))

        val page = assertIs<PagingSource.LoadResult.Page<Int, MovieCardResult>>(result)
        assertEquals(listOf(1), page.data.map { it.id })
    }

    @Test
    fun load_rethrows_cancellationException_instead_of_wrapping_as_error() = runTest {
        val cancellingDataSource = object : FakeMovieDataSource() {
            override suspend fun getDiscoverMovie(withGenres: String, page: Int) =
                throw CancellationException("cancelled")
        }
        val pagingSource = MovieGenrePagingSource(cancellingDataSource, withGenres = "28")

        assertFailsWith<CancellationException> {
            pagingSource.load(refreshParams(page = 1))
        }
    }
}
