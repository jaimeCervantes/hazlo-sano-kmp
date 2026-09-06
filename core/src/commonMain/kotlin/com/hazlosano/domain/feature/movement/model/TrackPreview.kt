package com.hazlosano.domain.feature.movement.model

/**
 * Cuántos puntos se guardan de un recorrido para poder dibujar su silueta.
 *
 * Es suficiente para reconocer una forma en una tarjeta pequeña y muy poco al lado de lo que ocupa
 * la traza entera: una salida de dos horas son ~3.600 lecturas.
 */
const val MAX_PREVIEW_POINTS: Int = 200

/**
 * El recorrido reducido a lo que hace falta para dibujarlo.
 *
 * Se muestrea repartido a lo largo del recorrido y no se recorta por el principio: los últimos
 * puntos son parte de la forma tanto como los primeros.
 *
 * Vive en `core` y es genérica porque la necesitan dos cosas que no se parecen —una salida grabada y
 * una ruta guardada— y la regla es la misma. Estaba escrita dentro de `SaveSessionUseCase`; se sacó
 * aquí al necesitarla la segunda, en vez de copiarla.
 */
fun <T> List<T>.sampledForPreview(max: Int = MAX_PREVIEW_POINTS): List<T> {
    if (size <= max) return this
    val step = size / max
    return filterIndexed { index, _ -> index % step == 0 }
}
