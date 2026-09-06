package com.hazlosano.feature.movement.routes.presentation

import com.hazlosano.domain.feature.movement.model.RouteProblem

/**
 * Lo que la pantalla de rutas tiene que decir sobre lo último que pasó.
 *
 * Antes era un `String` ya redactado dentro del ViewModel. Eso pone la copia en la capa de
 * presentación, donde ningún catálogo de traducciones la alcanza, y obliga a las pruebas a afirmar
 * sobre la redacción — así que corregir una coma rompía un test.
 *
 * El nombre de la ruta sí viaja dentro del mensaje: no es copia, es un dato que la persona escribió.
 */
sealed interface RoutesMessage {
    data class Imported(val routeName: String) : RoutesMessage
    data class Replaced(val routeName: String) : RoutesMessage
    data object Renamed : RoutesMessage
    data object Deleted : RoutesMessage
    data object NameRequired : RoutesMessage

    /**
     * Reemplazar contestó que la ruta ya existía, que es justo lo que reemplazar viene a resolver.
     * No debería ocurrir; se dice en vez de callarse porque callarlo dejaría a la persona mirando
     * una lista que no cambió sin ninguna explicación.
     */
    data object ReplaceFailed : RoutesMessage

    data class Failed(val problem: RouteProblem) : RoutesMessage
}
