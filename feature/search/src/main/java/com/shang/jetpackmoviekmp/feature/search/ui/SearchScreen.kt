package com.shang.jetpackmoviekmp.feature.search.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.shang.jetpackmoviekmp.core.designsystem.component.JMLazyVerticalGrid
import com.shang.jetpackmoviekmp.core.ui.ErrorScreen
import com.shang.jetpackmoviekmp.core.ui.LoadingScreen
import com.shang.jetpackmoviekmp.core.ui.MovieCard
import com.shang.jetpackmoviekmp.feature.search.R
import com.shang.jetpackmoviekmp.model.MovieCardData
import com.shang.jetpackmoviekmp.model.MovieCardResult
import com.shang.jetpackmoviekmp.model.asMovieCardData
import org.koin.compose.viewmodel.koinViewModel

/**
 * 顯示電影搜尋輸入、搜尋結果及其 Paging 載入狀態。
 *
 * @param viewModel 提供搜尋狀態與收藏操作的 ViewModel。
 * @param onMovieClick 使用者點擊搜尋結果電影卡片時的回呼。
 */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = koinViewModel(),
    onMovieClick: (MovieCardData) -> Unit,
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val movieSearchPager = viewModel.movieSearchPager.collectAsLazyPagingItems()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.fillMaxSize()) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            value = inputText,
            onValueChange = { inputText = it },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    viewModel.startSearch(inputText)
                },
            ),
            label = { Text(stringResource(R.string.search_movie_hint)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.search_movie_hint),
                )
            },
            shape = MaterialTheme.shapes.large,
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurface,
        )

        when {
            searchQuery.isEmpty() -> SearchInitialScreen()
            movieSearchPager.loadState.refresh is LoadState.Loading -> SearchLoadingScreen()
            movieSearchPager.loadState.refresh is LoadState.Error -> {
                SearchErrorScreen(onRetry = viewModel::retrySearch)
            }

            else -> SearchResultScreen(
                movieSearchPager,
                onMovieClick = onMovieClick,
                onCollectClick = {
                    viewModel.toggleMovieCollectStatus(it)
                },
            )
        }
    }
}

/** 顯示尚未提交搜尋時的引導內容。 */
@Composable
private fun SearchInitialScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = stringResource(R.string.search_movie_hint),
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Text(
            text = stringResource(R.string.search_movie_start),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            text = stringResource(R.string.search_movie_input_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** 顯示首次搜尋的載入狀態。 */
@Composable
private fun SearchLoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingScreen()
    }
}

/** 顯示首次搜尋錯誤與重試操作。 */
@Composable
private fun SearchErrorScreen(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ErrorScreen(onRetry = onRetry)
    }
}

/** 顯示搜尋結果及清單尾端的 Paging 狀態。 */
@Composable
private fun SearchResultScreen(
    movieSearchPager: LazyPagingItems<MovieCardResult>,
    onMovieClick: (MovieCardData) -> Unit,
    onCollectClick: (MovieCardData) -> Unit,
) {
    JMLazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(movieSearchPager.itemCount) { index ->
            val movie = movieSearchPager[index] ?: return@items
            MovieCard(
                data = movie.asMovieCardData(),
                onMovieClick = onMovieClick,
                onCollectClick = onCollectClick,
            )
        }
        item {
            when (movieSearchPager.loadState.append) {
                is LoadState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingScreen()
                    }
                }

                is LoadState.Error -> {
                    ErrorScreen(onRetry = movieSearchPager::retry)
                }

                is LoadState.NotLoading -> {
                    if (movieSearchPager.loadState.append.endOfPaginationReached) {
                        Text(
                            text = stringResource(R.string.search_movie_no_more),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
