package com.shang.jetpackmoviekmp.feature.search.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.shang.jetpackmoviekmp.data.repository.MovieRepository
import com.shang.jetpackmoviekmp.domain.usecase.GetSearchMovieListUseCase
import com.shang.jetpackmoviekmp.model.MovieCardData
import com.shang.jetpackmoviekmp.model.asMovieCardResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 管理電影搜尋關鍵字、分頁結果、收藏切換與重試操作。
 *
 * @property movieRepository 提供收藏資料的新增與刪除操作。
 * @property getSearchMovieListUseCase 建立會同步收藏狀態的搜尋分頁資料流。
 */
class SearchViewModel(
    private val movieRepository: MovieRepository,
    private val getSearchMovieListUseCase: GetSearchMovieListUseCase,
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
                    .flatMapLatest { searchQuery ->
                        getSearchMovieListUseCase(
                            query = searchQuery,
                            scope = viewModelScope,
                        )
                    }
            }
        }

    /** 提交 [query] 並開始搜尋；空白字串會回到初始狀態。 */
    fun startSearch(query: String) {
        mutableSearchQuery.value = query.trim()
    }

    /** 重新載入目前已提交的搜尋關鍵字。 */
    fun retrySearch() {
        retryTrigger.value++
    }

    /**
     * 依電影目前的收藏狀態新增或刪除收藏資料。
     *
     * @param data 使用者操作的電影卡片資料。
     */
    fun toggleMovieCollectStatus(data: MovieCardData) {
        viewModelScope.launch {
            if (data.movieCardIsCollect) {
                movieRepository.deleteMovieCollect(data.asMovieCardResult())
            } else {
                movieRepository.insertMovieCollect(data.asMovieCardResult())
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
