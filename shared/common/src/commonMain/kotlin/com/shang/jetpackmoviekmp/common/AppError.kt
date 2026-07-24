package com.shang.jetpackmoviekmp.common

/**
 * 跨 UseCase 通用的錯誤模型，作為 [AppResult.Failure] 攜帶的錯誤型別。
 *
 * 繼承 [Exception] 而非單純的 `sealed interface`，讓呼叫端（例如 `androidApp` 的
 * `MainUiState.Error(val throwable: Throwable)`）可以直接持有 [AppError]，不需要額外的
 * 轉換層。這裡繼承 [Exception] 只是為了讓型別本身「是一個」`Throwable`，本身不會被
 * `throw`——`AppError` 一律透過 [AppResult.Failure] 當作一般資料值經由 `Flow` 傳遞，
 * 不涉及 Kotlin/Native 的例外跨界拋出機制（`@Throws`／`NSError` 轉換）。
 *
 * 本次只定義目前實際用得到的分類；`Database`／`LocalStorage` 等其餘分類留待
 * 之後有實際呼叫路徑需要時再擴充（`sealed class` 新增分支屬於編譯期安全的擴充）。
 */
sealed class AppError(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /**
     * 網路請求流程中的錯誤，包裝既有的 [NetworkException]。
     *
     * @property exception 底層的 [NetworkException]，同時作為此例外的 [cause]。
     */
    data class Network(val exception: NetworkException) : AppError(cause = exception)

    /**
     * 未歸類到其他分類的錯誤。
     */
    data object Unknown : AppError()
}

/**
 * 將既有的 [Throwable]（例如 UseCase／Repository 內部處理 `kotlin.Result` 時取得的失敗原因）
 * 轉換為 [AppError]：[NetworkException] 對應 [AppError.Network]，其餘一律歸類為 [AppError.Unknown]。
 */
fun Throwable.toAppError(): AppError = when (this) {
    is NetworkException -> AppError.Network(this)
    else -> AppError.Unknown
}
