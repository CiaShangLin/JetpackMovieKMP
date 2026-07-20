package com.shang.jetpackmoviekmp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * App 根 composable，暫時作為 repository + domain 整合的簡易驗證畫面。
 *
 * 啟動時透過 [AppDiagnostics] 呼叫一次 configuration 流程；並提供一顆 button，
 * 讓使用者在繁體中文／英文間切換語言，再重新呼叫一次 diagnostics，藉此驗證
 * app facade 已接通底層資料流程。此畫面刻意維持簡單，不是正式的 settings 畫面。
 *
 * @param appDiagnostics 用來驗證資料流程的 app facade，預設由 Koin 注入。
 */
@Composable
fun App(
    appDiagnostics: AppDiagnostics = koinInject(),
) {
    AppContent(
        onLoadConfiguration = { appDiagnostics.loadConfigurationSummary() },
        onSetLanguage = { appDiagnostics.setLanguage(useEnglish = it) },
    )
}

/** [App] 的實作內容，把 facade 行為抽成回呼，方便 Preview 使用假資料。 */
@Composable
private fun AppContent(
    onLoadConfiguration: suspend () -> String,
    onSetLanguage: suspend (useEnglish: Boolean) -> String,
) {
    var text by remember { mutableStateOf("Loading...") }
    var useEnglish by remember { mutableStateOf(false) }
    var currentLanguageMode by remember { mutableStateOf("TRADITIONAL_CHINESE") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        text = try {
            onLoadConfiguration()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Button(onClick = {
                    val nextUseEnglish = !useEnglish
                    scope.launch {
                        text = try {
                            val nextLanguageMode = onSetLanguage(nextUseEnglish)
                            useEnglish = nextUseEnglish
                            currentLanguageMode = nextLanguageMode
                            "language=$nextLanguageMode result=${onLoadConfiguration()}"
                        } catch (e: Exception) {
                            "language switch error=${e.message}"
                        }
                    }
                }) {
                    Text(text = "Toggle language (current: $currentLanguageMode)")
                }
                Text(text = text)
            }
        }
    }
}

@Preview
@Composable
private fun AppPreview() {
    AppContent(
        onLoadConfiguration = { "Success(null)" },
        onSetLanguage = { useEnglish ->
            if (useEnglish) {
                "ENGLISH"
            } else {
                "TRADITIONAL_CHINESE"
            }
        },
    )
}
