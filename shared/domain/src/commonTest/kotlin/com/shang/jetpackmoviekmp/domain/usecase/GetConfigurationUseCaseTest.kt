package com.shang.jetpackmoviekmp.domain.usecase

import com.shang.jetpackmoviekmp.common.AppError
import com.shang.jetpackmoviekmp.common.AppResult
import com.shang.jetpackmoviekmp.domain.FakeMovieRepository
import com.shang.jetpackmoviekmp.domain.FakeUserDataRepository
import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.UserData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetConfigurationUseCaseTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun useCase(
        movieRepository: FakeMovieRepository = FakeMovieRepository(),
        userDataRepository: FakeUserDataRepository = FakeUserDataRepository(),
    ) = GetConfigurationUseCase(
        movieRepository = movieRepository,
        userDataRepository = userDataRepository,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun invoke_writes_cache_and_emits_success_when_api_call_succeeds() = runTest {
        val configuration = ConfigurationBean(changeKeys = listOf("images"))
        val movieRepository = FakeMovieRepository().apply {
            configurationResult = Result.success(configuration)
        }
        val userDataRepository = FakeUserDataRepository()

        val result = useCase(movieRepository, userDataRepository).invoke().first()

        assertEquals(AppResult.Success(configuration), result)
        assertEquals(configuration, userDataRepository.userData.first().configuration)
    }

    @Test
    fun invoke_falls_back_to_cache_when_api_call_fails_and_cache_exists() = runTest {
        val cachedConfiguration = ConfigurationBean(changeKeys = listOf("cached"))
        val movieRepository = FakeMovieRepository().apply {
            configurationResult = Result.failure(IllegalStateException("boom"))
        }
        val userDataRepository = FakeUserDataRepository(
            initial = UserData.getDefault().copy(configuration = cachedConfiguration),
        )

        val result = useCase(movieRepository, userDataRepository).invoke().first()

        val success = assertIs<AppResult.Success<ConfigurationBean>>(result)
        assertEquals(cachedConfiguration, success.data)
    }

    @Test
    fun invoke_emits_original_failure_when_api_call_fails_and_no_cache_exists() = runTest {
        val error = IllegalStateException("boom")
        val movieRepository = FakeMovieRepository().apply {
            configurationResult = Result.failure(error)
        }
        val userDataRepository = FakeUserDataRepository().apply {
            userDataFlow = emptyFlow()
        }

        val result = useCase(movieRepository, userDataRepository).invoke().first()

        val failure = assertIs<AppResult.Failure>(result)
        assertEquals(AppError.Unknown, failure.error)
    }
}
