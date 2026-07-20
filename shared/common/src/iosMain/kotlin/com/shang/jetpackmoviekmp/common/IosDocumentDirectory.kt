package com.shang.jetpackmoviekmp.common

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * 解析 iOS app documents 目錄底下的檔案路徑。
 *
 * @param fileName 要放在 documents 目錄中的檔名。
 * @return documents 目錄中的完整檔案路徑。
 * @throws IllegalArgumentException 當系統無法解析 documents 目錄時拋出。
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
