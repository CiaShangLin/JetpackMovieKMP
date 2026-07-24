package com.shang.jetpackmoviekmp.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AppResultTest {

    @Test
    fun appResult_success_holds_given_data() {
        // Arrange
        val data = "configuration"

        // Act
        val result: AppResult<String> = AppResult.Success(data)

        // Assert
        assertIs<AppResult.Success<String>>(result)
        assertEquals(data, result.data)
    }

    @Test
    fun appResult_failure_holds_given_error() {
        // Arrange
        val error = AppError.Unknown

        // Act
        val result: AppResult<String> = AppResult.Failure(error)

        // Assert
        assertIs<AppResult.Failure>(result)
        assertEquals(error, result.error)
    }

    @Test
    fun appError_network_preserves_underlying_networkException() {
        // Arrange
        val exception = NetworkException.UnknownError(IllegalStateException("boom"))

        // Act
        val error = AppError.Network(exception)

        // Assert
        assertEquals(exception, error.exception)
    }

    @Test
    fun appError_is_a_throwable_so_callers_can_hold_it_without_conversion() {
        // Arrange
        val exception = NetworkException.UnknownError(IllegalStateException("boom"))

        // Act
        val error: Throwable = AppError.Network(exception)

        // Assert
        assertIs<AppError.Network>(error)
        assertEquals(exception, error.cause)
    }

    @Test
    fun toAppError_wraps_networkException_as_network_error() {
        // Arrange
        val exception: Throwable = NetworkException.UnknownError(IllegalStateException("boom"))

        // Act
        val error = exception.toAppError()

        // Assert
        assertEquals(AppError.Network(exception as NetworkException), error)
    }

    @Test
    fun toAppError_maps_other_throwables_to_unknown() {
        // Arrange
        val exception: Throwable = IllegalStateException("boom")

        // Act
        val error = exception.toAppError()

        // Assert
        assertEquals(AppError.Unknown, error)
    }
}
