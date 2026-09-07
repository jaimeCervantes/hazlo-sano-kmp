package com.hazlosano.core.ui.components.atomic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * La barra superior pinta su propio fondo, y no el de quien la componga.
 *
 * **Esta prueba existe por un fallo que llegó del uso, no de una revisión.** La barra era un `Row`
 * transparente que dejaba el fondo a su llamante, y las ocho pantallas que la usan repetían a mano
 * la misma línea en su raíz. Siete se acordaron; `RouteDetailScreen` no, y era la única pantalla del
 * app cuyo header no respetaba el tema — que es exactamente lo que se reportó al abrir una ruta.
 *
 * Por eso se comprueba el **componente** y no las ocho pantallas: una vez la barra pinta lo suyo, el
 * fallo no lo puede repetir ninguna pantalla, ni las que existen ni las que se escriban. Afirmar lo
 * mismo ocho veces sería contar llamantes, no cubrir el defecto.
 *
 * El fondo hostil es lo que da valor a la prueba: sobre un padre del color del tema, una barra
 * transparente y una pintada se ven idénticas, así que la prueba pasaría con el fallo dentro.
 */
@OptIn(ExperimentalTestApi::class)
class HazloTopAppBarBackgroundTest {

    /** Un color que no está en ninguna paleta del app: si se cuela, es que la barra no pinta. */
    private val hostile = Color(0xFFFF00FF)

    @Test
    fun `the bar keeps its own background over whatever is behind it`() = runComposeUiTest {
        val themeBackground = mutableStateOf(Color.Unspecified)

        setContent {
            MaterialTheme {
                themeBackground.value = MaterialTheme.colorScheme.background
                Column(modifier = Modifier.fillMaxSize().background(hostile)) {
                    HazloTopAppBar(title = "Cañón del Sumidero")
                }
            }
        }

        val painted = onNodeWithTag(HazloTopAppBarTags.BAR).captureToImage().cornerPixel()

        assertEquals(
            themeBackground.value,
            painted,
            "la barra dejó ver lo que tenía detrás en vez de pintar el fondo del tema",
        )
    }

    /**
     * Y el color es el mismo que las pantallas ponían a mano, así que ninguna cambia de aspecto.
     *
     * Es la otra mitad del cambio: arreglar la que estaba rota no puede costar mover las siete que
     * estaban bien.
     */
    @Test
    fun `the colour is the one the screens were painting themselves`() = runComposeUiTest {
        val themeBackground = mutableStateOf(Color.Unspecified)

        setContent {
            MaterialTheme {
                themeBackground.value = MaterialTheme.colorScheme.background
                // Como lo componen hoy las siete pantallas que sí pintaban su raíz.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    HazloTopAppBar(title = "Mis rutas")
                }
            }
        }

        val painted = onNodeWithTag(HazloTopAppBarTags.BAR).captureToImage().cornerPixel()

        assertEquals(themeBackground.value, painted)
        assertTrue(themeBackground.value != hostile, "el fondo hostil no puede ser el del tema")
    }

    /**
     * El píxel de la esquina superior izquierda de la barra.
     *
     * Se mira una esquina y no el centro a propósito: el centro lo ocupan el título y los iconos, y
     * un píxel de tinta no dice nada del fondo. La esquina es lo primero que un fondo tiene que
     * cubrir y lo último que tapa cualquier contenido.
     */
    private fun ImageBitmap.cornerPixel(): Color = toPixelMap()[0, 0]
}
