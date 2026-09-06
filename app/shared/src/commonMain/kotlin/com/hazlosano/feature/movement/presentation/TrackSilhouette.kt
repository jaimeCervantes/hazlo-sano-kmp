package com.hazlosano.feature.movement.presentation

import com.hazlosano.domain.feature.movement.model.UserLocation
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max

/** Un punto de la silueta, ya en el cuadrado unidad: 0 arriba-izquierda, 1 abajo-derecha. */
data class SilhouettePoint(val x: Float, val y: Float)

/**
 * La forma de un recorrido, lista para dibujarse en cualquier tamaño.
 *
 * Se calcula aquí y no en el `Canvas` porque es una regla y no un pintado: proyectar, encuadrar y
 * conservar la proporción son decisiones que se pueden equivocar, y dentro de un `@Composable` no se
 * pueden probar sin levantar una pantalla.
 */
data class TrackSilhouette(val points: List<SilhouettePoint>) {
    /** Una silueta de un solo punto no dibuja nada: hace falta ir de algún sitio a otro. */
    val isDrawable: Boolean get() = points.size >= 2
}

/**
 * El grado de longitud se estrecha según se sube en latitud, así que sin corregirlo un recorrido de
 * este a oeste sale más ancho de lo que fue. La corrección es el coseno de la latitud media, que es
 * la aproximación que usa cualquier mapa a esta escala — la silueta de una salida cabe en unos
 * kilómetros, donde la curvatura de la Tierra no se nota.
 */
private fun longitudeScaleAt(latitudeDegrees: Double): Double =
    cos(latitudeDegrees * PI / 180.0).coerceAtLeast(MIN_LONGITUDE_SCALE)

/** Por debajo de esto sólo hay polos, y la escala se dispararía dividiendo por casi cero. */
private const val MIN_LONGITUDE_SCALE = 0.01

/**
 * Proyecta un recorrido en el cuadrado unidad, encuadrándolo y **conservando su proporción**.
 *
 * Estirar la silueta hasta llenar la tarjeta haría que un ida y vuelta en línea recta se viera como
 * un circuito: la forma es lo único que esta silueta comunica, así que deformarla es mentir. El
 * recorrido se centra en el eje que le sobra.
 */
fun List<UserLocation>.toSilhouette(): TrackSilhouette =
    silhouetteOf(map { it.latitude to it.longitude })

/**
 * La misma proyeccion, sobre coordenadas sueltas.
 *
 * Toma pares y no un tipo del dominio porque la necesitan dos cosas distintas -una salida guarda
 * `UserLocation` y una ruta `WayPoint`- y en Kotlin dos extensiones de `List<T>` con distinto `T`
 * chocan al compilar. Lo que comparten es la geometria, no el tipo del punto.
 */
fun silhouetteOf(coordinates: List<Pair<Double, Double>>): TrackSilhouette {
    if (coordinates.size < 2) return TrackSilhouette(emptyList())

    val latitudes = coordinates.map { it.first }
    val longitudes = coordinates.map { it.second }
    val minLatitude = latitudes.min()
    val maxLatitude = latitudes.max()
    val minLongitude = longitudes.min()
    val maxLongitude = longitudes.max()

    val longitudeScale = longitudeScaleAt((minLatitude + maxLatitude) / 2.0)
    val width = (maxLongitude - minLongitude) * longitudeScale
    val height = maxLatitude - minLatitude
    val span = max(width, height)

    // Todas las lecturas en el mismo sitio: hay puntos, pero no hay recorrido que dibujar.
    if (span <= 0.0) return TrackSilhouette(emptyList())

    // Lo que le sobra al eje corto se reparte a los dos lados, para que quede centrado.
    val horizontalPadding = (span - width) / 2.0
    val verticalPadding = (span - height) / 2.0

    return TrackSilhouette(
        coordinates.map { (latitude, longitude) ->
            val x = ((longitude - minLongitude) * longitudeScale + horizontalPadding) / span
            // La latitud crece hacia el norte y la pantalla hacia abajo, así que el eje se invierte.
            val y = 1.0 - ((latitude - minLatitude) + verticalPadding) / span
            SilhouettePoint(
                x = x.coerceIn(0.0, 1.0).toFloat(),
                y = y.coerceIn(0.0, 1.0).toFloat(),
            )
        },
    )
}
