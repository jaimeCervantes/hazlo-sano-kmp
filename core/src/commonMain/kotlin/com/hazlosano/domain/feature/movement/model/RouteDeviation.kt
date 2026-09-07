package com.hazlosano.domain.feature.movement.model

/**
 * Dónde estás respecto a la ruta que sigues.
 *
 * [Unknown] no es «estás bien»: es que no hay con qué responder — sin ruta, sin posición, o parado,
 * que es cuando la distancia al trazado deja de significar nada.
 */
sealed interface RouteStanding {
    data object Unknown : RouteStanding

    /** Dentro de lo que la señal puede equivocarse. [metersFromRoute] es a título informativo. */
    data class OnRoute(val metersFromRoute: Double) : RouteStanding

    /** Fuera del trazado, y lo bastante rato como para que no sea una lectura mala suelta. */
    data class OffRoute(val metersFromRoute: Double) : RouteStanding
}

/**
 * Decide si te has salido de la ruta, y lo dice sólo cuando merece la pena decirlo.
 *
 * **Los umbrales salen de medir.** Se replayaron las nueve trazas de campo comparando la señal cruda
 * contra el camino que de verdad se recorrió: moviéndose, el receptor nunca se aleja más de 21,3 m
 * del sitio por el que se pasó (p90 entre 1,4 y 11,7 m). [TOLERANCE_METERS] deja algo más del doble
 * de holgura sobre ese peor caso, y [PERSISTENCE_MILLIS] la afinó una salida siguiendo una ruta de
 * verdad.
 *
 * **Parado no se juzga, y esa regla la produjo una traza.** La cuarta captura —media hora con el
 * teléfono en una mesa bajo techo— coloca la señal hasta a **291 m** del sitio donde estaba. Parado,
 * la distancia al trazado es ruido, así que quien alimenta esto tiene que decir si hay movimiento;
 * sin él, la respuesta es [RouteStanding.Unknown] en vez de un desvío inventado.
 *
 * **Se mide y se avisa; no se corrige.** La implementación de referencia movía la posición sobre la
 * ruta cuando estaba cerca («smart snap»). Aquí no: lo que se graba tiene que ser por dónde fuiste.
 *
 * Es una clase con estado —igual que `LocationFilter`— porque la persistencia sólo se puede saber
 * recordando desde cuándo estás fuera. Se asume alimentada desde un solo hilo, como el filtro.
 */
class RouteDeviation(
    private val route: List<WayPoint>,
    private val toleranceMeters: Double = TOLERANCE_METERS,
    private val persistenceMillis: Long = PERSISTENCE_MILLIS,
) {
    /** Desde cuándo la distancia no baja del umbral, o `null` si ahora mismo está dentro. */
    private var strayingSince: Long? = null

    /**
     * @param moving si la grabación considera que te estás moviendo. Parado, no se juzga: ver la
     * nota de la clase sobre los 291 m de la cuarta traza.
     */
    fun standing(location: UserLocation, moving: Boolean): RouteStanding {
        // Un solo punto no es un trazado: no hay segmento contra el que medir.
        if (route.size < 2) return RouteStanding.Unknown

        if (!moving) {
            // La racha se olvida: al volver a moverse se empieza a contar de nuevo, en vez de
            // arrastrar el tiempo que se estuvo parado a 300 m por culpa de la deriva.
            strayingSince = null
            return RouteStanding.Unknown
        }

        val meters = distanceToRouteMeters(location.latitude, location.longitude, route)
            ?: return RouteStanding.Unknown

        if (meters <= toleranceMeters) {
            // Volver calla el aviso de inmediato: no hay nada que confirmar en estar donde debes.
            strayingSince = null
            return RouteStanding.OnRoute(meters)
        }

        val since = strayingSince ?: location.timestamp.also { strayingSince = it }
        return if (location.timestamp - since >= persistenceMillis) {
            RouteStanding.OffRoute(meters)
        } else {
            // Fuera, pero todavía puede ser una lectura mala: no se dice hasta que aguante.
            RouteStanding.OnRoute(meters)
        }
    }

    companion object {
        /**
         * A partir de aquí se considera que estás fuera del trazado.
         *
         * 50 m sobre un peor caso medido de 19,4 m. Coincide además con
         * `LocationFilter.MAX_USABLE_ACCURACY_METERS`: una lectura que el filtro se cree siquiera
         * declara como mucho esa precisión, así que por debajo de este umbral no se puede distinguir
         * un desvío de la incertidumbre de la propia lectura.
         */
        const val TOLERANCE_METERS: Double = 50.0

        /**
         * Cuánto tiene que aguantar el desvío antes de decirse.
         *
         * **Estaba en 30 s sin calibrar, y la salida de campo lo movió.** Se salió del trazado a
         * propósito en bici: la señal cruzó los 50 m, llegó a 77,7 m y volvió — **18 s fuera del
         * umbral**. Con 30 s el aviso no llegó a hablar, que es justo el fallo que la salida existía
         * para buscar.
         *
         * El otro lado —que una lectura mala suelta no dispare nada— resultó estar sostenido por
         * [TOLERANCE_METERS] y no por este número: en las nueve trazas, moviéndose, la señal cruda
         * nunca superó los 21,3 m respecto al camino recorrido, y **ninguna lectura de ninguna traza
         * en movimiento pasó de los 50 m**. Replayando contra una ruta real, la salida que la
         * recorrió sin desviarse no produce un solo aviso ni con la persistencia a cero.
         *
         * Así que el coste medido de bajarla es cero y la ganancia es no callarse ante un desvío
         * real. A las velocidades observadas (3-5 m/s en bici), 15 s son ~60 m más de camino
         * equivocado en vez de ~120 m.
         *
         * **Lo que sigue sin cubrir:** un desvío más corto que esto se escapa igual, y el que se
         * midió duró 18 s — el margen es de una o dos lecturas. No se bajó más porque la persistencia
         * se cuenta en tiempo y no en lecturas: con un receptor que reporte cada 10 s en vez de cada
         * 2, un número más pequeño dejaría de ser varias lecturas y pasaría a ser una.
         */
        const val PERSISTENCE_MILLIS: Long = 15_000L
    }
}
