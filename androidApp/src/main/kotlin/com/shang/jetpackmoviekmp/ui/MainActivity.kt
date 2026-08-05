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
import androidx.compose.runtime.movableContentOf
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
import com.shang.jetpackmoviekmp.feature.collect.di.collectModule
import com.shang.jetpackmoviekmp.feature.collect.navigation.CollectKey
import com.shang.jetpackmoviekmp.feature.collect.navigation.collectEntry
import com.shang.jetpackmoviekmp.feature.detail.di.detailModule
import com.shang.jetpackmoviekmp.feature.detail.navigation.MovieDetailKey
import com.shang.jetpackmoviekmp.feature.detail.navigation.movieDetailEntry
import com.shang.jetpackmoviekmp.feature.history.di.historyModule
import com.shang.jetpackmoviekmp.feature.history.navigation.HistoryKey
import com.shang.jetpackmoviekmp.feature.history.navigation.historyEntry
import com.shang.jetpackmoviekmp.feature.home.navigation.HomeKey
import com.shang.jetpackmoviekmp.feature.home.navigation.homeEntry
import com.shang.jetpackmoviekmp.feature.search.di.searchModule
import com.shang.jetpackmoviekmp.feature.search.navigation.SearchKey
import com.shang.jetpackmoviekmp.feature.search.navigation.searchEntry
import com.shang.jetpackmoviekmp.feature.setting.di.settingModule
import com.shang.jetpackmoviekmp.feature.setting.navigation.SettingKey
import com.shang.jetpackmoviekmp.feature.setting.navigation.settingEntry
import com.shang.jetpackmoviekmp.model.ThemeMode
import com.shang.jetpackmoviekmp.navigation.MainNavItem
import com.shang.jetpackmoviekmp.utils.LanguageSettingUtils
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import kotlin.reflect.KClass

private val loadedFeatureModuleKeys = mutableSetOf<KClass<out NavKey>>()

private fun loadFeatureModuleIfNeeded(navKey: NavKey) {
    val keyClass = navKey::class
    if (!loadedFeatureModuleKeys.add(keyClass)) return
    val module = when (navKey) {
        is CollectKey -> collectModule()
        is HistoryKey -> historyModule()
        is SearchKey -> searchModule()
        is SettingKey -> settingModule()
        is MovieDetailKey -> detailModule()
        else -> return
    }
    loadKoinModules(listOf(module))
}

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
            // 同步呼叫（而非 LaunchedEffect），確保 resources.updateConfiguration() 在下方
            // key(languageMode) 觸發的重組之前就完成，避免字串讀到套用前的舊語言。
            // 手動測試切換點：改呼叫 LanguageSettingUtils.setApplicationLocales(userData.languageMode)
            // 可比較 AppCompatDelegate 版本的行為，本次僅新增測試方法，預設仍維持 updateActivityLocale。
            remember(userData.languageMode) {
                LanguageSettingUtils.updateActivityLocale(
                    activity = this@MainActivity,
                    languageMode = userData.languageMode,
                )
            }

            val backStack = rememberNavBackStack(HomeKey)
            ThemeProvider(
                themeMode = userData.themeMode,
                activity = this@MainActivity,
            ) {
                // 只包住畫面內容（不含上面的 backStack），語言切換時只重組字串顯示，
                // 不會重置 backStack、也不需要 activity.recreate()。
                key(userData.languageMode) {
                    MainScreen(configuration.value, backStack, onRetry = {
                        viewModel.retryConfiguration()
                    })
                }
            }
        }
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

    // 用 movableContentOf 讓同一個 NavDisplay 組合狀態能在「有無包裝 JMNavigationSuiteScaffold」
    // 兩種父層結構間搬移，而不會因為切換到不同的呼叫點被 Compose 視為結構變化而整棵樹重建、
    // 遺失子畫面（例如搜尋輸入框、分頁選擇）的 remember 狀態。
    val navDisplay = remember(backStack) {
        movableContentOf {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = { navKey -> mainEntry(navKey, backStack) },
            )
        }
    }

    if (currentKey is MovieDetailKey) {
        navDisplay()
        return
    }

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
        navDisplay()
    }
}

private fun mainEntry(
    navKey: NavKey,
    backStack: NavBackStack<NavKey>,
): NavEntry<NavKey> {
    loadFeatureModuleIfNeeded(navKey)
    return when (navKey) {
        HomeKey -> homeEntry(onMovieClick = { movieId ->
            backStack.add(MovieDetailKey(movieId))
        }).second
        CollectKey -> collectEntry(onMovieClick = { movie ->
            backStack.add(MovieDetailKey(movie.movieCardId))
        }).second
        HistoryKey -> historyEntry(onMovieClick = { movie ->
            backStack.add(MovieDetailKey(movie.movieCardId))
        }).second
        SearchKey -> searchEntry(onMovieClick = { movie ->
            backStack.add(MovieDetailKey(movie.movieCardId))
        }).second
        SettingKey -> settingEntry().second
        is MovieDetailKey -> movieDetailEntry(
            key = navKey,
            onBackClick = { backStack.removeLastOrNull() },
            onMovieClick = { movie -> backStack.add(MovieDetailKey(movie.movieCardId)) },
        ).second
        else -> NavEntry(navKey) { PlaceholderScreen() }
    }
}

@Composable
private fun PlaceholderScreen() {
    Box(modifier = Modifier.fillMaxSize())
}
