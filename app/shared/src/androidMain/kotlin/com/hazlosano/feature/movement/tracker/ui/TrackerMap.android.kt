package com.hazlosano.feature.movement.tracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hazlosano.core.ui.theme.MapTheme
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.feature.movement.tracker.ui.map.MapBitmaps
import com.hazlosano.feature.movement.tracker.ui.map.MapLayers
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val DEFAULT_STYLE_URL = "https://demotiles.maplibre.org/style.json"
private val DEFAULT_TARGET = LatLng(19.4326, -99.1332) // Ciudad de México
private const val DEFAULT_ZOOM = 12.0
private const val USER_ZOOM = 16.0

@Composable
actual fun TrackerMap(userLocation: UserLocation?, modifier: Modifier) {
    val context = LocalContext.current
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context)
    }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapStyle by remember { mutableStateOf<Style?>(null) }
    var centeredOnUser by remember { mutableStateOf(false) }

    BindMapViewToLifecycle(mapView)

    AndroidView(factory = { mapView }, modifier = modifier) { view ->
        if (mapLibreMap == null) {
            view.getMapAsync { map ->
                mapLibreMap = map
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
                    mapStyle = style
                }
            }
        }
    }

    LaunchedEffect(userLocation, mapStyle) {
        val location = userLocation ?: return@LaunchedEffect
        val style = mapStyle ?: return@LaunchedEffect
        val map = mapLibreMap ?: return@LaunchedEffect

        val feature = Feature.fromGeometry(Point.fromLngLat(location.longitude, location.latitude))
            .apply { addNumberProperty("bearing", location.bearing) }
        (style.getSource(MapLayers.SOURCE_USER) as? GeoJsonSource)
            ?.setGeoJson(FeatureCollection.fromFeature(feature))

        val target = LatLng(location.latitude, location.longitude)
        if (!centeredOnUser) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, USER_ZOOM))
            centeredOnUser = true
        } else {
            map.animateCamera(CameraUpdateFactory.newLatLng(target))
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
