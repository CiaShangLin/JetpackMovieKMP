package com.shang.jetpackmoviekmp.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.shang.jetpackmoviekmp.core.designsystem.component.JMAsyncImage

@Composable
fun MovieActor(
    model: Any,
    modifier: Modifier = Modifier,
) {
    JMAsyncImage(
        model = model,
        contentDescription = null,
        modifier = modifier
            .clip(CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
            .padding(2.dp),
        clipToBounds = true,
        loadingContent = {
            LoadingImage(
                modifier = modifier
                    .clip(CircleShape),
            )
        },
        errorContent = {
            LoadingImage()
        },
    )
}

@Composable
private fun LoadingImage(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.icon_actor_placeholder),
        contentDescription = "載入中",
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
