package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.geo.haversineMeters
import com.hazlosano.domain.feature.movement.model.SessionStats
import com.hazlosano.domain.feature.movement.model.UserLocation
import kotlin.math.abs

class CalculateStatsUseCase {

    operator fun invoke(points: List<UserLocation>, totalTimeSeconds: Long): SessionStats {
        if (points.isEmpty()) return SessionStats()

        var maxAlt = points.first().altitude
        var minAlt = points.first().altitude
        var movingTime = 0L
        var totalAscent = 0.0
        var totalDescent = 0.0
        var maxSlope = 0.0
        var currentSlope = 0.0

        val altitudeDist = mutableMapOf<Int, Long>()

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]

            if (p2.altitude > maxAlt) maxAlt = p2.altitude
            if (p2.altitude < minAlt) minAlt = p2.altitude

            val dist = calculateDistance(p1, p2)
            val timeDiff = (p2.timestamp - p1.timestamp) / 1000L

            if (dist > 0.5) { // Moving threshold
                movingTime += timeDiff

                val altDiff = p2.altitude - p1.altitude
                if (altDiff > 0) totalAscent += altDiff else totalDescent += abs(altDiff)

                if (dist > 2.0) {
                    val slope = (altDiff / dist) * 100.0
                    if (abs(slope) > abs(maxSlope)) maxSlope = slope

                    // Current slope (using last segment for now, ideally smoothed)
                    if (i == points.size - 2) currentSlope = slope
                }
            }

            val range = (p1.altitude / 500).toInt() * 500
            altitudeDist[range] = (altitudeDist[range] ?: 0L) + timeDiff
        }

        val totalDistance = calculateTotalDistance(points)
        val avgSlope = if (totalDistance > 0) (totalAscent / totalDistance) * 100.0 else 0.0
        val vam = if (movingTime > 30) (totalAscent / (movingTime / 3600.0)) else 0.0

        // Pace calculation (min/km)
        val avgPace = if (totalDistance > 10) (totalTimeSeconds / 60.0) / (totalDistance / 1000.0) else 0.0

        // Current Pace (last 30 seconds or last 5 points)
        val currentPace = calculateCurrentPace(points)

        return SessionStats(
            maxAltitude = maxAlt,
            minAltitude = minAlt,
            totalAscent = totalAscent,
            totalDescent = totalDescent,
            avgSlope = avgSlope,
            maxSlope = maxSlope,
            currentSlope = currentSlope,
            movingTime = movingTime,
            vam = vam,
            currentPace = currentPace,
            avgPace = avgPace,
            altitudeDistribution = altitudeDist
        )
    }

    private fun calculateCurrentPace(points: List<UserLocation>): Double {
        if (points.size < 5) return 0.0
        val lastPoints = points.takeLast(5)
        var dist = 0.0
        for (i in 0 until lastPoints.size - 1) {
            dist += calculateDistance(lastPoints[i], lastPoints[i+1])
        }
        val time = (lastPoints.last().timestamp - lastPoints.first().timestamp) / 1000.0
        return if (dist > 5.0 && time > 0) (time / 60.0) / (dist / 1000.0) else 0.0
    }

    private fun calculateTotalDistance(points: List<UserLocation>): Double {
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += calculateDistance(points[i], points[i+1])
        }
        return total
    }

    private fun calculateDistance(p1: UserLocation, p2: UserLocation): Double {
        return haversineMeters(
            p1.latitude,
            p1.longitude,
            p2.latitude,
            p2.longitude
        )
    }
}
