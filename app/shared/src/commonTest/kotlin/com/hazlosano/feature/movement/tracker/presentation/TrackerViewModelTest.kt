package com.hazlosano.feature.movement.tracker.presentation

import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.LocationRepository
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository
import com.hazlosano.domain.feature.movement.usecase.SaveSessionUseCase
import com.hazlosano.domain.time.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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

private class FakeMovementSessionRepository : MovementSessionRepository {
    var savedSession: MovementSession? = null
    var savedPoints: List<UserLocation> = emptyList()

    override fun getAllSessions() = flowOf(emptyList<MovementSession>())
    override fun getSessionPoints(sessionId: Long) = flowOf(emptyList<UserLocation>())
    override suspend fun saveSession(session: MovementSession, rawPoints: List<UserLocation>) {
        savedSession = session
        savedPoints = rawPoints
    }
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
        sessionRepository: FakeMovementSessionRepository = FakeMovementSessionRepository(),
    ): TrackerViewModel =
        TrackerViewModel(
            locationRepository = FakeLocationRepository(updates),
            saveSession = SaveSessionUseCase(sessionRepository, TimeProvider { 0L }),
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

        assertFalse(viewModel.isRecording.value)
        assertEquals(0, viewModel.traveledPoints.value.size)
    }

    @Test
    fun recordingAccumulatesPathAndDistance() = runTest {
        val points = listOf(
            UserLocation(latitude = 19.4300, longitude = -99.1300, timestamp = 0),
            UserLocation(latitude = 19.4310, longitude = -99.1300, timestamp = 1000),
            UserLocation(latitude = 19.4320, longitude = -99.1300, timestamp = 2000),
        )
        val viewModel = buildViewModel(points.asFlow())

        viewModel.startRecording()
        viewModel.startTracking()
        runCurrent() // process the (delay-free) emissions without advancing the 1s timer
        viewModel.stopRecording()

        assertEquals(3, viewModel.traveledPoints.value.size)
        assertTrue(viewModel.distanceMeters.value > 0.0)
        assertFalse(viewModel.isRecording.value)
    }

    @Test
    fun locationIsNotRecordedWhenNotRecording() = runTest {
        val location = UserLocation(latitude = 19.4326, longitude = -99.1332)
        val viewModel = buildViewModel(flowOf(location))

        viewModel.startTracking()
        advanceUntilIdle()

        assertEquals(0, viewModel.traveledPoints.value.size)
        assertEquals(0.0, viewModel.distanceMeters.value)
    }

    @Test
    fun stoppingRecordingPersistsTheSession() = runTest {
        val points = listOf(
            UserLocation(latitude = 19.4300, longitude = -99.1300, timestamp = 0),
            UserLocation(latitude = 19.4310, longitude = -99.1300, timestamp = 1000),
            UserLocation(latitude = 19.4320, longitude = -99.1300, timestamp = 2000),
        )
        val sessionRepository = FakeMovementSessionRepository()
        val viewModel = buildViewModel(points.asFlow(), sessionRepository)

        viewModel.startRecording()
        viewModel.startTracking()
        runCurrent()
        viewModel.stopRecording()
        advanceUntilIdle()

        assertNotNull(sessionRepository.savedSession)
        assertEquals(3, sessionRepository.savedPoints.size)
        assertTrue(viewModel.sessionSaved.value)
    }
}
