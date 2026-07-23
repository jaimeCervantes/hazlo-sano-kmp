package com.hazlosano.domain.feature.movement.filter

import com.hazlosano.domain.geo.haversineMeters

/**
 * Filtro de Kalman Adaptativo para suavizar coordenadas GPS.
 * Ajusta dinámicamente el ruido de proceso basándose en la velocidad,
 * funcionando excelente tanto para caminar (lento) como para ir en bicicleta (rápido).
 */
class KalmanFilter(private val minProcessNoise: Double = 3.0) { // 3.0 m/s base (caminata)

    private var lat = 0.0
    private var lng = 0.0
    private var variance = -1.0 // -1 indica que no está inicializado
    private var lastTimestamp = 0L

    /**
     * Filtra una nueva coordenada y devuelve la posición suavizada.
     */
    fun filter(newLat: Double, newLng: Double, accuracy: Float, timestamp: Long): Pair<Double, Double> {
        val acc = accuracy.toDouble()
        if (variance < 0) {
            // Inicialización
            lat = newLat
            lng = newLng
            variance = acc * acc
            lastTimestamp = timestamp
            return Pair(lat, lng)
        }

        val durationSecs = (timestamp - lastTimestamp).coerceAtLeast(0L) / 1000.0
        if (durationSecs > 0) {
            // 1. Estimar velocidad para adaptar el filtro dinámicamente
            val distance = calculateDistance(lat, lng, newLat, newLng)
            val estimatedSpeed = distance / durationSecs

            // 2. Si el usuario va en bici, la velocidad será mayor.
            // Confiamos más en el movimiento rápido. Limitamos a 20.0 m/s (~72 km/h)
            // para que un "salto loco" del GPS no rompa el filtro.
            val dynamicProcessNoise = estimatedSpeed.coerceIn(minProcessNoise, 20.0)

            // 3. Aumentar la varianza según la incertidumbre del movimiento
            variance += durationSecs * dynamicProcessNoise * dynamicProcessNoise
            lastTimestamp = timestamp
        }

        // Ganancia de Kalman: K = Varianza / (Varianza + Varianza_Medida)
        val measurementVariance = acc * acc
        val k = variance / (variance + measurementVariance)

        // Actualizar estimación: X = X + K * (Medida - X)
        lat += k * (newLat - lat)
        lng += k * (newLng - lng)

        // Actualizar varianza: V = (1 - K) * V
        variance *= (1.0 - k)

        return Pair(lat, lng)
    }

    fun reset() {
        variance = -1.0
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return haversineMeters(lat1, lon1, lat2, lon2)
    }
}
