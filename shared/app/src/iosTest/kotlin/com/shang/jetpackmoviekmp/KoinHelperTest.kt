package com.shang.jetpackmoviekmp

import com.shang.jetpackmoviekmp.common.BaseHostUrlProvider
import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
import com.shang.jetpackmoviekmp.database.getTestDatabaseBuilder
import com.shang.jetpackmoviekmp.datastore.InMemoryPreferencesDataStore
import com.shang.jetpackmoviekmp.domain.usecase.GetHistoryMovieListUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetMovieDetailUseCase
import com.shang.jetpackmoviekmp.domain.usecase.GetMovieRecommendUseCase
import com.shang.jetpackmoviekmp.presenter.HomeMovieListPresenter
import com.shang.jetpackmoviekmp.presenter.SearchMovieListPresenter
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertIs

class KoinHelperTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun userDataRepository_afterInitKoin_resolvesRepository() {
        // Arrange
        initKoin(
            dataStore = InMemoryPreferencesDataStore(),
            databaseBuilder = { getTestDatabaseBuilder() },
            isDebug = true,
        )

        // Act
        val repository = KoinHelper.userDataRepository()

        // Assert
        assertIs<UserDataRepository>(repository)
    }

    @Test
    fun getMovieDetailUseCase_afterInitKoin_resolvesUseCase() {
        // Arrange
        initKoin(
            dataStore = InMemoryPreferencesDataStore(),
            databaseBuilder = { getTestDatabaseBuilder() },
            isDebug = true,
        )

        // Act
        val useCase = KoinHelper.getMovieDetailUseCase()

        // Assert
        assertIs<GetMovieDetailUseCase>(useCase)
    }

    @Test
    fun getMovieRecommendUseCase_afterInitKoin_resolvesUseCase() {
        // Arrange
        initKoin(
            dataStore = InMemoryPreferencesDataStore(),
            databaseBuilder = { getTestDatabaseBuilder() },
            isDebug = true,
        )

        // Act
        val useCase = KoinHelper.getMovieRecommendUseCase()

        // Assert
        assertIs<GetMovieRecommendUseCase>(useCase)
    }

    @Test
    fun getHistoryMovieListUseCase_afterInitKoin_resolvesUseCase() {
        // Arrange
        initKoin(
            dataStore = InMemoryPreferencesDataStore(),
            databaseBuilder = { getTestDatabaseBuilder() },
            isDebug = true,
        )

        // Act
        val useCase = KoinHelper.getHistoryMovieListUseCase()

        // Assert
        assertIs<GetHistoryMovieListUseCase>(useCase)
    }

    @Test
    fun getBaseHostUrlProvider_afterInitKoin_resolvesProvider() {
        // Arrange
        initKoin(
            dataStore = InMemoryPreferencesDataStore(),
            databaseBuilder = { getTestDatabaseBuilder() },
            isDebug = true,
        )

        // Act
        val provider = KoinHelper.getBaseHostUrlProvider()

        // Assert
        assertIs<BaseHostUrlProvider>(provider)
    }

    @Test
    fun createHomeMovieListPresenter_afterInitKoin_resolvesPresenterAndClears() {
        // Arrange
        initKoin(
            dataStore = InMemoryPreferencesDataStore(),
            databaseBuilder = { getTestDatabaseBuilder() },
            isDebug = true,
        )

        // Act
        val presenter = KoinHelper.createHomeMovieListPresenter(withGenres = "28")

        // Assert
        assertIs<HomeMovieListPresenter>(presenter)
        presenter.clear()
    }

    @Test
    fun createSearchMovieListPresenter_afterInitKoin_resolvesPresenterAndClears() {
        // Arrange
        initKoin(
            dataStore = InMemoryPreferencesDataStore(),
            databaseBuilder = { getTestDatabaseBuilder() },
            isDebug = true,
        )

        // Act
        val presenter = KoinHelper.createSearchMovieListPresenter(query = "Dune")

        // Assert
        assertIs<SearchMovieListPresenter>(presenter)
        presenter.clear()
    }
}
