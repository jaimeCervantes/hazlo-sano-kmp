package com.hazlosano.data.movement

import com.hazlosano.domain.feature.movement.model.WayPoint

/**
 * Cómo viaja a la base la silueta de una ruta.
 *
 * Es el mismo formato que [TrackPreviewFormat] usa para las salidas —pares separados por `;`— y sin
 * embargo es otro objeto, porque el tipo del punto es otro: una salida guarda `UserLocation` y una
 * ruta `WayPoint`, y son distintos a propósito (un punto de ruta puede no traer hora ni altitud).
 *
 * Se comparte lo que de verdad se comparte, que es la **regla del muestreo** —`sampledForPreview`,
 * en `core`—; unificar además la codificación obligaría a un tipo intermedio que sólo existiría para
 * que estos dos archivos se parecieran.
 */
internal object RoutePreviewFormat {

    private const val POINT_SEPARATOR = ';'
    private const val COORDINATE_SEPARATOR = ','

    fun encode(points: List<WayPoint>): String? {
        if (points.isEmpty()) return null
        return points.joinToString(POINT_SEPARATOR.toString()) { point ->
            "${point.latitude}$COORDINATE_SEPARATOR${point.longitude}"
        }
    }

    /**
     * Lo que se pueda leer, y nada más: un par ilegible se salta en vez de tumbar la lista. La
     * silueta es decoración, y una columna a medio escribir no puede costar las rutas.
     */
    fun decode(stored: String?): List<WayPoint> {
        if (stored.isNullOrBlank()) return emptyList()
        return stored.split(POINT_SEPARATOR).mapNotNull { pair ->
            val parts = pair.split(COORDINATE_SEPARATOR)
            if (parts.size != 2) return@mapNotNull null
            val latitude = parts[0].toDoubleOrNull() ?: return@mapNotNull null
            val longitude = parts[1].toDoubleOrNull() ?: return@mapNotNull null
            WayPoint(latitude = latitude, longitude = longitude)
        }
    }
}
