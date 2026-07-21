package com.shang.jetpackmoviekmp.ui

import com.shang.jetpackmoviekmp.domain.usecase.GetConfigurationUseCase
import com.shang.jetpackmoviekmp.model.ConfigurationBean
import com.shang.jetpackmoviekmp.model.UserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        movieRepository: FakeMovieRepository = FakeMovieRepository(),
        userDataRepository: FakeUserDataRepository = FakeUserDataRepository(),
    ): MainViewModel {
        val useCase = GetConfigurationUseCase(
            movieRepository = movieRepository,
            userDataRepository = userDataRepository,
            ioDispatcher = dispatcher,
        )
        return MainViewModel(
            getConfigurationUseCase = useCase,
            userDataRepository = userDataRepository,
        )
    }

    @Test
    fun `configuration 在 use case 成功時回傳 Success`() = runTest(dispatcher) {
        // Arrange
        val viewModel = createViewModel()

        // Act
        val job = viewModel.configuration.launchIn(this)

        // Assert
        assertEquals(MainUiState.Success(ConfigurationBean()), viewModel.configuration.value)
        job.cancel()
    }

    @Test
    fun `configuration 在 use case 失敗且無本地快取時回傳 Error`() = runTest(dispatcher) {
        // Arrange
        val movieRepository = FakeMovieRepository().apply {
            configurationResult = Result.failure(IllegalStateException("boom"))
        }
        val userDataRepository = FakeUserDataRepository().apply {
            userDataFlow = emptyFlow()
        }
        val viewModel = createViewModel(movieRepository, userDataRepository)

        // Act
        val job = viewModel.configuration.launchIn(this)

        // Assert
        assertIs<MainUiState.Error>(viewModel.configuration.value)
        job.cancel()
    }

    @Test
    fun `retryConfiguration 會重新觸發 configuration 載入`() = runTest(dispatcher) {
        // Arrange：先讓第一次載入失敗（無快取）
        val movieRepository = FakeMovieRepository().apply {
            configurationResult = Result.failure(IllegalStateException("boom"))
        }
        val userDataRepository = FakeUserDataRepository().apply {
            userDataFlow = emptyFlow()
        }
        val viewModel = createViewModel(movieRepository, userDataRepository)
        val job = viewModel.configuration.launchIn(this)
        assertIs<MainUiState.Error>(viewModel.configuration.value)
        movieRepository.configurationResult = Result.success(ConfigurationBean())

        // Act
        viewModel.retryConfiguration()

        // Assert
        assertEquals(MainUiState.Success(ConfigurationBean()), viewModel.configuration.value)
        job.cancel()
    }

    @Test
    fun `userData 反映 UserDataRepository 的資料`() = runTest(dispatcher) {
        // Arrange
        val viewModel = createViewModel()

        // Act
        val job = viewModel.userData.launchIn(this)

        // Assert
        assertEquals(UserData.getDefault(), viewModel.userData.value)
        job.cancel()
    }
}
