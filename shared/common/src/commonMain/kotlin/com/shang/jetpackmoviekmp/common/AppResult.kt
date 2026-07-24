package com.shang.jetpackmoviekmp.common

/**
 * 通用的成功／失敗容器，取代跨 iOS 匯出時語意不明確的 `kotlin.Result<T>`。
 *
 * `kotlin.Result<T>` 匯出給 Swift 後會被 type-erase 成 opaque boxed value，
 * 無法用 `as? T` 判斷成功／失敗；[AppResult] 以 `sealed interface` 定義，
 * 可被 SKIE 明確匯出成 Swift enum，讓呼叫端（含 iOS）能直接以型別區分結果。
 */
sealed interface AppResult<out T> {

    /**
     * 成功結果。
     *
     * @property data 成功取得的資料。
     */
    data class Success<T>(val data: T) : AppResult<T>

    /**
     * 失敗結果。
     *
     * @property error 失敗原因。
     */
    data class Failure(val error: AppError) : AppResult<Nothing>
}
