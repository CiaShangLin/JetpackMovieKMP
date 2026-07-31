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

/**
 * 顯示圓形演員頭像，圖片路徑缺失或載入失敗時改用預設圖示。
 *
 * @param model Coil 可識別的圖片模型；空白字串會視為沒有圖片。
 * @param modifier 套用至頭像容器的 Compose 修飾器。
 */
@Composable
fun MovieActor(
    model: Any?,
    modifier: Modifier = Modifier,
) {
    JMAsyncImage(
        model = model.asActorImageModel(),
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
            LoadingImage(
                modifier = modifier
                    .clip(CircleShape),
            )
        },
    )
}

/** 將空白圖片路徑轉換為 `null`，使元件顯示預設頭像。 */
internal fun Any?.asActorImageModel(): Any? =
    if (this is String && isBlank()) null else this

@Composable
private fun LoadingImage(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.icon_actor_placeholder),
        contentDescription = "載入中",
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
