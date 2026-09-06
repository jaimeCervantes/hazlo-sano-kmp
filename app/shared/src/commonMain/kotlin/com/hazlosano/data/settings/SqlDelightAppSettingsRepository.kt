package com.hazlosano.data.settings

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.hazlosano.data.db.HazloSanoDatabase
import com.hazlosano.domain.settings.AppSettingsRepository
import com.hazlosano.domain.settings.ThemePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** La clave con la que se guarda el tema. Estable: la base la lee después de cada actualización. */
private const val THEME_KEY = "theme"

/** Los ajustes, guardados en la base compartida [HazloSanoDatabase]. */
class SqlDelightAppSettingsRepository(
    database: HazloSanoDatabase,
) : AppSettingsRepository {

    private val queries = database.appSettingQueries

    /**
     * El flujo emite de nuevo cuando la fila cambia, que es lo que hace que elegir un tema repinte
     * la pantalla que ya está abierta sin que nadie tenga que avisarla.
     */
    override fun themePreference(): Flow<ThemePreference> =
        queries.selectSetting(THEME_KEY)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { ThemePreference.fromStoredValue(it) }

    override suspend fun setThemePreference(preference: ThemePreference) {
        withContext(Dispatchers.Default) {
            queries.upsertSetting(key = THEME_KEY, settingValue = preference.storedValue)
        }
    }
}
