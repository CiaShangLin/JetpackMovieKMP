package com.shang.jetpackmoviekmp.feature.setting.di

import com.shang.jetpackmoviekmp.feature.setting.ui.SettingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * 提供設定頁 [SettingViewModel] 的 Koin module。
 *
 * @return 設定 feature 所需的 Koin module。
 */
fun settingModule() = module {
    viewModel {
        SettingViewModel(
            userDataRepository = get(),
        )
    }
}
