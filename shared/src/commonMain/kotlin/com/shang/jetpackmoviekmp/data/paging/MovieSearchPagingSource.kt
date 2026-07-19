package com.shang.jetpackmoviekmp.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shang.jetpackmoviekmp.model.MovieCardResult
import com.shang.jetpackmoviekmp.network.datasource.MovieDataSource
import kotlinx.coroutines.CancellationException

/**
 * 依關鍵字分頁搜尋電影。
 *
 * @property movieDataSource 提供 `getMovieSearch` 呼叫的網路資料來源。
 * @property query 搜尋關鍵字，對應 TMDB `query` 查詢參數。
 */
class MovieSearchPagingSource(
    private val movieDataSource: MovieDataSource,
    private val query: String,
) : PagingSource<Int, MovieCardResult>() {

    /**
     * 依目前錨點位置推算 refresh 時應使用的頁碼。
     *
     * @param state 目前的 paging 狀態。
     * @return 推算出的頁碼；若無法推算則回傳 null。
     */
    override fun getRefreshKey(state: PagingState<Int, MovieCardResult>): Int? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
    }

    /**
     * 載入指定頁碼的搜尋結果。
     *
     * Cancellation 會被重新拋出，維持 coroutine cooperative cancellation。
     *
     * @param params 載入參數，`key` 為 null 時預設載入第 1 頁。
     * @return 成功時回傳 [LoadResult.Page]（含 `prevKey`／`nextKey`）；
     *   失敗（network 回應失敗或例外）時回傳 [LoadResult.Error]。
     */
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MovieCardResult> {
        return try {
            val page = params.key ?: 1
            val response =
                movieDataSource.getMovieSearch(query = query, page = params.key ?: 1)

            if (response.isSuccess) {
                val totalPages = response.data?.totalPages ?: page
                val prevKey = if (page == 1) {
                    null
                } else {
                    page - 1
                }
                val nextKey = if (page >= totalPages) {
                    null
                } else {
                    page + 1
                }
                LoadResult.Page(
                    data = response.data?.results ?: emptyList(),
                    prevKey = prevKey,
                    nextKey = nextKey,
                )
            } else {
                LoadResult.Error(
                    response.error ?: Exception("Unknown error occurred"),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
