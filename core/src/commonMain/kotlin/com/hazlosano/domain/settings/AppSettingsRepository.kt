package com.hazlosano.domain.settings

import kotlinx.coroutines.flow.Flow

/**
 * Los ajustes que la persona elige y el app recuerda.
 *
 * El tema se expone como flujo y no como una lectura suelta porque cambiarlo tiene que alcanzar a la
 * pantalla que ya está pintada: elegir "claro" en ajustes repinta el app entero sin reiniciarlo.
 *
 * Nace con un solo ajuste a propósito. El idioma entra por aquí en el siguiente slice, y la forma
 * —un flujo por ajuste, no un objeto de preferencias— es lo que evita que cambiar uno recomponga a
 * quien observa el otro.
 */
interface AppSettingsRepository {
    fun themePreference(): Flow<ThemePreference>

    suspend fun setThemePreference(preference: ThemePreference)
}
