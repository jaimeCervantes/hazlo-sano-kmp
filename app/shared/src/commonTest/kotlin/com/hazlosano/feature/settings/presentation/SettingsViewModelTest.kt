package com.hazlosano.feature.settings.presentation

import com.hazlosano.domain.settings.AppSettingsRepository
import com.hazlosano.domain.settings.LanguagePreference
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
    private val language = MutableStateFlow(LanguagePreference.DEFAULT)
    val written = mutableListOf<ThemePreference>()
    val languagesWritten = mutableListOf<LanguagePreference>()

    override fun themePreference(): Flow<ThemePreference> = theme.asStateFlow()

    override suspend fun setThemePreference(preference: ThemePreference) {
        written += preference
        theme.value = preference
    }

    override fun languagePreference(): Flow<LanguagePreference> = language.asStateFlow()

    override suspend fun setLanguagePreference(preference: LanguagePreference) {
        languagesWritten += preference
        language.value = preference
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

    @Test
    fun `choosing a language stores it and reaches whoever is watching`() = runTest {
        val repository = FakeAppSettingsRepository()
        val viewModel = SettingsViewModel(repository)

        viewModel.chooseLanguage(LanguagePreference.ENGLISH)

        assertEquals(listOf(LanguagePreference.ENGLISH), repository.languagesWritten)
        assertEquals(
            LanguagePreference.ENGLISH,
            viewModel.languagePreference.first { it == LanguagePreference.ENGLISH },
        )
    }

    /** Los dos ajustes van por flujos distintos: elegir uno no toca el otro. */
    @Test
    fun `choosing a language leaves the theme alone`() = runTest {
        val repository = FakeAppSettingsRepository(ThemePreference.DARK)
        val viewModel = SettingsViewModel(repository)

        viewModel.chooseLanguage(LanguagePreference.ENGLISH)

        assertEquals(emptyList(), repository.written)
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
