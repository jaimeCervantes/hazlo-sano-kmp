package com.hazlosano.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.domain.settings.AppSettingsRepository
import com.hazlosano.domain.settings.LanguagePreference
import com.hazlosano.domain.settings.ThemePreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Los ajustes de la persona.
 *
 * No hay estado de carga ni de error, y es una decisión: el tema tiene siempre una respuesta —seguir
 * al sistema, si nunca se eligió otra cosa— así que una pantalla que dijera "cargando ajustes" o
 * "no se pudieron leer" estaría inventando dos situaciones que no existen.
 */
class SettingsViewModel(
    private val repository: AppSettingsRepository,
) : ViewModel() {

    val themePreference: StateFlow<ThemePreference> = repository.themePreference()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ThemePreference.DEFAULT,
        )

    val languagePreference: StateFlow<LanguagePreference> = repository.languagePreference()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = LanguagePreference.DEFAULT,
        )

    fun chooseTheme(preference: ThemePreference) {
        viewModelScope.launch { repository.setThemePreference(preference) }
    }

    fun chooseLanguage(preference: LanguagePreference) {
        viewModelScope.launch { repository.setLanguagePreference(preference) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
