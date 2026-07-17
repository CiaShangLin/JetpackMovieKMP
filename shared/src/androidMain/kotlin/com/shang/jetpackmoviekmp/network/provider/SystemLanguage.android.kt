package com.shang.jetpackmoviekmp.network.provider

import java.util.Locale

internal actual fun currentSystemLanguageCode(): String? =
    Locale.getDefault().language.ifBlank { null }
