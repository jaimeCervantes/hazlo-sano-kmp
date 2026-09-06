package com.hazlosano.domain.feature.movement.model

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * A qué distancia estás del trazado de una ruta.
 *
 * Es la pregunta que hay debajo de «te has salido»: no la distancia al punto más cercano de la ruta,
 * sino al **segmento** más cercano. La diferencia importa cuando la ruta tiene los puntos separados
 * —un GPX dibujado a mano puede tener uno cada 200 m— y estás en medio de un tramo recto: por
 * puntos parecerías estar a 100 m de la ruta, cuando estás encima de ella.
 */

/** Por debajo de esto sólo hay polos, y la escala se dispararía dividiendo por casi cero. */
private const val MIN_LONGITUDE_SCALE = 0.01

/** Metros por grado de latitud. Constante suficiente a la escala de una salida. */
private const val METERS_PER_DEGREE_LATITUDE = 111_320.0

/**
 * La distancia en metros del punto al trazado, o `null` si la ruta no describe ningún recorrido.
 *
 * Se proyecta en un plano local en vez de usar haversine sobre cada segmento: a la escala de una
 * salida —unos kilómetros— la diferencia es de centímetros, y a cambio la proyección de un punto
 * sobre un segmento es aritmética simple en vez de trigonometría esférica. La longitud se corrige
 * por el coseno de la latitud, que es lo que la hace comparable con la latitud en metros.
 */
fun distanceToRouteMeters(
    latitude: Double,
    longitude: Double,
    route: List<WayPoint>,
): Double? {
    if (route.isEmpty()) return null
    if (route.size == 1) {
        return distanceBetween(latitude, longitude, route[0].latitude, route[0].longitude)
    }

    var closest = Double.MAX_VALUE
    for (index in 0 until route.size - 1) {
        val distance = distanceToSegmentMeters(
            latitude = latitude,
            longitude = longitude,
            fromLatitude = route[index].latitude,
            fromLongitude = route[index].longitude,
            toLatitude = route[index + 1].latitude,
            toLongitude = route[index + 1].longitude,
        )
        if (distance < closest) closest = distance
    }
    return closest
}

/**
 * La distancia del punto al segmento, no a sus extremos.
 *
 * Se proyecta el punto sobre la recta del segmento y se recorta la proyección a `[0, 1]`: fuera de
 * ese rango el punto más cercano del segmento es uno de sus extremos, que es justo lo que el recorte
 * elige.
 */
private fun distanceToSegmentMeters(
    latitude: Double,
    longitude: Double,
    fromLatitude: Double,
    fromLongitude: Double,
    toLatitude: Double,
    toLongitude: Double,
): Double {
    val longitudeScale = longitudeScaleAt((fromLatitude + toLatitude) / 2.0)

    val pointX = (longitude - fromLongitude) * longitudeScale * METERS_PER_DEGREE_LATITUDE
    val pointY = (latitude - fromLatitude) * METERS_PER_DEGREE_LATITUDE
    val segmentX = (toLongitude - fromLongitude) * longitudeScale * METERS_PER_DEGREE_LATITUDE
    val segmentY = (toLatitude - fromLatitude) * METERS_PER_DEGREE_LATITUDE

    val segmentLengthSquared = segmentX * segmentX + segmentY * segmentY
    // Un segmento de longitud cero son dos puntos repetidos: la distancia es al punto.
    if (segmentLengthSquared == 0.0) return sqrt(pointX * pointX + pointY * pointY)

    val projection = ((pointX * segmentX + pointY * segmentY) / segmentLengthSquared)
        .coerceIn(0.0, 1.0)
    val closestX = projection * segmentX
    val closestY = projection * segmentY

    val dx = pointX - closestX
    val dy = pointY - closestY
    return sqrt(dx * dx + dy * dy)
}

private fun distanceBetween(
    latitude: Double,
    longitude: Double,
    otherLatitude: Double,
    otherLongitude: Double,
): Double {
    val longitudeScale = longitudeScaleAt((latitude + otherLatitude) / 2.0)
    val dx = (longitude - otherLongitude) * longitudeScale * METERS_PER_DEGREE_LATITUDE
    val dy = (latitude - otherLatitude) * METERS_PER_DEGREE_LATITUDE
    return sqrt(dx * dx + dy * dy)
}

private fun longitudeScaleAt(latitudeDegrees: Double): Double =
    cos(latitudeDegrees * PI / 180.0).coerceAtLeast(MIN_LONGITUDE_SCALE)
