package com.shang.jetpackmoviekmp.network.extension

import com.shang.jetpackmoviekmp.network.model.NetworkException
import com.shang.jetpackmoviekmp.network.model.NetworkResponse
import com.shang.jetpackmoviekmp.network.model.toNetworkException
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException

/**
 * Executes a Ktor HTTP request and converts it into a [NetworkResponse].
 *
 * Cancellation is rethrown so coroutine cancellation remains cooperative.
 * HTTP error responses, serialization failures, timeouts, and connection
 * failures are normalized into [NetworkException].
 *
 * @param apiCall Suspended block that returns the Ktor [HttpResponse].
 * @return A successful response containing decoded [T], or an error response.
 */
suspend inline fun <reified T> safeApiCall(
    apiCall: suspend () -> HttpResponse,
): NetworkResponse<T> {
    return try {
        val call = apiCall.invoke()
        NetworkResponse(
            code = call.status.value,
            data = call.body<T>(),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: ResponseException) {
        NetworkResponse<T>(
            code = e.response.status.value,
            error = NetworkException.HttpError(
                httpCode = e.response.status.value,
                errorBody = e.response.bodyAsText(),
            ),
        )
    } catch (e: Exception) {
        NetworkResponse(code = -1, error = e.toNetworkException())
    }
}

/**
 * Maps successful [NetworkResponse.data] while preserving status code and error state.
 *
 * @param transform Converts the successful payload into the caller-facing model.
 * @return A response with mapped data, or the original error if no data is present.
 */
inline fun <T, R> NetworkResponse<T>.mapData(transform: (T) -> R): NetworkResponse<R> {
    return if (data != null) {
        NetworkResponse(
            code = code,
            data = transform(data),
            error = null,
        )
    } else {
        NetworkResponse(
            code = code,
            data = null,
            error = error,
        )
    }
}
