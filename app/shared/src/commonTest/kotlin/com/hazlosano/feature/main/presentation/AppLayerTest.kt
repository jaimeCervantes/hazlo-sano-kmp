package com.hazlosano.feature.main.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Qué hay encima de la pantalla principal — la pregunta que contestan a la vez lo que se pinta y lo
 * que deshace el gesto de volver atrás.
 *
 * Lo que se afirma aquí es **el orden**, porque es lo único que puede estar mal y lo que un refactor
 * mueve sin darse cuenta. Que las dos cosas usen la misma respuesta es lo que impide que se separen;
 * que la respuesta sea la correcta es lo que se prueba abajo.
 */
class AppLayerTest {

    private fun layer(
        settings: Boolean = false,
        movement: Boolean = false,
        pillarInfo: Boolean = false,
        sleepHistory: Boolean = false,
    ) = topLayer(
        settingsOpen = settings,
        movementOpen = movement,
        pillarInfoOpen = pillarInfo,
        sleepHistoryOpen = sleepHistory,
    )

    /**
     * Sin nada abierto no hay nada que deshacer, y volver atrás vuelve a ser del sistema: sale del
     * app, que es lo que espera quien lo hace desde la pantalla principal.
     */
    @Test
    fun `the main screen has nothing on top of it`() {
        assertNull(layer())
    }

    @Test
    fun `each thing that opens is what is on top`() {
        assertEquals(AppLayer.Settings, layer(settings = true))
        assertEquals(AppLayer.Movement, layer(movement = true))
        assertEquals(AppLayer.PillarInfo, layer(pillarInfo = true))
        assertEquals(AppLayer.SleepHistory, layer(sleepHistory = true))
    }

    /**
     * Ajustes tapa a todo lo demás, y es el caso que costó un fallo.
     *
     * Se abre **desde** cualquier sitio, incluido el pilar de Movimiento, así que tiene que ganar. En
     * el slice 3 estaba comprobado por debajo del pilar: se ponía la bandera, el pilar seguía
     * devolviendo su pantalla, y Ajustes no llegaba a pintarse nunca.
     */
    @Test
    fun `settings covers everything including the movement pillar`() {
        assertEquals(
            AppLayer.Settings,
            layer(settings = true, movement = true, pillarInfo = true, sleepHistory = true),
        )
    }

    /** Y el pilar tapa a la ficha de un pilar, por lo mismo: se entra al pilar desde ella. */
    @Test
    fun `the movement pillar covers the pillar sheet and the sleep history`() {
        assertEquals(AppLayer.Movement, layer(movement = true, pillarInfo = true))
        assertEquals(AppLayer.Movement, layer(movement = true, sleepHistory = true))
    }

    @Test
    fun `the pillar sheet covers the sleep history`() {
        assertEquals(AppLayer.PillarInfo, layer(pillarInfo = true, sleepHistory = true))
    }

    /**
     * El orden entero, dicho de una vez.
     *
     * Se comprueba quitando capas de arriba abajo: con las cuatro abiertas manda la primera, y al
     * cerrarla manda la siguiente. Es lo que hace el gesto de volver atrás pulsado cuatro veces, y
     * afirmarlo así es afirmar que deshace las cosas en el orden inverso al que se hicieron.
     */
    @Test
    fun `going back peels the layers in order`() {
        val peeled = mutableListOf<AppLayer?>()
        var settings = true
        var movement = true
        var pillarInfo = true
        var sleepHistory = true

        repeat(5) {
            val top = layer(settings, movement, pillarInfo, sleepHistory)
            peeled += top
            when (top) {
                AppLayer.Settings -> settings = false
                AppLayer.Movement -> movement = false
                AppLayer.PillarInfo -> pillarInfo = false
                AppLayer.SleepHistory -> sleepHistory = false
                null -> Unit
            }
        }

        assertEquals(
            listOf(
                AppLayer.Settings,
                AppLayer.Movement,
                AppLayer.PillarInfo,
                AppLayer.SleepHistory,
                null,
            ),
            peeled,
        )
    }
}
