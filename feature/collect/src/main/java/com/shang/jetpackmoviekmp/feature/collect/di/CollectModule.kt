package com.shang.jetpackmoviekmp.feature.collect.di

import com.shang.jetpackmoviekmp.feature.collect.ui.CollectViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * 提供收藏頁 [CollectViewModel] 的 Koin module。
 *
 * @return 收藏 feature 所需的 Koin module。
 */
fun collectModule() = module {
    viewModel {
        CollectViewModel(
            movieRepository = get(),
        )
    }
}
