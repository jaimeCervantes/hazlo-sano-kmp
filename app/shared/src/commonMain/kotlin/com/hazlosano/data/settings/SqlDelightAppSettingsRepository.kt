package com.hazlosano.data.settings

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.hazlosano.data.db.HazloSanoDatabase
import com.hazlosano.domain.settings.AppSettingsRepository
import com.hazlosano.domain.settings.LanguagePreference
import com.hazlosano.domain.settings.ThemePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Las claves con las que se guarda cada ajuste. Estables: la base las lee después de cada
 * actualización, así que renombrarlas equivale a olvidar lo que la persona eligió.
 */
private const val THEME_KEY = "theme"
private const val LANGUAGE_KEY = "language"

/**
 * Los ajustes, guardados en la base compartida [HazloSanoDatabase].
 *
 * Sumar el idioma no pidió migración ninguna: la tabla es clave/valor precisamente para esto, y la
 * `7.sqm` que la creó sigue siendo la última.
 */
class SqlDelightAppSettingsRepository(
    database: HazloSanoDatabase,
) : AppSettingsRepository {

    private val queries = database.appSettingQueries

    /**
     * El flujo emite de nuevo cuando la fila cambia, que es lo que hace que elegir un ajuste
     * repinte la pantalla que ya está abierta sin que nadie tenga que avisarla.
     */
    override fun themePreference(): Flow<ThemePreference> =
        setting(THEME_KEY).map { ThemePreference.fromStoredValue(it) }

    override suspend fun setThemePreference(preference: ThemePreference) {
        store(THEME_KEY, preference.storedValue)
    }

    override fun languagePreference(): Flow<LanguagePreference> =
        setting(LANGUAGE_KEY).map { LanguagePreference.fromStoredValue(it) }

    override suspend fun setLanguagePreference(preference: LanguagePreference) {
        store(LANGUAGE_KEY, preference.storedValue)
    }

    private fun setting(key: String): Flow<String?> =
        queries.selectSetting(key)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)

    private suspend fun store(key: String, value: String) {
        withContext(Dispatchers.Default) {
            queries.upsertSetting(key = key, settingValue = value)
        }
    }
}
