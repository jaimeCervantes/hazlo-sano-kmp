package com.hazlosano.feature.movement.tracker.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.domain.feature.movement.model.RecordingState
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.LocationRepository
import com.hazlosano.domain.feature.movement.repository.RecordingController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the tracker screen: streams the user's live location for the map and reflects the session
 * being recorded.
 *
 * The recording itself belongs to [RecordingController] — on Android a foreground service — so
 * leaving this screen, backgrounding the app or turning the screen off cannot end a session. This
 * ViewModel only observes it and forwards start/stop.
 */
class TrackerViewModel(
    private val locationRepository: LocationRepository,
    private val recordingController: RecordingController,
) : ViewModel() {

    private val _userLocation = MutableStateFlow<UserLocation?>(null)
    val userLocation: StateFlow<UserLocation?> = _userLocation.asStateFlow()

    val recording: StateFlow<RecordingState> = recordingController.state
    val lastSavedSession: StateFlow<RecordingState?> = recordingController.lastSavedSession

    private var tracking = false

    fun startTracking() {
        if (tracking) return
        tracking = true
        viewModelScope.launch {
            locationRepository.getLocationUpdates().collect { location ->
                _userLocation.value = location
            }
        }
    }

    fun startRecording() {
        recordingController.startRecording()
    }

    fun stopRecording() {
        recordingController.stopRecording()
    }

    fun acknowledgeSavedSession() {
        recordingController.acknowledgeSavedSession()
    }
}
