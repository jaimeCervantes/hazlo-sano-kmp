package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.geo.degreesToRadians
import com.hazlosano.domain.geo.haversineMeters
import com.hazlosano.domain.time.TimeProvider
import com.hazlosano.domain.feature.movement.filter.KalmanFilter
import com.hazlosano.domain.feature.movement.model.NavigationState
import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.model.WayPoint
import com.hazlosano.domain.feature.movement.repository.LocationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlin.math.cos

private const val HARD_SNAP_THRESHOLD = 20.0
private const val SOFT_SNAP_THRESHOLD = 50.0
private const val OFF_ROUTE_THRESHOLD = 70.0
private const val MAX_PLAUSIBLE_SPEED_MPS = 40.0

class TrackNavigationUseCase(
    private val locationRepository: LocationRepository,
    private val calculateStatsUseCase: CalculateStatsUseCase,
    private val timeProvider: TimeProvider
) {
    private val kalmanFilter = KalmanFilter()
    private var traveledPoints = mutableListOf<UserLocation>()
    private var distanceTraveled = 0.0
    private var elevationGain = 0.0
    private var startTime = 0L

    operator fun invoke(targetRoute: Route?): Flow<NavigationState> = flow {
        startTime = timeProvider.nowMillis()
        traveledPoints.clear()
        kalmanFilter.reset()
        distanceTraveled = 0.0
        elevationGain = 0.0

        val locationFlow = locationRepository.getLocationUpdates()
            .onEach { rawLocation ->
                val (fLat, fLng) = kalmanFilter.filter(
                    rawLocation.latitude, rawLocation.longitude,
                    rawLocation.accuracy, rawLocation.timestamp
                )
                val filtered = rawLocation.copy(latitude = fLat, longitude = fLng)

                if (traveledPoints.isNotEmpty()) {
                    val last = traveledPoints.last()
                    val dist = calculateDistance(last, filtered)
                    val time = (filtered.timestamp - last.timestamp) / 1000.0
                    if (time > 0 && (dist / time) > MAX_PLAUSIBLE_SPEED_MPS) return@onEach
                }

                val finalLocation = if (targetRoute != null) applySmartSnap(filtered, targetRoute) else filtered

                if (traveledPoints.isNotEmpty()) {
                    val last = traveledPoints.last()
                    distanceTraveled += calculateDistance(last, finalLocation)
                    val altDiff = finalLocation.altitude - last.altitude
                    if (altDiff > 0) elevationGain += altDiff
                }
                traveledPoints.add(finalLocation)
            }

        val timerFlow = flow {
            while (true) {
                emit(timeProvider.nowMillis())
                delay(1000)
            }
        }

        combine(locationFlow, timerFlow) { location, currentTime ->
            val elapsedSeconds = (currentTime - startTime) / 1000
            val speed = if (elapsedSeconds > 0) (distanceTraveled / elapsedSeconds) * 3.6 else 0.0
            val currentPoints = traveledPoints.toList()

            NavigationState(
                traveledPoints = currentPoints,
                elapsedTime = elapsedSeconds,
                distanceTraveled = distanceTraveled,
                currentSpeed = speed,
                elevationGain = elevationGain,
                isOffRoute = calculateMinDistanceToRoute(
                    location,
                    targetRoute
                ) > OFF_ROUTE_THRESHOLD,
                stats = calculateStatsUseCase(currentPoints, elapsedSeconds)
            )
        }.collect { emit(it) }
    }

    private fun applySmartSnap(current: UserLocation, route: Route): UserLocation {
        if (route.points.isEmpty()) return current
        var minDistance = Double.MAX_VALUE
        var closestPoint: WayPoint? = null

        for (i in 0 until route.points.size - 1) {
            val projected = projectPointOnSegment(current, route.points[i], route.points[i+1])
            val dist = calculateDistance(current, projected)
            if (dist < minDistance) {
                minDistance = dist
                closestPoint = projected
            }
        }

        val target = closestPoint ?: return current

        return when {
            minDistance < HARD_SNAP_THRESHOLD -> current.copy(latitude = target.latitude, longitude = target.longitude)
            minDistance < SOFT_SNAP_THRESHOLD -> {
                val ratio = (minDistance - HARD_SNAP_THRESHOLD) / (SOFT_SNAP_THRESHOLD - HARD_SNAP_THRESHOLD)
                current.copy(
                    latitude = target.latitude * (1.0 - ratio) + current.latitude * ratio,
                    longitude = target.longitude * (1.0 - ratio) + current.longitude * ratio
                )
            }
            else -> current
        }
    }

    private fun calculateMinDistanceToRoute(current: UserLocation, route: Route?): Double {
        if (route == null || route.points.isEmpty()) return 0.0
        return route.points.minOf { calculateDistance(current, WayPoint(it.latitude, it.longitude)) }
    }

    private fun projectPointOnSegment(p: UserLocation, a: WayPoint, b: WayPoint): WayPoint {
        val latScale = cos(degreesToRadians(a.latitude))
        val px = p.longitude * latScale
        val py = p.latitude
        val ax = a.longitude * latScale
        val ay = a.latitude
        val bx = b.longitude * latScale
        val by = b.latitude
        val dx = bx - ax
        val dy = by - ay
        if (dx == 0.0 && dy == 0.0) return a
        val t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)
        return when {
            t <= 0.0 -> a
            t >= 1.0 -> b
            else -> WayPoint(ay + t * dy, (ax + t * dx) / latScale)
        }
    }

    private fun calculateDistance(p1: UserLocation, p2: UserLocation): Double {
        return haversineMeters(
            p1.latitude,
            p1.longitude,
            p2.latitude,
            p2.longitude
        )
    }

    private fun calculateDistance(p1: UserLocation, p2: WayPoint): Double {
        return haversineMeters(
            p1.latitude,
            p1.longitude,
            p2.latitude,
            p2.longitude
        )
    }
}
