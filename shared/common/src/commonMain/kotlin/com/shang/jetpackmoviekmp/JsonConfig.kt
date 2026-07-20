package com.shang.jetpackmoviekmp

import kotlinx.serialization.json.Json

/**
 * 共用的 JSON 解析設定。
 *
 * 目前網路回應與偏好設定持久化都會使用這份設定，避免 API 增加欄位時造成解析失敗。
 */
val sharedJson = Json { ignoreUnknownKeys = true }
