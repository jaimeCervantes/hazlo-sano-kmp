package com.hazlosano.domain.feature.movement.model

/**
 * Geographic bounding box of a set of points, used to frame a recorded path on a map.
 *
 * Kept in the domain (instead of inside the platform map code) so the framing decision is testable
 * without a map. Does not handle paths that cross the antimeridian: a recorded session spans a few
 * kilometers, so treating longitude as a plain interval is accurate enough here.
 */
data class GeoBounds(
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double,
) {
    val centerLatitude: Double
        get() = (minLatitude + maxLatitude) / 2

    val centerLongitude: Double
        get() = (minLongitude + maxLongitude) / 2

    /** False when every point sits at the same coordinate, where a map cannot fit an area. */
    val spansAnArea: Boolean
        get() = maxLatitude > minLatitude || maxLongitude > minLongitude
}

/** Bounds covering every point, or null when there is nothing to frame. */
fun List<UserLocation>.boundingBox(): GeoBounds? {
    if (isEmpty()) return null

    var minLatitude = first().latitude
    var maxLatitude = minLatitude
    var minLongitude = first().longitude
    var maxLongitude = minLongitude

    for (point in this) {
        if (point.latitude < minLatitude) minLatitude = point.latitude
        if (point.latitude > maxLatitude) maxLatitude = point.latitude
        if (point.longitude < minLongitude) minLongitude = point.longitude
        if (point.longitude > maxLongitude) maxLongitude = point.longitude
    }

    return GeoBounds(
        minLatitude = minLatitude,
        minLongitude = minLongitude,
        maxLatitude = maxLatitude,
        maxLongitude = maxLongitude,
    )
}
