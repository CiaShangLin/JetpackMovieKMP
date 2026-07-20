package com.shang.jetpackmoviekmp

import kotlinx.serialization.json.Json

/** 全模組共用的 JSON 設定，忽略未知欄位以避免後端新增欄位時解析失敗。 */
val sharedJson = Json { ignoreUnknownKeys = true }
