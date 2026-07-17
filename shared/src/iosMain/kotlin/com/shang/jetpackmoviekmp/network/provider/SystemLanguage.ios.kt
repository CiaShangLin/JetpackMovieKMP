package com.shang.jetpackmoviekmp.network.provider

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

internal actual fun currentSystemLanguageCode(): String? =
    runCatching { NSLocale.currentLocale.languageCode.ifBlank { null } }.getOrNull()
