package com.shang.jetpackmoviekmp.network.model

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.JsonConvertException
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

/**
 * Normalized network-layer exception hierarchy.
 *
 * The data layer exposes these errors instead of leaking Ktor, serialization,
 * or platform socket exceptions to callers.
 */
sealed class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /**
     * HTTP response with a non-success status code.
     *
     * @param httpCode HTTP status code returned by the server.
     * @param errorBody Optional raw response body for diagnostics.
     */
    data class HttpError(
        val httpCode: Int,
        val errorBody: String? = null,
    ) : NetworkException("HTTP Error: $httpCode")

    /**
     * Failure caused by connectivity or DNS resolution problems.
     *
     * @param cause Underlying platform or Ktor exception.
     */
    data class ConnectionError(
        override val cause: Throwable,
    ) : NetworkException("Connection failed", cause)

    /**
     * Failure caused by request, connection, or socket timeout.
     *
     * @param cause Underlying timeout exception.
     */
    data class TimeoutError(
        override val cause: Throwable,
    ) : NetworkException("Request timeout", cause)

    /**
     * Failure caused by response decoding or serialization.
     *
     * @param cause Underlying parser exception.
     */
    data class ParseError(
        override val cause: Throwable,
    ) : NetworkException("Parse failed", cause)

    /**
     * Failure that did not match a known network error category.
     *
     * @param cause Original exception.
     */
    data class UnknownError(
        override val cause: Throwable,
    ) : NetworkException("Unknown error", cause)
}

/**
 * Converts common Ktor, IO, and serialization exceptions to [NetworkException].
 *
 * Existing [NetworkException] instances are returned unchanged.
 */
fun Throwable.toNetworkException(): NetworkException {
    return when (this) {
        is NetworkException -> this

        is ResponseException -> NetworkException.HttpError(
            httpCode = response.status.value,
        )

        is HttpRequestTimeoutException,
        is ConnectTimeoutException,
        is SocketTimeoutException,
        -> NetworkException.TimeoutError(this)

        is JsonConvertException,
        is SerializationException,
        -> NetworkException.ParseError(this)

        is UnresolvedAddressException,
        is IOException,
        -> NetworkException.ConnectionError(this)

        else -> NetworkException.UnknownError(this)
    }
}
