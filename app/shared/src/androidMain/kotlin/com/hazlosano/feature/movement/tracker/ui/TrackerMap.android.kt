package com.hazlosano.feature.movement.tracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hazlosano.core.ui.theme.MapTheme
import com.hazlosano.feature.movement.tracker.ui.map.MapBitmaps
import com.hazlosano.feature.movement.tracker.ui.map.MapLayers
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private const val DEFAULT_STYLE_URL = "https://demotiles.maplibre.org/style.json"
private val DEFAULT_TARGET = LatLng(19.4326, -99.1332) // Ciudad de México
private const val DEFAULT_ZOOM = 12.0

@Composable
actual fun TrackerMap(modifier: Modifier) {
    val context = LocalContext.current
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context)
    }

    BindMapViewToLifecycle(mapView)

    AndroidView(factory = { mapView }, modifier = modifier) { view ->
        view.getMapAsync { map ->
            map.cameraPosition = CameraPosition.Builder()
                .target(DEFAULT_TARGET)
                .zoom(DEFAULT_ZOOM)
                .build()
            map.setStyle(Style.Builder().fromUri(DEFAULT_STYLE_URL)) { style ->
                val theme = MapTheme()
                style.addImage(MapBitmaps.ICON_ARROW, MapBitmaps.createArrowBitmap())
                style.addImage(
                    MapBitmaps.ICON_HEADING,
                    MapBitmaps.createHeadingBeamBitmap(theme.userHeadingBeamColor),
                )
                MapLayers.setup(style, theme)
            }
        }
    }
}

@Composable
private fun BindMapViewToLifecycle(mapView: MapView) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        mapView.onCreate(null)
        // Catch up to the current lifecycle state, since observers do not replay past events.
        val currentState = lifecycleOwner.lifecycle.currentState
        if (currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onStop()
            mapView.onDestroy()
        }
    }
}
