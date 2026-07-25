package com.hazlosano.data.movement

import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.LocationRepository
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository
import com.hazlosano.domain.feature.movement.usecase.SaveSessionUseCase
import com.hazlosano.domain.time.TimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
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

private class RecordingMovementSessionRepository : MovementSessionRepository {
    var savedSession: MovementSession? = null
    var savedPoints: List<UserLocation> = emptyList()

    override fun getAllSessions() = flowOf(emptyList<MovementSession>())
    override fun getSessionPoints(sessionId: Long) = flowOf(emptyList<UserLocation>())
    override suspend fun saveSession(session: MovementSession, rawPoints: List<UserLocation>) {
        savedSession = session
        savedPoints = rawPoints
    }
}

/** A clock the test moves by hand, so elapsed time does not depend on how long the test ran. */
private class FakeClock(var nowMillis: Long = 0) : TimeProvider {
    override fun nowMillis(): Long = nowMillis
}

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRecordingTest {

    @Test
    fun accumulatesThePathAndDistanceOfTheSession() = runTest {
        val locations = MutableSharedFlow<UserLocation>(replay = 3)
        val recording = buildRecording(locations)

        recording.start()
        runCurrent()
        locations.emit(UserLocation(latitude = 19.4300, longitude = -99.1300))
        locations.emit(UserLocation(latitude = 19.4310, longitude = -99.1300))
        runCurrent()

        val state = recording.state.value
        recording.stop() // the tick loop would otherwise outlive the test
        advanceUntilIdle()

        assertTrue(state.isRecording)
        assertEquals(2, state.traveledPoints.size)
        assertTrue(state.distanceMeters > 0.0)
    }

    @Test
    fun ignoresLocationsBeforeTheRecordingStarts() = runTest {
        val locations = MutableSharedFlow<UserLocation>(replay = 1)
        val recording = buildRecording(locations)

        locations.emit(UserLocation(latitude = 19.4300, longitude = -99.1300))
        runCurrent()

        assertEquals(0, recording.state.value.traveledPoints.size)
    }

    @Test
    fun reportsTheRealDurationEvenIfNothingTickedInBetween() = runTest {
        val locations = MutableSharedFlow<UserLocation>(replay = 1)
        val clock = FakeClock(nowMillis = 1_000)
        val repository = RecordingMovementSessionRepository()
        val recording = buildRecording(locations, clock, repository)

        recording.start()
        runCurrent()
        locations.emit(UserLocation(latitude = 19.4300, longitude = -99.1300))
        runCurrent()
        // Twelve minutes with the screen off: the clock moved, the ticks did not.
        clock.nowMillis += 720_000
        recording.stop()
        advanceUntilIdle()

        assertEquals(720, repository.savedSession?.elapsedTime)
        assertEquals(720, recording.lastSavedSession.value?.elapsedSeconds)
    }

    @Test
    fun persistsWhatWasRecordedWhenItStops() = runTest {
        val locations = MutableSharedFlow<UserLocation>(replay = 3)
        val repository = RecordingMovementSessionRepository()
        val clock = FakeClock(nowMillis = 5_000)
        val recording = buildRecording(locations, clock, repository)

        recording.start()
        runCurrent()
        locations.emit(UserLocation(latitude = 19.4300, longitude = -99.1300, altitude = 2200.0))
        locations.emit(UserLocation(latitude = 19.4310, longitude = -99.1300, altitude = 2210.0))
        runCurrent()
        clock.nowMillis += 300_000
        recording.stop()
        advanceUntilIdle()

        val saved = repository.savedSession
        assertNotNull(saved)
        assertEquals(300, saved.elapsedTime)
        assertEquals(2, repository.savedPoints.size)
        assertTrue(saved.distanceTraveled > 0.0)
        assertFalse(recording.state.value.isRecording)
    }

    @Test
    fun clearsTheFinishedSessionSoTheTrackerStopsDrawingIt() = runTest {
        val locations = MutableSharedFlow<UserLocation>(replay = 2)
        val clock = FakeClock(nowMillis = 5_000)
        val recording = buildRecording(locations, clock)

        recording.start()
        runCurrent()
        locations.emit(UserLocation(latitude = 19.4300, longitude = -99.1300))
        locations.emit(UserLocation(latitude = 19.4310, longitude = -99.1300))
        runCurrent()
        clock.nowMillis += 300_000
        recording.stop()
        advanceUntilIdle()

        // The route of a session that already ended must not stay on the map.
        val state = recording.state.value
        assertFalse(state.isRecording)
        assertEquals(0, state.traveledPoints.size)
        assertEquals(0.0, state.distanceMeters)
        assertEquals(0, state.elapsedSeconds)
    }

    @Test
    fun offersTheSavedSessionOnceAndForgetsItWhenAcknowledged() = runTest {
        val locations = MutableSharedFlow<UserLocation>(replay = 2)
        val clock = FakeClock(nowMillis = 5_000)
        val recording = buildRecording(locations, clock)

        recording.start()
        runCurrent()
        locations.emit(UserLocation(latitude = 19.4300, longitude = -99.1300))
        locations.emit(UserLocation(latitude = 19.4310, longitude = -99.1300))
        runCurrent()
        clock.nowMillis += 300_000
        recording.stop()
        advanceUntilIdle()

        val saved = recording.lastSavedSession.value
        assertNotNull(saved)
        assertEquals(300, saved.elapsedSeconds)
        assertEquals(2, saved.traveledPoints.size)

        recording.acknowledgeSavedSession()
        assertNull(recording.lastSavedSession.value)
    }

    @Test
    fun startingANewSessionDropsThePreviousConfirmation() = runTest {
        val locations = MutableSharedFlow<UserLocation>(replay = 1)
        val recording = buildRecording(locations)

        recording.start()
        runCurrent()
        locations.emit(UserLocation(latitude = 19.4300, longitude = -99.1300))
        runCurrent()
        recording.stop()
        advanceUntilIdle()
        assertNotNull(recording.lastSavedSession.value)

        recording.start()
        runCurrent()
        val pending = recording.lastSavedSession.value
        recording.stop()
        advanceUntilIdle()

        assertNull(pending)
    }

    @Test
    fun doesNotPersistASessionWithoutAnyLocation() = runTest {
        val repository = RecordingMovementSessionRepository()
        val recording = buildRecording(flowOf(), FakeClock(), repository)

        recording.start()
        runCurrent()
        recording.stop()
        advanceUntilIdle()

        assertNull(repository.savedSession)
        assertNull(recording.lastSavedSession.value)
    }

    @Test
    fun startingTwiceKeepsTheSessionThatIsAlreadyRunning() = runTest {
        val locations = MutableSharedFlow<UserLocation>(replay = 1)
        val recording = buildRecording(locations)

        recording.start()
        runCurrent()
        locations.emit(UserLocation(latitude = 19.4300, longitude = -99.1300))
        runCurrent()
        recording.start()
        runCurrent()

        val points = recording.state.value.traveledPoints.size
        recording.stop()
        advanceUntilIdle()

        assertEquals(1, points)
    }

    private fun TestScope.buildRecording(
        updates: Flow<UserLocation>,
        clock: FakeClock = FakeClock(),
        repository: MovementSessionRepository = RecordingMovementSessionRepository(),
    ): SessionRecording =
        SessionRecording(
            scope = this,
            locationRepository = FakeLocationRepository(updates),
            saveSession = SaveSessionUseCase(repository, clock),
            timeProvider = clock,
        )
}
