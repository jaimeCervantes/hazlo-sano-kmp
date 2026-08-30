package com.hazlosano.data.movement

import com.hazlosano.data.movement.trace.FakeTraceStore
import com.hazlosano.data.movement.trace.TraceStore
import com.hazlosano.domain.feature.movement.filter.DiscardReason
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
import kotlin.random.Random
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

    override suspend fun updateDistance(sessionId: Long, distanceMeters: Double) = Unit
}

/** A clock the test moves by hand, so elapsed time does not depend on how long the test ran. */
private class FakeClock(var nowMillis: Long = 0) : TimeProvider {
    override fun nowMillis(): Long = nowMillis
}

private const val BASE_LATITUDE = 19.4300
private const val BASE_LONGITUDE = -99.1300
private const val METERS_PER_DEGREE_LATITUDE = 111_320.0
private const val METERS_PER_DEGREE_LONGITUDE = 104_990.0 // at this latitude

/** A reading a given number of metres from a fixed spot, with the accuracy of a typical fix. */
private fun locationAt(
    northMeters: Double,
    eastMeters: Double = 0.0,
    atMillis: Long,
    accuracyMeters: Float = 10f,
): UserLocation = UserLocation(
    latitude = BASE_LATITUDE + northMeters / METERS_PER_DEGREE_LATITUDE,
    longitude = BASE_LONGITUDE + eastMeters / METERS_PER_DEGREE_LONGITUDE,
    accuracy = accuracyMeters,
    timestamp = atMillis,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRecordingTest {

    @Test
    fun accumulatesThePathAndDistanceOfTheSession() = runTest {
        val locations = MutableSharedFlow<UserLocation>(replay = 5)
        val recording = buildRecording(locations)

        recording.start()
        runCurrent()
        // Walking away for longer than a departure takes to be confirmed: before that the readings
        // are held rather than recorded, because a receiver going in circles looks the same.
        (0..4).forEach { index ->
            locations.emit(locationAt(northMeters = index * 30.0, atMillis = index * 20_000L))
        }
        runCurrent()

        val state = recording.state.value
        recording.stop() // the tick loop would otherwise outlive the test
        advanceUntilIdle()

        assertTrue(state.isRecording)
        assertTrue(state.traveledPoints.size >= 2, "recorded ${state.traveledPoints.size} points")
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

    @Test
    fun doesNotInflateTheDistanceOfASessionSpentStandingStill() = runTest {
        val locations = MutableSharedFlow<UserLocation>(extraBufferCapacity = 64)
        val repository = RecordingMovementSessionRepository()
        val clock = FakeClock(nowMillis = 1_000)
        val recording = buildRecording(locations, clock, repository)

        recording.start()
        runCurrent()
        // Two minutes of GPS noise around a single spot: waiting, not moving.
        val noise = Random(seed = 7)
        repeat(60) { reading ->
            locations.emit(
                locationAt(
                    northMeters = noise.nextDouble(-10.0, 10.0),
                    eastMeters = noise.nextDouble(-10.0, 10.0),
                    atMillis = reading * 2_000L,
                ),
            )
        }
        runCurrent()
        clock.nowMillis += 120_000
        recording.stop()
        advanceUntilIdle()

        val saved = repository.savedSession
        assertNotNull(saved)
        assertTrue(saved.distanceTraveled < 15.0, "standing still saved ${saved.distanceTraveled} m")
    }

    @Test
    fun startsANewSessionWithoutDraggingThePreviousOneIntoIt() = runTest {
        val locations = MutableSharedFlow<UserLocation>(extraBufferCapacity = 8)
        val repository = RecordingMovementSessionRepository()
        val clock = FakeClock(nowMillis = 1_000)
        val recording = buildRecording(locations, clock, repository)

        recording.start()
        runCurrent()
        locations.emit(locationAt(northMeters = 0.0, atMillis = 0L))
        locations.emit(locationAt(northMeters = 60.0, atMillis = 2_000L))
        runCurrent()
        recording.stop()
        advanceUntilIdle()

        // A different outing, ten minutes later and five kilometres away.
        recording.start()
        runCurrent()
        locations.emit(locationAt(northMeters = 5_000.0, atMillis = 600_000L))
        locations.emit(locationAt(northMeters = 5_060.0, atMillis = 602_000L))
        runCurrent()
        clock.nowMillis += 60_000
        recording.stop()
        advanceUntilIdle()

        val saved = repository.savedSession
        assertNotNull(saved)
        assertTrue(
            saved.distanceTraveled < 200.0,
            "the new session inherited ${saved.distanceTraveled} m from the previous one",
        )
    }

    @Test
    fun recordingNormallyLeavesNoTraceBehind() = runTest {
        val locations = MutableSharedFlow<UserLocation>(extraBufferCapacity = 8)
        val traceStore = FakeTraceStore()
        val recording = buildRecording(locations, traceStore = traceStore)

        recording.start()
        runCurrent()
        locations.emit(locationAt(northMeters = 0.0, atMillis = 0L))
        locations.emit(locationAt(northMeters = 60.0, atMillis = 2_000L))
        runCurrent()
        recording.stop()
        advanceUntilIdle()

        assertEquals(0, traceStore.opensRequested, "a normal recording opened a trace")
        assertTrue(traceStore.finishedTraces.isEmpty())
    }

    @Test
    fun capturesEveryReadingWithTheVerdictTheFilterGaveIt() = runTest {
        val locations = MutableSharedFlow<UserLocation>(extraBufferCapacity = 64)
        val traceStore = FakeTraceStore()
        val recording = buildRecording(locations, traceStore = traceStore)

        recording.start(captureTrace = true)
        runCurrent()
        // Someone standing still: most of these are noise the filter turns away, which is exactly
        // what a saved session cannot tell you about afterwards.
        val noise = Random(seed = 11)
        val emitted = (0 until 40).map { reading ->
            locationAt(
                northMeters = noise.nextDouble(-8.0, 8.0),
                eastMeters = noise.nextDouble(-8.0, 8.0),
                atMillis = reading * 2_000L,
            )
        }
        emitted.forEach { locations.emit(it) }
        runCurrent()
        recording.stop()
        advanceUntilIdle()

        val captured = traceStore.lastSink?.appended.orEmpty()
        assertEquals(emitted, captured.map { it.reading }, "the trace altered or lost readings")
        assertTrue(captured.any { it.wasAccepted }, "nothing was accepted, so nothing was recorded")
        assertTrue(
            captured.any { !it.wasAccepted },
            "standing still rejected nothing, so this cannot show the rejected half is kept",
        )
    }

    @Test
    fun theTraceIsTiedToTheFirstPointOfTheSessionItProduced() = runTest {
        val locations = MutableSharedFlow<UserLocation>(extraBufferCapacity = 8)
        val repository = RecordingMovementSessionRepository()
        val traceStore = FakeTraceStore()
        val recording = buildRecording(locations, FakeClock(), repository, traceStore)

        recording.start(captureTrace = true)
        runCurrent()
        locations.emit(locationAt(northMeters = 0.0, atMillis = 4_000L))
        locations.emit(locationAt(northMeters = 60.0, atMillis = 6_000L))
        runCurrent()
        recording.stop()
        advanceUntilIdle()

        val firstStoredPoint = repository.savedPoints.first().timestamp
        assertEquals(firstStoredPoint, traceStore.lastSink?.tiedTo)
        assertNotNull(traceStore.readTrace(firstStoredPoint), "the session cannot find its trace")
    }

    @Test
    fun keepsTheReadingsThrownAwayBeforeTheSessionEvenStarts() = runTest {
        val locations = MutableSharedFlow<UserLocation>(extraBufferCapacity = 8)
        val repository = RecordingMovementSessionRepository()
        val traceStore = FakeTraceStore()
        val recording = buildRecording(locations, FakeClock(), repository, traceStore)

        recording.start(captureTrace = true)
        runCurrent()
        // A cold receiver: the first fixes say little more than "somewhere in this neighbourhood",
        // and how long that lasts is one of the things worth measuring.
        locations.emit(locationAt(northMeters = 0.0, atMillis = 0L, accuracyMeters = 120f))
        locations.emit(locationAt(northMeters = 0.0, atMillis = 2_000L, accuracyMeters = 90f))
        locations.emit(locationAt(northMeters = 0.0, atMillis = 4_000L))
        locations.emit(locationAt(northMeters = 60.0, atMillis = 6_000L))
        runCurrent()
        recording.stop()
        advanceUntilIdle()

        val captured = traceStore.lastSink?.appended.orEmpty()
        assertEquals(4, captured.size)
        assertEquals(
            listOf(DiscardReason.POOR_ACCURACY, DiscardReason.POOR_ACCURACY),
            captured.take(2).map { it.discardReason },
        )
        // Those readings came before the first stored point, and the trace is still that session's.
        assertEquals(repository.savedPoints.first().timestamp, traceStore.lastSink?.tiedTo)
    }

    @Test
    fun aRecordingThatSavedNothingStillClosesItsTrace() = runTest {
        val traceStore = FakeTraceStore()
        val repository = RecordingMovementSessionRepository()
        val recording = buildRecording(flowOf(), FakeClock(), repository, traceStore)

        recording.start(captureTrace = true)
        runCurrent()
        recording.stop()
        advanceUntilIdle()

        assertNull(repository.savedSession)
        assertTrue(traceStore.lastSink?.finished == true, "the trace file was left open")
        assertNull(traceStore.lastSink?.tiedTo, "a trace was tied to a session that never existed")
    }

    private fun TestScope.buildRecording(
        updates: Flow<UserLocation>,
        clock: FakeClock = FakeClock(),
        repository: MovementSessionRepository = RecordingMovementSessionRepository(),
        traceStore: TraceStore = FakeTraceStore(),
    ): SessionRecording =
        SessionRecording(
            scope = this,
            locationRepository = FakeLocationRepository(updates),
            saveSession = SaveSessionUseCase(repository, clock),
            timeProvider = clock,
            traceStore = traceStore,
        )
}
