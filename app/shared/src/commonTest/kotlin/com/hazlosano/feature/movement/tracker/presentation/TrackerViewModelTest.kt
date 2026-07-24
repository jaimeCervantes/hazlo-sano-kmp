package com.hazlosano.feature.movement.tracker.presentation

import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.LocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import kotlin.test.assertNull

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

    @Test
    fun startsWithoutLocation() {
        val viewModel = TrackerViewModel(FakeLocationRepository(flowOf()))

        assertNull(viewModel.userLocation.value)
    }

    @Test
    fun exposesLocationAfterStartTracking() = runTest {
        val location = UserLocation(latitude = 19.4326, longitude = -99.1332)
        val viewModel = TrackerViewModel(FakeLocationRepository(flowOf(location)))

        viewModel.startTracking()
        advanceUntilIdle()

        assertEquals(location, viewModel.userLocation.value)
    }
}
