package com.shang.jetpackmoviekmp.feature.setting.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shang.jetpackmoviekmp.feature.setting.R
import com.shang.jetpackmoviekmp.model.LanguageMode
import com.shang.jetpackmoviekmp.model.ThemeMode
import com.shang.jetpackmoviekmp.model.UserData
import org.koin.compose.viewmodel.koinViewModel

/**
 * 設定頁，列出主題設定、語言設定、開發者資訊三個項目，並依使用者操作彈出對應 Dialog。
 *
 * @param viewModel 提供設定畫面狀態與操作的 ViewModel。
 */
@Composable
fun SettingScreen(viewModel: SettingViewModel = koinViewModel()) {
    var showThemeSettingDialog by remember { mutableStateOf(false) }
    var showLanguageSettingDialog by remember { mutableStateOf(false) }
    var showDevelopersSettingDialog by remember { mutableStateOf(false) }
    val userData by viewModel.userData.collectAsState()

    SettingScreen(
        userData = userData,
        onThemeSettingClick = { showThemeSettingDialog = true },
        onLanguageSettingClick = { showLanguageSettingDialog = true },
        onDevelopersSettingClick = { showDevelopersSettingDialog = true },
    )

    if (showThemeSettingDialog) {
        ThemeSettingDialog(
            onDismissRequest = { showThemeSettingDialog = false },
            currentTheme = userData.themeMode,
            onThemeSelected = { theme ->
                showThemeSettingDialog = false
                viewModel.setThemeMode(theme)
            },
        )
    }

    if (showLanguageSettingDialog) {
        LanguageSettingDialog(
            onDismissRequest = { showLanguageSettingDialog = false },
            currentLanguage = userData.languageMode,
            onLanguageSelected = { language ->
                showLanguageSettingDialog = false
                viewModel.setLanguageMode(language)
            },
        )
    }

    if (showDevelopersSettingDialog) {
        DevelopersSettingDialog(
            onDismissRequest = { showDevelopersSettingDialog = false },
        )
    }
}

@Composable
private fun SettingScreen(
    userData: UserData,
    modifier: Modifier = Modifier,
    onThemeSettingClick: () -> Unit,
    onLanguageSettingClick: () -> Unit,
    onDevelopersSettingClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
    ) {
        Text(
            text = stringResource(id = R.string.setting),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurface,
            thickness = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        )
        LazyColumn {
            item {
                ThemeSetting(onClick = onThemeSettingClick, themeMode = userData.themeMode)
            }
            item {
                LanguageSetting(
                    onClick = onLanguageSettingClick,
                    languageMode = userData.languageMode,
                )
            }
            item {
                DevelopersSetting(
                    onClick = onDevelopersSettingClick,
                )
            }
        }
    }
}

@Composable
private fun ThemeSetting(onClick: () -> Unit, themeMode: ThemeMode) {
    val currentThemeText = when (themeMode) {
        ThemeMode.LIGHT -> stringResource(id = R.string.theme_light_mode)
        ThemeMode.DARK -> stringResource(id = R.string.theme_dark_mode)
        ThemeMode.SYSTEM -> stringResource(id = R.string.theme_system_default)
    }

    SettingItem(
        icon = Icons.Rounded.Palette,
        title = stringResource(id = R.string.theme_setting_title),
        content = stringResource(id = R.string.theme_setting_current_format, currentThemeText),
        onClick = onClick,
    )
}

@Composable
private fun LanguageSetting(onClick: () -> Unit, languageMode: LanguageMode) {
    val currentLanguageText = when (languageMode) {
        LanguageMode.TRADITIONAL_CHINESE -> stringResource(id = R.string.language_traditional_chinese)
        LanguageMode.ENGLISH -> stringResource(id = R.string.language_english)
        LanguageMode.SYSTEM_DEFAULT -> stringResource(id = R.string.language_system_default)
    }
    SettingItem(
        icon = Icons.Rounded.Language,
        title = stringResource(id = R.string.language_setting_title),
        content = stringResource(
            id = R.string.language_setting_current_format,
            currentLanguageText,
        ),
        onClick = onClick,
    )
}

@Composable
private fun DevelopersSetting(onClick: () -> Unit) {
    SettingItem(
        icon = Icons.Rounded.Info,
        title = stringResource(id = R.string.developers_setting_title),
        content = stringResource(id = R.string.developers_setting_content),
        onClick = onClick,
    )
}

@Composable
private fun SettingItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    content: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 16.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                content,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.padding(8.dp),
        )
    }
}
