package com.shang.jetpackmoviekmp.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UiStateTest {

    @Test
    fun toUiState_maps_appResult_success_to_uiState_success() {
        // Arrange
        val data = "configuration"
        val result: AppResult<String> = AppResult.Success(data)

        // Act
        val state = result.toUiState()

        // Assert
        assertIs<UiState.Success<String>>(state)
        assertEquals(data, state.data)
    }

    @Test
    fun toUiState_maps_appResult_failure_to_uiState_error_with_same_throwable() {
        // Arrange
        val error = AppError.Unknown
        val result: AppResult<String> = AppResult.Failure(error)

        // Act
        val state = result.toUiState()

        // Assert
        assertIs<UiState.Error>(state)
        assertEquals(error, state.throwable)
    }
}
