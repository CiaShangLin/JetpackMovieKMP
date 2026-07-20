package com.shang.jetpackmoviekmp.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter.Companion.DefaultTransform
import coil3.compose.AsyncImagePainter.State
import com.shang.jetpackmoviekmp.core.designsystem.R

@Composable
fun JMAsyncImage(
    model: Any?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    transform: (State) -> State = DefaultTransform,
    onState: ((State) -> Unit)? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.FillBounds,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DefaultFilterQuality,
    clipToBounds: Boolean = true,
    loadingContent: @Composable () -> Unit = { DefaultLoadingImage() },
    errorContent: @Composable () -> Unit = { DefaultErrorImage() },
) {
    var imageState: State by remember(model) { mutableStateOf(State.Empty) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            transform = transform,
            onState = { newState ->
                imageState = newState
                onState?.invoke(newState)
            },
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
            filterQuality = filterQuality,
            clipToBounds = clipToBounds,
        )

        AnimatedVisibility(
            visible = imageState.isLoading(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            loadingContent()
        }

        AnimatedVisibility(
            visible = imageState is State.Error,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            errorContent()
        }
    }
}

private fun State.isLoading(): Boolean = this is State.Empty || this is State.Loading

@Composable
fun DefaultLoadingImage(
    modifier: Modifier = Modifier,
    size: Int = 80,
) {
    Image(
        painter = painterResource(id = R.drawable.icon_movie_card_loading),
        contentDescription = "載入中",
        modifier = modifier.size(size.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun DefaultErrorImage(
    modifier: Modifier = Modifier,
    size: Int = 80,
) {
    Image(
        painter = painterResource(id = R.drawable.icon_movie_card_error),
        contentDescription = "載入失敗",
        modifier = modifier.size(size.dp),
        contentScale = ContentScale.Fit,
    )
}

@Preview(showBackground = true)
@Composable
private fun JMAsyncImagePreview() {
    JMAsyncImage(
        model = "https://example.com/image.jpg",
        contentDescription = "示例圖片",
        modifier = Modifier.size(200.dp),
    )
}
