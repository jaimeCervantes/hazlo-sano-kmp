package com.hazlosano.domain.geo

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

fun haversineMeters(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val lat1Rad = degreesToRadians(lat1)
    val lat2Rad = degreesToRadians(lat2)
    val deltaLatRad = degreesToRadians(lat2 - lat1)
    val deltaLonRad = degreesToRadians(lon2 - lon1)

    val a = sin(deltaLatRad / 2.0).pow(2.0) +
        cos(lat1Rad) * cos(lat2Rad) *
        sin(deltaLonRad / 2.0).pow(2.0)
    val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))

    return EARTH_RADIUS_METERS * c
}

fun degreesToRadians(degrees: Double): Double = degrees * PI / 180.0
