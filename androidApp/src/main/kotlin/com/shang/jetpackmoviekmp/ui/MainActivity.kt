package com.shang.jetpackmoviekmp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.shang.jetpackmoviekmp.core.designsystem.component.JMBackground
import com.shang.jetpackmoviekmp.core.designsystem.component.JMNavigationSuiteScaffold
import com.shang.jetpackmoviekmp.core.designsystem.theme.JetpackMovieComposeTheme
import com.shang.jetpackmoviekmp.core.designsystem.theme.OnBackground
import com.shang.jetpackmoviekmp.core.designsystem.theme.Primary
import com.shang.jetpackmoviekmp.core.designsystem.theme.PrimaryContainer
import com.shang.jetpackmoviekmp.core.designsystem.theme.SurfaceVariant
import com.shang.jetpackmoviekmp.core.ui.ErrorScreen
import com.shang.jetpackmoviekmp.core.ui.LoadingScreen
import com.shang.jetpackmoviekmp.feature.home.navigation.HomeKey
import com.shang.jetpackmoviekmp.feature.home.navigation.homeEntry
import com.shang.jetpackmoviekmp.model.LanguageMode
import com.shang.jetpackmoviekmp.model.ThemeMode
import com.shang.jetpackmoviekmp.navigation.MainNavItem
import com.shang.jetpackmoviekmp.utils.LanguageSettingUtils
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

/**
 * feature module 導入前的暫時佔位畫面，作為 [rememberNavBackStack] 的 start key。
 */
@Serializable
private data object PlaceholderKey : NavKey

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val splashScreen = installSplashScreen()

        setContent {
            val viewModel = koinViewModel<MainViewModel>()
            val configuration = viewModel.configuration.collectAsState()
            val userData by viewModel.userData.collectAsState()
            splashScreen.setKeepOnScreenCondition {
                configuration.value is MainUiState.Loading
            }
            LaunchedEffect(userData.languageMode) {
                LanguageSettingUtils.updateActivityLocale(
                    activity = this@MainActivity,
                    languageMode = userData.languageMode,
                )
            }

            LanguageProvider(languageMode = userData.languageMode) {
                val backStack = rememberNavBackStack(PlaceholderKey)
                ThemeProvider(
                    themeMode = userData.themeMode,
                    activity = this@MainActivity,
                ) {
                    MainScreen(configuration.value, backStack, onRetry = {
                        viewModel.retryConfiguration()
                    })
                }
            }
        }
    }
}

@Composable
private fun LanguageProvider(
    languageMode: LanguageMode,
    content: @Composable () -> Unit,
) {
    key(languageMode) {
        content()
    }
}

@Composable
private fun ThemeProvider(
    themeMode: ThemeMode,
    activity: ComponentActivity,
    content: @Composable () -> Unit,
) {
    val isSystemDarkTheme = isSystemInDarkTheme()

    val isDarkTheme = remember(themeMode, isSystemDarkTheme) {
        when (themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> isSystemDarkTheme
        }
    }

    // 根據主題動態設置系統欄顏色
    LaunchedEffect(isDarkTheme) {
        if (isDarkTheme) {
            // 暗色主題：使用深色背景色調
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(
                    Color(0xFF101217).toArgb(), // DarkBackground
                ),
                navigationBarStyle = SystemBarStyle.dark(
                    Color(0xFF181A20).toArgb(), // DarkSurface
                ),
            )
        } else {
            // 亮色主題：使用主色調
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(
                    PrimaryContainer.toArgb(), // PrimaryContainer
                    Primary.toArgb(), // Primary (暗色模式下的狀態欄)
                ),
                navigationBarStyle = SystemBarStyle.light(
                    SurfaceVariant.toArgb(), // SurfaceVariant
                    OnBackground.toArgb(), // OnBackground (暗色模式下的導覽欄)
                ),
            )
        }
    }

    JetpackMovieComposeTheme(darkTheme = isDarkTheme) {
        content()
    }
}

@Composable
fun MainScreen(mainUiState: MainUiState, backStack: NavBackStack<NavKey>, onRetry: () -> Unit) {
    JMBackground(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.statusBars.asPaddingValues()),
    ) {
        when (mainUiState) {
            is MainUiState.Loading -> {
                MainLoadingScreen()
            }

            is MainUiState.Error -> {
                MainErrorScreen(mainUiState.throwable as Exception?, onRetry = onRetry)
            }

            is MainUiState.Success -> {
                SuccessScreen(backStack = backStack)
            }
        }
    }
}

@Composable
fun MainLoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingScreen()
    }
}

@Composable
fun MainErrorScreen(exception: Exception?, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ErrorScreen(
            onRetry = onRetry,
            throwable = exception,
        )
    }
}

@Composable
fun SuccessScreen(backStack: NavBackStack<NavKey>) {
    val currentKey = backStack.lastOrNull()

    JMNavigationSuiteScaffold(
        navigationSuiteItems = {
            MainNavItem.entries.forEach { item ->
                item(
                    selected = currentKey == item.key,
                    onClick = {
                        if (currentKey != item.key) {
                            backStack.removeLastOrNull()
                            backStack.add(item.key)
                        }
                    },
                    icon = {
                        Icon(
                            item.unselectedIcon,
                            contentDescription = stringResource(item.iconTextId),
                        )
                    },
                    selectedIcon = {
                        Icon(
                            item.selectedIcon,
                            contentDescription = stringResource(item.iconTextId),
                        )
                    },
                    label = {
                        Text(stringResource(item.titleTextId))
                    },
                )
            }
        },
    ) {
        // 待各分頁 feature module 導入後再依 MainNavItem 補上對應的 NavEntry，
        // 尚未導入的分頁一律回退到 PlaceholderScreen。
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = { navKey ->
                when (navKey) {
                    HomeKey -> homeEntry(onMovieClick = {}).second
                    else -> NavEntry(navKey) { PlaceholderScreen() }
                }
            },
        )
    }
}

@Composable
private fun PlaceholderScreen() {
    Box(modifier = Modifier.fillMaxSize())
}
