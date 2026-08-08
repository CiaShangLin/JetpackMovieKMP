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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.shang.jetpackmoviekmp.common.UiState
import com.shang.jetpackmoviekmp.core.designsystem.component.JMBackground
import com.shang.jetpackmoviekmp.core.designsystem.component.JMNavigationSuiteScaffold
import com.shang.jetpackmoviekmp.core.designsystem.theme.JetpackMovieComposeTheme
import com.shang.jetpackmoviekmp.core.designsystem.theme.OnBackground
import com.shang.jetpackmoviekmp.core.designsystem.theme.Primary
import com.shang.jetpackmoviekmp.core.designsystem.theme.PrimaryContainer
import com.shang.jetpackmoviekmp.core.designsystem.theme.SurfaceVariant
import com.shang.jetpackmoviekmp.core.ui.ErrorScreen
import com.shang.jetpackmoviekmp.core.ui.LoadingScreen
import com.shang.jetpackmoviekmp.feature.collect.navigation.CollectKey
import com.shang.jetpackmoviekmp.feature.collect.navigation.collectEntry
import com.shang.jetpackmoviekmp.feature.detail.navigation.MovieDetailKey
import com.shang.jetpackmoviekmp.feature.detail.navigation.movieDetailEntry
import com.shang.jetpackmoviekmp.feature.history.navigation.HistoryKey
import com.shang.jetpackmoviekmp.feature.history.navigation.historyEntry
import com.shang.jetpackmoviekmp.feature.home.navigation.HomeKey
import com.shang.jetpackmoviekmp.feature.home.navigation.homeEntry
import com.shang.jetpackmoviekmp.feature.search.navigation.SearchKey
import com.shang.jetpackmoviekmp.feature.search.navigation.searchEntry
import com.shang.jetpackmoviekmp.feature.setting.navigation.SettingKey
import com.shang.jetpackmoviekmp.feature.setting.navigation.settingEntry
import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.ThemeMode
import com.shang.jetpackmoviekmp.navigation.MainNavItem
import com.shang.jetpackmoviekmp.utils.LanguageSettingUtils
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val splashScreen = installSplashScreen()

        setContent {
            val viewModel = koinViewModel<MainViewModel>()
            val configuration = viewModel.configuration.collectAsState()
            val userData by viewModel.userData.collectAsState()
            splashScreen.setKeepOnScreenCondition {
                configuration.value is UiState.Loading
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
                userData.languageMode
            }

            val topLevelBackStack = remember { TopLevelBackStack<NavKey>(HomeKey) }
            ThemeProvider(
                themeMode = userData.themeMode,
                activity = this@MainActivity,
            ) {
                // 只包住畫面內容（不含上面的 topLevelBackStack），語言切換時只重組字串顯示，
                // 不會重置 topLevelBackStack、也不需要 activity.recreate()。
                key(userData.languageMode) {
                    MainScreen(configuration.value, topLevelBackStack, onRetry = {
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

/**
 * App 主畫面容器，依 [mainUiState] 分派載入中／錯誤／成功三種畫面。
 *
 * @param mainUiState 首次進入 App 所需設定資料的載入狀態
 * @param topLevelBackStack 底部導覽用的頂層 back stack，成功狀態時交給 [SuccessScreen] 使用
 * @param onRetry 載入失敗時使用者點擊重試的回呼
 */
@Composable
fun MainScreen(mainUiState: UiState<ConfigurationBean>, topLevelBackStack: TopLevelBackStack<NavKey>, onRetry: () -> Unit) {
    JMBackground(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.statusBars.asPaddingValues())
            // 讓 testTag 在 UiAutomator 端可透過 resource-id 查找，不受裝置語言影響，
            // 供 :benchmark module 的 BaselineProfileGenerator 驅動導覽列切換頁籤使用。
            .semantics { testTagsAsResourceId = true },
    ) {
        when (mainUiState) {
            is UiState.Loading -> {
                MainLoadingScreen()
            }

            is UiState.Error -> {
                MainErrorScreen(mainUiState.throwable as Exception?, onRetry = onRetry)
            }

            is UiState.Success -> {
                SuccessScreen(topLevelBackStack = topLevelBackStack)
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

/**
 * 設定資料載入成功後的主要導覽畫面：渲染 [NavDisplay] 並包裝底部導覽列。
 *
 * detail（[MovieDetailKey]）顯示時隱藏底部導覽列；其餘畫面顯示底部導覽列，
 * 並依 [TopLevelBackStack.topLevelKey] 標示目前選取的 Tab。
 *
 * @param topLevelBackStack 每個底部導覽 Tab 各自維護獨立 sub back stack 的頂層 back stack
 */
@Composable
fun SuccessScreen(topLevelBackStack: TopLevelBackStack<NavKey>) {
    val currentKey = topLevelBackStack.backStack.lastOrNull()

    // 用 movableContentOf 讓同一個 NavDisplay 組合狀態能在「有無包裝 JMNavigationSuiteScaffold」
    // 兩種父層結構間搬移，而不會因為切換到不同的呼叫點被 Compose 視為結構變化而整棵樹重建、
    // 遺失子畫面（例如搜尋輸入框、分頁選擇）的 remember 狀態。
    val navDisplay = remember(topLevelBackStack) {
        movableContentOf {
            NavDisplay(
                backStack = topLevelBackStack.backStack,
                onBack = { topLevelBackStack.removeLast() },
                entryProvider = { navKey -> mainEntry(navKey, topLevelBackStack) },
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
                    selected = topLevelBackStack.topLevelKey == item.key,
                    onClick = {
                        if (topLevelBackStack.topLevelKey != item.key) {
                            topLevelBackStack.addTopLevel(item.key)
                        }
                    },
                    // benchmark/BaselineProfileGenerator.kt 有一份對應的 nav_* resource-id 硬編碼清單，
                    // 改這裡（尤其是 enum name）要記得同步更新那邊。
                    modifier = Modifier.testTag("nav_${item.name.lowercase()}"),
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
        navDisplay()
    }
}

private fun mainEntry(
    navKey: NavKey,
    topLevelBackStack: TopLevelBackStack<NavKey>,
): NavEntry<NavKey> {
    return when (navKey) {
        HomeKey -> homeEntry(onMovieClick = { movieId ->
            topLevelBackStack.add(MovieDetailKey(movieId))
        }).second
        CollectKey -> collectEntry(onMovieClick = { movie ->
            topLevelBackStack.add(MovieDetailKey(movie.movieCardId))
        }).second
        HistoryKey -> historyEntry(onMovieClick = { movie ->
            topLevelBackStack.add(MovieDetailKey(movie.movieCardId))
        }).second
        SearchKey -> searchEntry(onMovieClick = { movie ->
            topLevelBackStack.add(MovieDetailKey(movie.movieCardId))
        }).second
        SettingKey -> settingEntry().second
        is MovieDetailKey -> movieDetailEntry(
            key = navKey,
            onBackClick = { topLevelBackStack.removeLast() },
            onMovieClick = { movie -> topLevelBackStack.add(MovieDetailKey(movie.movieCardId)) },
        ).second
        else -> NavEntry(navKey) { PlaceholderScreen() }
    }
}

@Composable
private fun PlaceholderScreen() {
    Box(modifier = Modifier.fillMaxSize())
}
