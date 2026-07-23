package com.shang.jetpackmoviekmp

import com.shang.jetpackmoviekmp.data.repository.UserDataRepository
import com.shang.jetpackmoviekmp.database.getTestDatabaseBuilder
import com.shang.jetpackmoviekmp.datastore.InMemoryPreferencesDataStore
import com.shang.jetpackmoviekmp.domain.usecase.GetMovieDetailUseCase
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
}
