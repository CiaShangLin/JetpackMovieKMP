package com.shang.jetpackmoviekmp.feature.setting.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.shang.jetpackmoviekmp.feature.setting.R
import com.shang.jetpackmoviekmp.model.LanguageMode

/**
 * 語言選擇 Dialog，列出 SYSTEM_DEFAULT／TRADITIONAL_CHINESE／ENGLISH 三個選項供使用者單選。
 *
 * @param onDismissRequest 使用者關閉 Dialog 時的回呼。
 * @param currentLanguage 目前的語言模式。
 * @param onLanguageSelected 使用者選擇語言時的回呼。
 */
@Composable
fun LanguageSettingDialog(
    onDismissRequest: () -> Unit,
    currentLanguage: LanguageMode,
    onLanguageSelected: (LanguageMode) -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .widthIn(
                    min = 280.dp,
                    max = 560.dp,
                )
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.language_selection_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Column(
                    modifier = Modifier.selectableGroup(),
                ) {
                    LanguageOption(
                        text = stringResource(R.string.language_system_default),
                        selected = currentLanguage == LanguageMode.SYSTEM_DEFAULT,
                        onClick = { onLanguageSelected(LanguageMode.SYSTEM_DEFAULT) },
                    )
                    LanguageOption(
                        text = stringResource(R.string.language_traditional_chinese),
                        selected = currentLanguage == LanguageMode.TRADITIONAL_CHINESE,
                        onClick = { onLanguageSelected(LanguageMode.TRADITIONAL_CHINESE) },
                    )
                    LanguageOption(
                        text = stringResource(R.string.language_english),
                        selected = currentLanguage == LanguageMode.ENGLISH,
                        onClick = { onLanguageSelected(LanguageMode.ENGLISH) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.onPrimaryContainer,
                unselectedColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
