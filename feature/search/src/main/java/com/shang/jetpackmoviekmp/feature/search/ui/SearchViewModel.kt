package com.shang.jetpackmoviekmp.feature.search.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** 管理電影搜尋關鍵字、分頁結果與重試操作。 */
class SearchViewModel(
    private val movieRepository: MovieRepository,
) : ViewModel() {

    private val mutableSearchQuery = MutableStateFlow("")
    private val retryTrigger = MutableStateFlow(0)

    /** 目前已提交的搜尋關鍵字。 */
    val searchQuery = mutableSearchQuery.asStateFlow()

    /** 目前關鍵字對應的電影搜尋分頁資料。 */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val movieSearchPager = searchQuery
        .debounce(SEARCH_DEBOUNCE_MILLIS)
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                flowOf(PagingData.empty())
            } else {
                retryTrigger
                    .map { query }
                    .flatMapLatest(movieRepository::getMovieSearchPager)
            }
        }
        .cachedIn(viewModelScope)

    /** 提交 [query] 並開始搜尋；空白字串會回到初始狀態。 */
    fun startSearch(query: String) {
        mutableSearchQuery.value = query.trim()
    }

    /** 重新載入目前已提交的搜尋關鍵字。 */
    fun retrySearch() {
        retryTrigger.value++
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
