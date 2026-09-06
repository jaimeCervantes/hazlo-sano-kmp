package com.hazlosano.data.settings

import com.hazlosano.data.db.DatabaseProvider
import com.hazlosano.domain.settings.AppSettingsRepository
import com.hazlosano.domain.settings.LanguagePreference
import com.hazlosano.domain.settings.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * El almacén de ajustes de este target (DI manual, como el resto del proyecto).
 *
 * El respaldo **no** es un no-op, a diferencia del de movimiento. La web no inicializa la base
 * —`DatabaseProvider.initialize` se llama en Android, escritorio e iOS, y en ningún sitio de
 * `jsMain`—, y un no-op ahí dejaría el selector de tema pintado y muerto: se toca "oscuro" y no pasa
 * nada. Con un respaldo en memoria el ajuste funciona durante la sesión y sólo se pierde al recargar,
 * que es una limitación honesta y visible en vez de un control roto.
 */
internal fun appSettingsRepository(): AppSettingsRepository =
    if (DatabaseProvider.isInitialized) {
        SqlDelightAppSettingsRepository(DatabaseProvider.get())
    } else {
        InMemoryAppSettingsRepository
    }

/** Recuerda los ajustes mientras el app siga abierto, y nada más. Ver la nota de arriba. */
internal object InMemoryAppSettingsRepository : AppSettingsRepository {
    private val theme = MutableStateFlow(ThemePreference.DEFAULT)
    private val language = MutableStateFlow(LanguagePreference.DEFAULT)

    override fun themePreference(): Flow<ThemePreference> = theme.asStateFlow()

    override suspend fun setThemePreference(preference: ThemePreference) {
        theme.value = preference
    }

    override fun languagePreference(): Flow<LanguagePreference> = language.asStateFlow()

    override suspend fun setLanguagePreference(preference: LanguagePreference) {
        language.value = preference
    }
}
