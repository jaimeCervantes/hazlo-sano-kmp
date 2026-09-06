package com.hazlosano.feature.movement.tracker.presentation

import com.hazlosano.domain.feature.movement.model.RecordingState
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.LocationRepository
import com.hazlosano.domain.feature.movement.repository.RecordingController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeLocationRepository(
    private val updates: Flow<UserLocation>,
) : LocationRepository {
    override fun getLocationUpdates(): Flow<UserLocation> = updates
}

@OptIn(ExperimentalCoroutinesApi::class)
class TrackerViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        updates: Flow<UserLocation> = flowOf(),
        controller: FakeRecordingController = FakeRecordingController(),
    ): TrackerViewModel =
        TrackerViewModel(
            locationRepository = FakeLocationRepository(updates),
            recordingController = controller,
            routes = FakeRoutes(),
        )

    @Test
    fun exposesLocationAfterStartTracking() = runTest {
        val location = UserLocation(latitude = 19.4326, longitude = -99.1332)
        val viewModel = buildViewModel(flowOf(location))

        viewModel.startTracking()
        advanceUntilIdle()

        assertEquals(location, viewModel.userLocation.value)
    }

    @Test
    fun startsNotRecording() {
        val viewModel = buildViewModel()

        assertFalse(viewModel.recording.value.isRecording)
        assertEquals(0, viewModel.recording.value.traveledPoints.size)
    }

    @Test
    fun handsRecordingOverToTheController() {
        val controller = FakeRecordingController()
        val viewModel = buildViewModel(controller = controller)

        viewModel.startRecording()

        assertEquals(1, controller.startCount)
        assertTrue(viewModel.recording.value.isRecording)
    }

    @Test
    fun reflectsWhatTheControllerRecordedWhileTheScreenWasAway() {
        val controller = FakeRecordingController()
        val viewModel = buildViewModel(controller = controller)
        viewModel.startRecording()

        // The service kept recording with the app in the background.
        controller.publish(
            RecordingState(
                isRecording = true,
                startedAtMillis = 0,
                traveledPoints = listOf(
                    UserLocation(latitude = 19.4300, longitude = -99.1300),
                    UserLocation(latitude = 19.4310, longitude = -99.1300),
                ),
                distanceMeters = 111.0,
                elapsedSeconds = 420,
            ),
        )

        val recording = viewModel.recording.value
        assertTrue(recording.isRecording)
        assertEquals(2, recording.traveledPoints.size)
        assertEquals(111.0, recording.distanceMeters)
        assertEquals(420, recording.elapsedSeconds)
    }

    @Test
    fun stoppingAsksTheControllerToStopAndSurfacesTheSavedSession() {
        val controller = FakeRecordingController()
        val viewModel = buildViewModel(controller = controller)
        viewModel.startRecording()

        viewModel.stopRecording()

        assertEquals(1, controller.stopCount)
        assertFalse(viewModel.recording.value.isRecording)
        assertNotNull(viewModel.lastSavedSession.value)
    }

    @Test
    fun stopsShowingTheFinishedRouteOnTheMap() {
        val controller = FakeRecordingController()
        val viewModel = buildViewModel(controller = controller)
        viewModel.startRecording()
        controller.publish(
            RecordingState(
                isRecording = true,
                startedAtMillis = 0,
                traveledPoints = listOf(
                    UserLocation(latitude = 19.4300, longitude = -99.1300),
                    UserLocation(latitude = 19.4310, longitude = -99.1300),
                ),
                distanceMeters = 111.0,
                elapsedSeconds = 60,
            ),
        )

        viewModel.stopRecording()

        // What the map draws must be empty once the session ended, however long the screen lives.
        assertEquals(0, viewModel.recording.value.traveledPoints.size)
    }

    @Test
    fun forgetsTheConfirmationOnceTheScreenShowedIt() {
        val controller = FakeRecordingController()
        val viewModel = buildViewModel(controller = controller)
        viewModel.startRecording()
        viewModel.stopRecording()

        viewModel.acknowledgeSavedSession()

        assertNull(viewModel.lastSavedSession.value)
    }

    @Test
    fun recordsWithoutATraceUnlessItWasAskedFor() {
        val controller = FakeRecordingController()
        val viewModel = buildViewModel(controller = controller)

        viewModel.startRecording()

        assertEquals(false, controller.startedWithTraceCapture)
    }

    @Test
    fun carriesTheTraceCaptureChoiceIntoTheRecordingItStarts() {
        val controller = FakeRecordingController()
        val viewModel = buildViewModel(controller = controller)

        viewModel.setCaptureTrace(true)
        viewModel.startRecording()

        assertEquals(true, viewModel.captureTrace.value)
        assertEquals(true, controller.startedWithTraceCapture)
    }

    @Test
    fun locationUpdatesDoNotFeedTheRecording() = runTest {
        val controller = FakeRecordingController()
        val viewModel = buildViewModel(
            flowOf(UserLocation(latitude = 19.4326, longitude = -99.1332)),
            controller,
        )

        viewModel.startTracking()
        advanceUntilIdle()

        // The recording collects its own locations in the service; the screen only draws them.
        assertEquals(0, viewModel.recording.value.traveledPoints.size)
    }
}
