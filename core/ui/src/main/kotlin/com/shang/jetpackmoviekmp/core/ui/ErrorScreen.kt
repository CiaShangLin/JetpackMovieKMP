package com.shang.jetpackmoviekmp.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shang.jetpackmoviekmp.common.AppError
import com.shang.jetpackmoviekmp.common.NetworkException

@Composable
fun ErrorScreen(
    modifier: Modifier = Modifier,
    throwable: Throwable? = null,
    onRetry: () -> Unit = {},
    errorText: Int = throwable?.toErrorText() ?: R.string.default_error_text,
    retryText: Int = R.string.retry_button_text,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(errorText),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text(stringResource(retryText))
        }
    }
}

/**
 * 將 domain/common 層錯誤轉換成 Android UI 字串資源。
 *
 * @return 對應的錯誤訊息字串資源 ID。
 */
fun Throwable.toErrorText(): Int {
    return when (this) {
        is AppError.Network -> exception.toErrorText()
        is AppError.Unknown -> R.string.default_error_text
        is NetworkException.HttpError -> when (httpCode) {
            in 400..499 -> R.string.server_error_text
            in 500..599 -> R.string.server_error_text
            else -> R.string.default_error_text
        }
        is NetworkException.ConnectionError -> R.string.network_error_text
        is NetworkException.TimeoutError -> R.string.timeout_error_text
        is NetworkException.ParseError -> R.string.server_error_text
        is NetworkException.UnknownError -> R.string.unknown_error_text
        else -> R.string.default_error_text
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorScreenPreview() {
    ErrorScreen(onRetry = { })
}
