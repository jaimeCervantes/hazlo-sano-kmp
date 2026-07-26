package com.hazlosano.domain.feature.movement.model

/**
 * Climb and descent accumulated with hysteresis.
 *
 * Adding up every rise between consecutive readings is what makes a flat outing report hundreds of
 * metres of climb: altitude is the noisiest thing a GPS reports, so half of those rises never
 * happened. Instead a reference altitude is kept, and only a change that clears [thresholdMeters]
 * counts — at which point the reference moves with it, so a long steady climb still adds up to its
 * real height while noise around one altitude adds nothing.
 *
 * The threshold is a parameter rather than a constant because it belongs to the quality of the fix
 * that reported the altitude, not to the terrain.
 */
data class Elevation(
    val ascentMeters: Double = 0.0,
    val descentMeters: Double = 0.0,
    private val referenceMeters: Double? = null,
) {
    fun accumulating(altitudeMeters: Double, thresholdMeters: Double): Elevation {
        val reference = referenceMeters ?: return copy(referenceMeters = altitudeMeters)

        val change = altitudeMeters - reference
        if (change > thresholdMeters) {
            return copy(ascentMeters = ascentMeters + change, referenceMeters = altitudeMeters)
        }
        if (change < -thresholdMeters) {
            return copy(descentMeters = descentMeters - change, referenceMeters = altitudeMeters)
        }
        return this
    }
}
