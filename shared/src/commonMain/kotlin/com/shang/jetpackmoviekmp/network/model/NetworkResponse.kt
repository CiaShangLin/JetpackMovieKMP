package com.shang.jetpackmoviekmp.network.model

import com.shang.jetpackmoviekmp.common.NetworkException

/**
 * Represents the normalized result of a network request.
 *
 * @param code HTTP status code, or `-1` when the request failed before receiving a response.
 * @param data Decoded response body for successful requests.
 * @param error Normalized failure information for unsuccessful requests.
 */
data class NetworkResponse<out T>(
    val code: Int,
    val data: T? = null,
    val error: NetworkException? = null,
) {
    /**
     * Returns true only when the HTTP status is successful and [data] is present.
     */
    val isSuccess: Boolean
        get() = code in 200..299 && data != null
}
