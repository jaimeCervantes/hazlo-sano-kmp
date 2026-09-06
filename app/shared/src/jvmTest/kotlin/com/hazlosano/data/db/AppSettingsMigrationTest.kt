package com.hazlosano.data.db

import com.hazlosano.data.settings.SqlDelightAppSettingsRepository
import com.hazlosano.domain.settings.LanguagePreference
import com.hazlosano.domain.settings.ThemePreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * La tabla de ajustes, y que el tema elegido sobreviva.
 *
 * Lo que de verdad se comprueba aquí es que **las dos formas de llegar al esquema acaban en el mismo
 * sitio**: una instalación nueva lo crea entero desde el `.sq`, y una que venía de antes llega
 * aplicando las migraciones una a una. Si las dos no coincidieran, el ajuste funcionaría al instalar
 * y fallaría al actualizar, que es el fallo que nadie ve hasta que ya está publicado.
 */
class AppSettingsMigrationTest {

    @Test
    fun `an install that came from version one ends up with the settings table`() {
        val driver = inMemoryDriver()

        driver.migrateFromVersionOne()

        assertTrue(
            "AppSettingEntity" in driver.tableNames(),
            "una instalación que se actualizó no tiene dónde guardar el tema",
        )
        assertEquals(setOf("key", "settingValue"), driver.columnsOf("AppSettingEntity"))
    }

    @Test
    fun `a fresh install and an upgraded one have the same settings table`() {
        val fresh = inMemoryDriver().also { HazloSanoDatabase.Schema.create(it) }
        val upgraded = inMemoryDriver().also { it.migrateFromVersionOne() }

        assertEquals(
            fresh.columnsOf("AppSettingEntity"),
            upgraded.columnsOf("AppSettingEntity"),
        )
    }

    @Test
    fun `a phone that was never asked follows the system`() = runTest {
        val repository = SqlDelightAppSettingsRepository(inMemoryHazloSanoDatabase())

        assertEquals(ThemePreference.SYSTEM, repository.themePreference().first())
    }

    @Test
    fun `the chosen theme survives being stored`() = runTest {
        val repository = SqlDelightAppSettingsRepository(inMemoryHazloSanoDatabase())

        repository.setThemePreference(ThemePreference.DARK)

        assertEquals(ThemePreference.DARK, repository.themePreference().first())
    }

    /**
     * Cambiar de opinión reemplaza la fila en vez de sumar una segunda. La tabla tiene la clave como
     * primaria, así que una segunda fila ni siquiera se puede insertar — lo que este test fija es
     * que la escritura no falle al intentarlo, que es lo que pasaría con un `INSERT` a secas.
     */
    @Test
    fun `changing your mind replaces the choice instead of failing`() = runTest {
        val repository = SqlDelightAppSettingsRepository(inMemoryHazloSanoDatabase())

        repository.setThemePreference(ThemePreference.DARK)
        repository.setThemePreference(ThemePreference.LIGHT)
        repository.setThemePreference(ThemePreference.SYSTEM)

        assertEquals(ThemePreference.SYSTEM, repository.themePreference().first())
    }

    @Test
    fun `the chosen language survives being stored`() = runTest {
        val repository = SqlDelightAppSettingsRepository(inMemoryHazloSanoDatabase())

        repository.setLanguagePreference(LanguagePreference.ENGLISH)

        assertEquals(LanguagePreference.ENGLISH, repository.languagePreference().first())
    }

    /**
     * Los dos ajustes viven en la misma tabla y no se pisan: es la propiedad que hace que añadir el
     * idioma no necesitara migración ninguna.
     */
    @Test
    fun `theme and language are kept apart in the same table`() = runTest {
        val repository = SqlDelightAppSettingsRepository(inMemoryHazloSanoDatabase())

        repository.setThemePreference(ThemePreference.DARK)
        repository.setLanguagePreference(LanguagePreference.ENGLISH)

        assertEquals(ThemePreference.DARK, repository.themePreference().first())
        assertEquals(LanguagePreference.ENGLISH, repository.languagePreference().first())
    }

    /**
     * Una preferencia guardada por una versión que conocía una opción más no puede tumbar el
     * arranque: se vuelve a seguir al sistema.
     */
    @Test
    fun `a stored value this version does not know falls back to the system`() = runTest {
        val database = inMemoryHazloSanoDatabase()
        database.appSettingQueries.upsertSetting(key = "theme", settingValue = "SEPIA")

        val repository = SqlDelightAppSettingsRepository(database)

        assertEquals(ThemePreference.SYSTEM, repository.themePreference().first())
    }
}
