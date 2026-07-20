package com.shang.jetpackmoviekmp.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 提供 design system 預設背景色的全螢幕容器。
 *
 * @param modifier 套用在背景容器上的修飾符。
 * @param content 背景容器內要顯示的內容。
 */
@Composable
fun JMBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = { },
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        content.invoke()
    }
}
