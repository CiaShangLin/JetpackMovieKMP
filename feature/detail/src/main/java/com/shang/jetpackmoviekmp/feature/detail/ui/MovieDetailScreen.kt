package com.shang.jetpackmoviekmp.feature.detail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shang.jetpackmoviekmp.core.designsystem.component.JMAsyncImage
import com.shang.jetpackmoviekmp.core.designsystem.theme.StarRatingColor
import com.shang.jetpackmoviekmp.core.ui.ErrorScreen
import com.shang.jetpackmoviekmp.core.ui.LoadingScreen
import com.shang.jetpackmoviekmp.core.ui.MovieActor
import com.shang.jetpackmoviekmp.core.ui.MovieCard
import com.shang.jetpackmoviekmp.feature.detail.R
import com.shang.jetpackmoviekmp.model.MovieCardData
import com.shang.jetpackmoviekmp.model.MovieCastAndCrewBean
import com.shang.jetpackmoviekmp.model.MovieDetailBean
import com.shang.jetpackmoviekmp.model.asMovieCardData
import com.shang.jetpackmoviekmp.model.asMovieCardResult
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 顯示電影詳情、收藏操作、演員頭像與推薦電影。
 *
 * @param movieId 要顯示的電影識別碼。
 * @param onBackClick 使用者點選返回時的回呼。
 * @param onMovieClick 使用者點選推薦電影時的回呼。
 * @param viewModel 提供頁面狀態與操作的 ViewModel。
 */
@Composable
fun MovieDetailScreen(
    movieId: Int,
    onBackClick: () -> Unit,
    onMovieClick: (MovieCardData) -> Unit,
    viewModel: MovieDetailViewModel = koinViewModel(
        key = "MovieDetailViewModel_$movieId",
        parameters = { parametersOf(movieId) },
    ),
) {
    val movieDetail by viewModel.movieDetail.collectAsState()
    val movieCollect by viewModel.movieCollect.collectAsState()
    val movieRecommendations by viewModel.movieRecommendations.collectAsState()
    val movieActors by viewModel.movieActors.collectAsState()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val state = movieDetail) {
            MovieDetailUiState.Loading -> LoadingScreen()
            is MovieDetailUiState.Error -> ErrorScreen(
                throwable = state.throwable,
                onRetry = viewModel::retryMovieDetail,
            )
            is MovieDetailUiState.Success -> MovieDetailContent(
                movie = state.movie,
                isCollect = movieCollect,
                movieActors = movieActors,
                movieRecommendations = movieRecommendations,
                onMovieClick = onMovieClick,
                onCollectClick = viewModel::toggleCollect,
            )
        }

        DetailIconButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 8.dp),
            onClick = onBackClick,
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
        }

        if (movieDetail is MovieDetailUiState.Success) {
            val movie = (movieDetail as MovieDetailUiState.Success).movie
            DetailIconButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 8.dp),
                onClick = {
                    viewModel.toggleCollect(movie.asMovieCardResult().asMovieCardData().copy(movieCardIsCollect = movieCollect))
                },
            ) {
                Icon(
                    imageVector = if (movieCollect) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun DetailIconButton(
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun MovieDetailContent(
    movie: MovieDetailBean,
    isCollect: Boolean,
    movieActors: DetailSectionState<List<MovieCastAndCrewBean.Cast>>,
    movieRecommendations: DetailSectionState<List<com.shang.jetpackmoviekmp.model.MovieCardResult>>,
    onMovieClick: (MovieCardData) -> Unit,
    onCollectClick: (MovieCardData) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        JMAsyncImage(
            model = movie.backdropPath,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        )
        LazyColumn {
            item { MovieSummary(movie) }
            item { MovieOverview(movie.overview) }
            item { MovieActors(movieActors) }
            item {
                MovieRecommendations(
                    movieRecommendations = movieRecommendations,
                    onMovieClick = onMovieClick,
                    onCollectClick = onCollectClick,
                )
            }
        }
    }
}

@Composable
private fun MovieSummary(movie: MovieDetailBean) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(text = movie.title, style = MaterialTheme.typography.headlineLarge)
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DetailMetadata(Icons.Rounded.StarRate, String.format("%.1f", movie.voteAverage), StarRatingColor)
            DetailMetadata(Icons.Rounded.CalendarMonth, movie.releaseDate)
            DetailMetadata(Icons.Rounded.Timer, movie.runtime.toString())
        }
    }
}

@Composable
private fun DetailMetadata(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = tint)
        Spacer(modifier = Modifier.width(2.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MovieOverview(overview: String) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)) {
        Text(stringResource(R.string.movie_detail_overview), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(overview, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MovieActors(state: DetailSectionState<List<MovieCastAndCrewBean.Cast>>) {
    when (state) {
        DetailSectionState.Loading -> LoadingScreen()
        is DetailSectionState.Error -> Unit
        is DetailSectionState.Success -> if (state.data.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.movie_detail_main_cast), style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.data, key = { it.creditId }) { actor ->
                        MovieActor(
                            model = actor.profilePath,
                            modifier = Modifier.size(96.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieRecommendations(
    movieRecommendations: DetailSectionState<List<com.shang.jetpackmoviekmp.model.MovieCardResult>>,
    onMovieClick: (MovieCardData) -> Unit,
    onCollectClick: (MovieCardData) -> Unit,
) {
    when (movieRecommendations) {
        DetailSectionState.Loading -> LoadingScreen()
        is DetailSectionState.Error -> Unit
        is DetailSectionState.Success -> if (movieRecommendations.data.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.movie_detail_recommendations), style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(movieRecommendations.data, key = { it.id }) { recommendation ->
                        MovieCard(
                            data = recommendation.asMovieCardData(),
                            modifier = Modifier.width(180.dp),
                            onMovieClick = onMovieClick,
                            onCollectClick = onCollectClick,
                        )
                    }
                }
            }
        }
    }
}
