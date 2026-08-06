package com.shang.jetpackmoviekmp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.request.ImageRequest
import com.shang.jetpackmoviekmp.core.designsystem.component.JMAsyncImage
import com.shang.jetpackmoviekmp.core.designsystem.theme.StarRatingColor
import com.shang.jetpackmoviekmp.core.ui.coil.TmdbImageSize
import com.shang.jetpackmoviekmp.model.MovieCardData

/** Preview 使用的示範圖片 URL。 */
private const val DEMO_URL =
    "https://fastly.picsum.photos/id/1020/400/300.jpg?hmac=tyq3V0QObhO4gvke1hMd7uZOQ2Sd5LwaQYB9zLBdi2w"

@Composable
fun MovieCard(
    data: MovieCardData,
    modifier: Modifier = Modifier,
    onMovieClick: (MovieCardData) -> Unit = {},
    onCollectClick: (MovieCardData) -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = MaterialTheme.shapes.medium,
            )
            .movieCardSurface(
                shape = MaterialTheme.shapes.medium,
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.onSurface,
                borderWidth = 1.dp,
            )
            .clickable {
                onMovieClick(data)
            },
    ) {
        Column {
            Box {
                MovieCover(model = data.movieCardPosterPath)
                MovieRating(
                    modifier = Modifier
                        .padding(bottom = 8.dp, start = 8.dp)
                        .align(Alignment.BottomStart),
                    voteAverage = data.movieCardVoteAverage,
                )
                MovieCollectButton(
                    modifier = Modifier
                        .padding(top = 8.dp, end = 8.dp)
                        .align(Alignment.TopEnd),
                    isCollect = data.movieCardIsCollect,
                    onClick = {
                        onCollectClick(data)
                    },
                )
            }
            MovieTitle(data.movieCardTitle)
            MovieReleaseTitle(data.movieCardReleaseDate)
        }
    }
}

@Composable
fun MovieCover(model: Any) {
    JMAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(model)
            .apply { extras[TmdbImageSize.key] = TmdbImageSize.LIST_THUMBNAIL }
            .build(),
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(3f / 4f)
            .padding(start = 1.dp, end = 1.dp, top = 1.dp),
        contentScale = ContentScale.FillBounds,
    )
}

/**
 * 以單次繪製合併背景填色與邊框，取代 `background` + `border` 兩層 modifier，
 * 降低每個清單項目的 draw phase 呼叫次數；視覺輸出（顏色、圓角、邊框寬度）與原本一致。
 */
private fun Modifier.movieCardSurface(
    shape: Shape,
    backgroundColor: Color,
    borderColor: Color,
    borderWidth: Dp,
): Modifier = drawWithCache {
    val path = shape.createOutline(size, layoutDirection, this).toPath()
    val strokeWidthPx = borderWidth.toPx()
    onDrawBehind {
        drawPath(path = path, color = backgroundColor, style = Fill)
        drawPath(path = path, color = borderColor, style = Stroke(width = strokeWidthPx))
    }
}

/** 將 [Outline] 轉為可直接繪製的 [Path]，涵蓋矩形、圓角矩形與任意路徑三種形狀。 */
private fun Outline.toPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}

/**
 * 電影標題文字。
 *
 * 固定佔用 2 行垂直空間（`minLines` 與 `maxLines` 皆為 2），
 * 使同一格線中的卡片高度不因標題實際行數而參差不齊，行為對齊 iOS 端
 * `MovieCardView` 的 `.lineLimit(2, reservesSpace: true)`。
 *
 * @param title 電影標題文字，超過 2 行會被截斷。
 */
@Composable
fun MovieTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
        minLines = 2,
        maxLines = 2,
    )
}

@Composable
fun MovieReleaseTitle(releaseDate: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.CalendarToday,
            contentDescription = "Release Date",
            modifier = Modifier
                .size(24.dp)
                .padding(start = 8.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = releaseDate,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp),
            maxLines = 1,
        )
    }
}

@Composable
fun MovieRating(modifier: Modifier, voteAverage: Double) {
    Row(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = "Rating",
            modifier = Modifier.size(16.dp),
            tint = StarRatingColor,
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            String.format("%.1f", voteAverage),
            modifier = Modifier.padding(end = 4.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
fun MovieCollectButton(modifier: Modifier, isCollect: Boolean, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(32.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = if (isCollect) {
                Icons.Rounded.Favorite
            } else {
                Icons.Rounded.FavoriteBorder
            },
            contentDescription = "Collect",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Preview
@Composable
private fun MovieCardPreview() {
    MovieCard(
        data = MovieCardData(
            movieCardId = 1,
            movieCardTitle = "Sample Movie",
            movieCardPosterPath = DEMO_URL,
            movieCardReleaseDate = "2023-10-01",
            movieCardVoteAverage = 8.7,
            movieCardIsCollect = false,
            movieCardTimestamp = System.currentTimeMillis(),
        ),
    )
}
