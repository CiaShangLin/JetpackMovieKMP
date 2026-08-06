package com.shang.jetpackmoviekmp.common

/**
 * ViewModel 層共用的載入狀態容器，統一表達 `Loading`／`Success`／`Error` 三種狀態。
 */
sealed interface UiState<out T> {

    /** 載入中。 */
    data object Loading : UiState<Nothing>

    /**
     * 載入成功。
     *
     * @property data 成功取得的資料。
     */
    data class Success<T>(val data: T) : UiState<T>

    /**
     * 載入失敗。
     *
     * @property throwable 失敗原因。
     */
    data class Error(val throwable: Throwable) : UiState<Nothing>
}

/**
 * 將 [AppResult] 轉換為 [UiState]。
 *
 * @return [AppResult.Success] 對應 [UiState.Success]；[AppResult.Failure] 對應 [UiState.Error]。
 */
fun <T> AppResult<T>.toUiState(): UiState<T> = when (this) {
    is AppResult.Success -> UiState.Success(data)
    is AppResult.Failure -> UiState.Error(error)
}
