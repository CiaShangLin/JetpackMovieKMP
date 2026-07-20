package com.shang.jetpackmoviekmp.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Jetpack Movie 明亮模式色彩配置。
 */
val LightMovieColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,

    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,

    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,

    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,

    background = Background,
    onBackground = OnBackground,

    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,

    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,

    outline = Outline,
    outlineVariant = OutlineVariant,
)

/**
 * Jetpack Movie 深色模式色彩配置。
 */
val DarkMovieColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,

    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,

    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,

    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,

    background = DarkBackground,
    onBackground = DarkOnBackground,

    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,

    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,

    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
)

/**
 * Jetpack Movie Android app 的 Material theme。
 *
 * @param darkTheme 是否使用深色模式，預設跟隨系統設定。
 * @param content 套用主題後要顯示的 Compose 內容。
 */
@Composable
fun JetpackMovieComposeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) {
        DarkMovieColorScheme
    } else {
        LightMovieColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

// 🏠 **首頁 (HomePage)**
// // 頁面背景
// Scaffold(containerColor = MaterialTheme.colorScheme.background)
//
// // 分類Tab容器
// Surface(color = MaterialTheme.colorScheme.surface)
//
// // 分類Tab文字 - 選中狀態
// Text(color = MaterialTheme.colorScheme.primary)
//
// // 分類Tab文字 - 未選中狀態
// Text(color = MaterialTheme.colorScheme.onSurfaceVariant)
//
// // 分類Tab底線 - 選中狀態
// Divider(color = MaterialTheme.colorScheme.primary)
//
// // 載入更多按鈕
// Button(colors = ButtonDefaults.outlinedButtonColors(
// containerColor = MaterialTheme.colorScheme.surface,
// contentColor = MaterialTheme.colorScheme.onSurface))

// 🎬 **電影卡片 (MovieCard)**
// // 卡片背景
// Card(colors = CardDefaults.cardColors(
// containerColor = MaterialTheme.colorScheme.surface
// ))
//
// // 卡片陰影
// Card(elevation = CardDefaults.cardElevation(
// defaultElevation = 4.dp
// ))
//
// // 電影標題文字
// Text(
// color = MaterialTheme.colorScheme.onSurface,
// style = MaterialTheme.typography.titleMedium
// )
//
// // 發行日期文字
// Text(
// color = MaterialTheme.colorScheme.onSurfaceVariant,
// style = MaterialTheme.typography.bodySmall
// )
//
// // 收藏按鈕背景
// IconButton(
// colors = IconButtonDefaults.iconButtonColors(
// containerColor = MovieCardShadow // Color(0x80000000)
// )
// )
//
// // 收藏愛心 - 已收藏
// Icon(tint = MovieFavoriteRed) // Color(0xFFEF4444)
//
// // 收藏愛心 - 未收藏
// Icon(tint = MaterialTheme.colorScheme.onPrimary) // 白色
//
// // 評分標籤背景
// Surface(color = MovieOverlay) // Color(0x80000000)
//
// // 評分星星
// Icon(tint = MovieRatingYellow) // Color(0xFFFBBF24)
//
// // 評分文字
// Text(color = MaterialTheme.colorScheme.onPrimary) // 白色

// 🔍 **搜尋頁面 (SearchPage)**
// 頁面背景
// Column(modifier = Modifier.background(MaterialTheme.colorScheme.background))
//
// // 搜尋欄容器
// Surface(
// color = MaterialTheme.colorScheme.surface,
// shadowElevation = 4.dp
// )
//
// // 搜尋輸入框
// OutlinedTextField(
// colors = OutlinedTextFieldDefaults.colors(
// focusedBorderColor = MaterialTheme.colorScheme.primary,
// unfocusedBorderColor = MaterialTheme.colorScheme.outline,
// focusedTextColor = MaterialTheme.colorScheme.onSurface,
// unfocusedTextColor = MaterialTheme.colorScheme.onSurface
// )
// )
//
// // 搜尋圖標
// Icon(tint = MaterialTheme.colorScheme.onSurfaceVariant)
//
// // 搜尋按鈕
// Button(
// colors = ButtonDefaults.buttonColors(
// containerColor = MaterialTheme.colorScheme.primary,
// contentColor = MaterialTheme.colorScheme.onPrimary
// )
// )
//
// // 載入指示器
// CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
//
// // 結果數量文字
// Text(color = MaterialTheme.colorScheme.onSurfaceVariant)

// ❤️ **收藏頁面 (FavoritesPage)**
// // 頁面背景
// Column(modifier = Modifier.background(MaterialTheme.colorScheme.background))
//
// // 標題容器
// Surface(color = MaterialTheme.colorScheme.surface)
//
// // 頁面標題
// Text(
// color = MaterialTheme.colorScheme.onSurface,
// style = MaterialTheme.typography.headlineMedium
// )
//
// // 空狀態圖標
// Icon(
// tint = MaterialTheme.colorScheme.onSurfaceVariant,
// modifier = Modifier.size(64.dp)
// )
//
// // 空狀態標題
// Text(
// color = MaterialTheme.colorScheme.onSurface,
// style = MaterialTheme.typography.titleMedium
// )
//
// // 空狀態描述
// Text(
// color = MaterialTheme.colorScheme.onSurfaceVariant,
// style = MaterialTheme.typography.bodyMedium
// )

// 📚 **歷史頁面 (HistoryPage)**
// // 頁面背景
// Column(modifier = Modifier.background(MaterialTheme.colorScheme.background))
//
// // 標題容器
// Surface(color = MaterialTheme.colorScheme.surface)
//
// // 頁面標題
// Text(
// color = MaterialTheme.colorScheme.onSurface,
// style = MaterialTheme.typography.headlineMedium
// )
//
// // 清空按鈕
// TextButton(
// colors = ButtonDefaults.textButtonColors(
// contentColor = MaterialTheme.colorScheme.onSurfaceVariant
// )
// )
//
// // 清空圖標
// Icon(tint = MaterialTheme.colorScheme.onSurfaceVariant)

// ⚙️ **設定頁面 (SettingsPage)**
// // 頁面背景
// Column(modifier = Modifier.background(MaterialTheme.colorScheme.background))
//
// // 設定項目背景
// Surface(
// color = MaterialTheme.colorScheme.surface,
// shape = MaterialTheme.shapes.medium
// )
//
// // 設定項目圖標背景
// Surface(
// color = MaterialTheme.colorScheme.primaryContainer,
// shape = MaterialTheme.shapes.small
// )
//
// // 設定項目圖標
// Icon(tint = MaterialTheme.colorScheme.primary)
//
// // 設定項目標題
// Text(
// color = MaterialTheme.colorScheme.onSurface,
// style = MaterialTheme.typography.titleMedium
// )
//
// // 設定項目描述
// Text(
// color = MaterialTheme.colorScheme.onSurfaceVariant,
// style = MaterialTheme.typography.bodyMedium
// )
//
// // 主題預覽按鈕 - 選中狀態
// Surface(
// color = MaterialTheme.colorScheme.primaryContainer,
// border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
// )
//
// // 主題預覽按鈕 - 未選中狀態
// Surface(
// color = MaterialTheme.colorScheme.surface,
// border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
// )

// 🎭 **電影詳情頁面 (MovieDetailPage)**
// // 頁面背景
// Column(modifier = Modifier.background(MaterialTheme.colorScheme.background))
//
// // 橫幅遮罩
// Box(modifier = Modifier.background(
// brush = Brush.verticalGradient(
// colors = listOf(
// Color.Transparent,
// MovieOverlay // Color(0x80000000)
// )
// )
// ))
//
// // 返回按鈕背景
// IconButton(
// colors = IconButtonDefaults.iconButtonColors(
// containerColor = MovieOverlay
// )
// )
//
// // 播放按鈕
// Button(
// colors = ButtonDefaults.buttonColors(
// containerColor = MaterialTheme.colorScheme.primary,
// contentColor = MaterialTheme.colorScheme.onPrimary
// )
// )
//
// // 電影標題
// Text(
// color = MaterialTheme.colorScheme.onSurface,
// style = MaterialTheme.typography.headlineLarge
// )
//
// // 類型標籤
// AssistChip(
// colors = AssistChipDefaults.assistChipColors(
// containerColor = MaterialTheme.colorScheme.secondaryContainer,
// labelColor = MaterialTheme.colorScheme.onSecondaryContainer
// )
// )
//
// // 劇情簡介標題
// Text(
// color = MaterialTheme.colorScheme.onSurface,
// style = MaterialTheme.typography.titleMedium
// )
//
// // 劇情簡介內容
// Text(
// color = MaterialTheme.colorScheme.onSurfaceVariant,
// style = MaterialTheme.typography.bodyMedium
// )

// 🧭 **底部導覽 (BottomNavigation)**
// // 導覽容器
// NavigationBar(
// containerColor = MaterialTheme.colorScheme.surface,
// contentColor = MaterialTheme.colorScheme.onSurface
// )
//
// // 導覽項目 - 選中狀態
// NavigationBarItem(
// colors = NavigationBarItemDefaults.colors(
// selectedIconColor = MaterialTheme.colorScheme.primary,
// selectedTextColor = MaterialTheme.colorScheme.primary,
// indicatorColor = MaterialTheme.colorScheme.primaryContainer
// )
// )
//
// // 導覽項目 - 未選中狀態
// NavigationBarItem(
// colors = NavigationBarItemDefaults.colors(
// unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
// unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
// )
// )

// 💬 **對話框 (Dialog)**
// // 對話框背景
// Surface(
// color = MaterialTheme.colorScheme.surface,
// shape = MaterialTheme.shapes.large
// )
//
// // 對話框標題
// Text(
// color = MaterialTheme.colorScheme.onSurface,
// style = MaterialTheme.typography.headlineMedium
// )
//
// // 對話框內容
// Text(
// color = MaterialTheme.colorScheme.onSurfaceVariant,
// style = MaterialTheme.typography.bodyMedium
// )

// 🎨 **主題切換相關**
// // 狀態欄顏色
// window.statusBarColor = MaterialTheme.colorScheme.primary.toArgb()
//
// // 導覽欄顏色
// window.navigationBarColor = MaterialTheme.colorScheme.surface.toArgb()
