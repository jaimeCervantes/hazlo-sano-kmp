package com.hazlosano.feature.movement.ui

import com.hazlosano.feature.movement.presentation.MovementPlace
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.outings_title
import hazlosano.app.shared.generated.resources.routes_title
import hazlosano.app.shared.generated.resources.tracker_title
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Lo que dice la barra de arriba en cada sitio del pilar.
 *
 * Spec: `features/transiciones_del_pilar.feature`, slice 2.
 *
 * Los títulos vivían uno en cada pantalla. Al salir el cromo fuera de la animación se juntaron en una
 * tabla, y una tabla se puede leer del revés: esto la afirma para que mover la barra de sitio no haya
 * cambiado de paso lo que dice.
 */
class MovementPillarTitleTest {

    @Test
    fun `each place keeps the title it had`() {
        assertEquals(
            mapOf(
                MovementPlace.Record to Res.string.tracker_title,
                MovementPlace.Routes to Res.string.routes_title,
                MovementPlace.Outings to Res.string.outings_title,
            ),
            MovementPlace.entries.associateWith { it.titleResource() },
        )
    }

    /**
     * Grabar es el único que no se llama igual arriba que abajo: arriba dice «Movimiento», el nombre
     * del pilar, y abajo «Grabar», que es lo que se hace ahí.
     *
     * Se afirma porque es justo la clase de detalle que un refactor unifica sin querer, creyendo que
     * arregla una inconsistencia.
     */
    @Test
    fun `recording is titled after the pillar and not after the tab`() {
        assertEquals(Res.string.tracker_title, MovementPlace.Record.titleResource())
    }
}
