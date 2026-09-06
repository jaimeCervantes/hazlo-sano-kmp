package com.hazlosano.domain.settings

import kotlinx.coroutines.flow.Flow

/**
 * Los ajustes que la persona elige y el app recuerda.
 *
 * El tema se expone como flujo y no como una lectura suelta porque cambiarlo tiene que alcanzar a la
 * pantalla que ya está pintada: elegir "claro" en ajustes repinta el app entero sin reiniciarlo.
 *
 * Un flujo por ajuste, y no un objeto de preferencias con los dos dentro: así cambiar el idioma no
 * recompone a quien sólo estaba mirando el tema.
 */
interface AppSettingsRepository {
    fun themePreference(): Flow<ThemePreference>

    suspend fun setThemePreference(preference: ThemePreference)

    fun languagePreference(): Flow<LanguagePreference>

    suspend fun setLanguagePreference(preference: LanguagePreference)
}
