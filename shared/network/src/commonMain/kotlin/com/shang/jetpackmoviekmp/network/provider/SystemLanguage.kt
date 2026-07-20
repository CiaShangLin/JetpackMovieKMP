package com.shang.jetpackmoviekmp.network.provider

/**
 * 回傳目前平台/系統的語言代碼；無法取得時回傳 null。
 */
internal expect fun currentSystemLanguageCode(): String?
