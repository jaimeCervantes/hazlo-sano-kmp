package com.hazlosano.feature.movement.tracker.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.domain.feature.movement.model.NavigationState
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.LocationRepository
import com.hazlosano.domain.feature.movement.usecase.CalculateStatsUseCase
import com.hazlosano.domain.feature.movement.usecase.SaveSessionUseCase
import com.hazlosano.domain.geo.haversineMeters
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the tracker: streams the user's live location and, while a session is being recorded,
 * accumulates the traveled path, distance and elapsed time.
 *
 * Collection starts only once [startTracking] is called (after the location permission is granted)
 * and is idempotent. Persistence of the finished session is handled by a later slice.
 */
class TrackerViewModel(
    private val locationRepository: LocationRepository,
    private val saveSession: SaveSessionUseCase,
    private val calculateStats: CalculateStatsUseCase = CalculateStatsUseCase(),
) : ViewModel() {

    private val _userLocation = MutableStateFlow<UserLocation?>(null)
    val userLocation: StateFlow<UserLocation?> = _userLocation.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _sessionSaved = MutableStateFlow(false)
    val sessionSaved: StateFlow<Boolean> = _sessionSaved.asStateFlow()

    private val _traveledPoints = MutableStateFlow<List<UserLocation>>(emptyList())
    val traveledPoints: StateFlow<List<UserLocation>> = _traveledPoints.asStateFlow()

    private val _distanceMeters = MutableStateFlow(0.0)
    val distanceMeters: StateFlow<Double> = _distanceMeters.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private var tracking = false
    private var timerJob: Job? = null

    fun startTracking() {
        if (tracking) return
        tracking = true
        viewModelScope.launch {
            locationRepository.getLocationUpdates().collect { location ->
                _userLocation.value = location
                if (_isRecording.value) appendTraveled(location)
            }
        }
    }

    fun startRecording() {
        if (_isRecording.value) return
        _traveledPoints.value = emptyList()
        _distanceMeters.value = 0.0
        _elapsedSeconds.value = 0L
        _sessionSaved.value = false
        _isRecording.value = true
        _userLocation.value?.let(::appendTraveled)
        timerJob = viewModelScope.launch {
            while (_isRecording.value) {
                delay(1000)
                _elapsedSeconds.value += 1
            }
        }
    }

    fun stopRecording() {
        if (!_isRecording.value) return
        _isRecording.value = false
        timerJob?.cancel()
        timerJob = null

        val points = _traveledPoints.value
        if (points.isEmpty()) return

        val elapsed = _elapsedSeconds.value
        val distance = _distanceMeters.value
        viewModelScope.launch {
            val stats = calculateStats(points, elapsed)
            val state = NavigationState(
                traveledPoints = points,
                elapsedTime = elapsed,
                distanceTraveled = distance,
                elevationGain = stats.totalAscent,
                stats = stats,
            )
            saveSession(name = "Sesión de movimiento", routeId = null, state = state)
            _sessionSaved.value = true
        }
    }

    private fun appendTraveled(location: UserLocation) {
        _traveledPoints.value.lastOrNull()?.let { previous ->
            _distanceMeters.value += haversineMeters(
                previous.latitude,
                previous.longitude,
                location.latitude,
                location.longitude,
            )
        }
        _traveledPoints.value = _traveledPoints.value + location
    }
}
