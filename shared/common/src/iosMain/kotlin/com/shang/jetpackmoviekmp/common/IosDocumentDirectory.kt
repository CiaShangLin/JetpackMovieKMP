package com.shang.jetpackmoviekmp.common

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * 解析穩定的 iOS app document 目錄下、指定檔名的完整路徑，確保 app 重啟後仍可讀取同一個檔案。
 *
 * @param fileName 檔案名稱（不含目錄）。
 * @return document 目錄下該檔名的完整路徑。
 * @throws IllegalArgumentException 當無法解析 iOS document directory 時拋出。
 */
@OptIn(ExperimentalForeignApi::class)
fun resolveIosDocumentDirectoryPath(fileName: String): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(documentDirectory?.path) { "Unable to resolve iOS document directory" } +
        "/$fileName"
}
