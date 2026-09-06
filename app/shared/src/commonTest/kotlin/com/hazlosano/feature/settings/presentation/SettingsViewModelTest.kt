package com.hazlosano.feature.settings.presentation

import com.hazlosano.domain.settings.AppSettingsRepository
import com.hazlosano.domain.settings.ThemePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeAppSettingsRepository(
    initial: ThemePreference = ThemePreference.DEFAULT,
) : AppSettingsRepository {
    private val theme = MutableStateFlow(initial)
    val written = mutableListOf<ThemePreference>()

    override fun themePreference(): Flow<ThemePreference> = theme.asStateFlow()

    override suspend fun setThemePreference(preference: ThemePreference) {
        written += preference
        theme.value = preference
    }
}

class SettingsViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `it starts on what the phone already decided`() = runTest {
        val viewModel = SettingsViewModel(FakeAppSettingsRepository())

        assertEquals(ThemePreference.SYSTEM, viewModel.themePreference.value)
    }

    @Test
    fun `it reports the preference that was already stored`() = runTest {
        val viewModel = SettingsViewModel(FakeAppSettingsRepository(ThemePreference.DARK))

        assertEquals(ThemePreference.DARK, viewModel.themePreference.first { it == ThemePreference.DARK })
    }

    @Test
    fun `choosing a theme stores it`() = runTest {
        val repository = FakeAppSettingsRepository()
        val viewModel = SettingsViewModel(repository)

        viewModel.chooseTheme(ThemePreference.LIGHT)

        assertEquals(listOf(ThemePreference.LIGHT), repository.written)
    }

    /**
     * Lo que hace que el ajuste no sea decorativo: elegir repinta lo que ya está en pantalla, porque
     * quien observa el tema lee del mismo flujo que la pantalla de ajustes escribe.
     */
    @Test
    fun `choosing a theme reaches whoever is watching`() = runTest {
        val repository = FakeAppSettingsRepository()
        val viewModel = SettingsViewModel(repository)

        viewModel.chooseTheme(ThemePreference.DARK)

        assertEquals(ThemePreference.DARK, viewModel.themePreference.first { it == ThemePreference.DARK })
    }
}
