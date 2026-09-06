package com.hazlosano.core.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.domain.settings.LanguagePreference
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.settings_theme_light
import hazlosano.app.shared.generated.resources.tracker_start
import org.jetbrains.compose.resources.stringResource
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Que elegir un idioma cambie de verdad las cadenas que se leen.
 *
 * Es la prueba que justifica el diseño de [HazloLanguage]. El camino evidente —proveer un
 * `ComposeEnvironment` propio— no existe: en Compose Multiplatform 1.11 esa interfaz,
 * `LocalComposeEnvironment` y el constructor de `ResourceEnvironment` son **internos**. Lo que sí
 * funciona es mover el locale de la plataforma, porque el entorno por defecto se calcula desde
 * `Locale.current` y se memoiza con él como clave. Esta prueba fija que ese camino sigue abierto: el
 * día que la librería cambie por dentro, falla aquí y no en el teléfono de alguien.
 */
@OptIn(ExperimentalTestApi::class)
class HazloLanguageTest {

    private val original: Locale = Locale.getDefault()

    @AfterTest
    fun restore() {
        Locale.setDefault(original)
    }

    private fun readIn(preference: LanguagePreference): Pair<String, String> {
        lateinit var read: Pair<String, String>
        runComposeUiTest {
            setContent {
                HazloLanguage(preference) {
                    read = stringResource(Res.string.tracker_start) to
                        stringResource(Res.string.settings_theme_light)
                }
            }
        }
        return read
    }

    @Test
    fun `choosing english reads the english catalogue`() {
        assertEquals("Start" to "Light", readIn(LanguagePreference.ENGLISH))
    }

    @Test
    fun `choosing spanish reads the spanish catalogue`() {
        assertEquals("Iniciar" to "Claro", readIn(LanguagePreference.SPANISH))
    }

    /**
     * Y se puede volver: elegir inglés y después español no deja el app en inglés, que es el fallo
     * que tendría un locale que se fija y no se restaura.
     */
    @Test
    fun `changing your mind changes the language back`() {
        readIn(LanguagePreference.ENGLISH)

        assertEquals("Iniciar" to "Claro", readIn(LanguagePreference.SPANISH))
    }

    /**
     * Volver a «seguir al sistema» devuelve el locale con el que arrancó el app, no el último que se
     * eligió. Sin esto la opción existiría en la pantalla y no llevaría a ninguna parte: una vez
     * elegido un idioma, el app se quedaría en él para siempre.
     */
    @Test
    fun `going back to the system restores the locale the app started with`() {
        val started = Locale.getDefault()

        readIn(LanguagePreference.ENGLISH)
        readIn(LanguagePreference.SYSTEM)

        assertEquals(started, Locale.getDefault())
    }
}
