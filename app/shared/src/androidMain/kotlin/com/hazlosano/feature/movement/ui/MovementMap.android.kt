package com.hazlosano.feature.movement.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.core.ui.theme.MapTheme
import com.hazlosano.core.util.map.MapConstants
import com.hazlosano.domain.feature.movement.model.GeoBounds
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.model.boundingBox
import com.hazlosano.feature.movement.tracker.ui.map.MapBitmaps
import com.hazlosano.feature.movement.tracker.ui.map.MapLayers
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlin.math.roundToInt

private const val TAG = "MovementMap"
private const val USER_ZOOM = 16.0
private const val PATH_ZOOM = 15.0
private const val PATH_PADDING_PX = 120

/**
 * Rendering and camera work happen in [AndroidView]'s update block — which runs on every
 * recomposition, after the view is attached and measured — instead of effects keyed on the style.
 * MapLibre needs a live surface and a measured view for both, and driving it from effects left the
 * map blank until an unrelated lifecycle event repainted it.
 */
@Composable
actual fun MovementMap(
    userLocation: UserLocation?,
    path: List<UserLocation>,
    modifier: Modifier,
    fitPathInView: Boolean,
) {
    var styleStatus by remember { mutableStateOf("cargando estilo…") }
    var framedPathKey by remember { mutableStateOf<String?>(null) }
    var centeredOnUser by remember { mutableStateOf(false) }
    // The style callback fires long after this composition; read the values it needs through
    // rememberUpdatedState so it never draws or frames a stale path.
    val currentPath by rememberUpdatedState(path)
    val currentFitPathInView by rememberUpdatedState(fitPathInView)
    val currentUserLocation by rememberUpdatedState(userLocation)

    val context: Context = LocalContext.current
    val mapView = remember(context) {
        MapLibre.getInstance(context)
        // Texture mode renders into the view hierarchy instead of a SurfaceView with its own
        // window. A SurfaceView inside AndroidView only gets its surface when the window is laid
        // out again, which left the map — and even the style download — waiting until an
        // unrelated event (locking and unlocking the screen) forced it.
        val options = MapLibreMapOptions.createFromAttributes(context).textureMode(true)
        MapView(context, options)
    }
    BindMapViewToLifecycle(mapView)

    Box(modifier = modifier) {
        AndroidView(
            factory = { _: Context ->
                val view = mapView
                view.addOnDidFailLoadingMapListener { error ->
                    styleStatus = "fallo del mapa: $error"
                    Log.e(TAG, "map failed to load: $error")
                }
                view.getMapAsync { map ->
                    map.setStyle(Style.Builder().fromUri(MapConstants.OSM_STYLE_URL)) { style ->
                        val theme = MapTheme()
                        style.addImage(MapBitmaps.ICON_ARROW, MapBitmaps.createArrowBitmap())
                        style.addImage(
                            MapBitmaps.ICON_HEADING,
                            MapBitmaps.createHeadingBeamBitmap(theme.userHeadingBeamColor),
                        )
                        MapLayers.setup(style, theme)

                        // Draw and frame here too: this callback can arrive after the last update
                        // pass, and without it the camera would stay at its default world view.
                        style.drawUser(currentUserLocation)
                        style.drawPath(currentPath)
                        val bounds = currentPath.boundingBox()
                        if (currentFitPathInView && bounds != null) {
                            framedPathKey = currentPath.frameKey()
                            map.frame(bounds, view.width, view.height)
                        }
                        styleStatus = "estilo OK · zoom ${map.cameraPosition.zoom.roundToInt()}"
                        Log.i(
                            TAG,
                            "style loaded, layers=${style.layers.size}, " +
                                "points=${currentPath.size}, view=${view.width}x${view.height}",
                        )
                    }
                }
                view
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.getMapAsync { map ->
                    val style = map.style ?: return@getMapAsync
                    style.drawUser(userLocation)
                    style.drawPath(path)

                    if (fitPathInView) {
                        val bounds = path.boundingBox() ?: return@getMapAsync
                        val key = path.frameKey()
                        if (framedPathKey != key) {
                            framedPathKey = key
                            map.frame(bounds, view.width, view.height)
                            Log.i(TAG, "framed ${path.size} points, view=${view.width}x${view.height}")
                        }
                    } else if (userLocation != null) {
                        val target = LatLng(userLocation.latitude, userLocation.longitude)
                        if (centeredOnUser) {
                            map.animateCamera(CameraUpdateFactory.newLatLng(target))
                        } else {
                            centeredOnUser = true
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, USER_ZOOM))
                        }
                    }
                }
            },
        )

        // Temporary diagnostic while the map is being validated on device.
        Text(
            text = "$styleStatus · puntos: ${path.size}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(HazloSpaces.sm)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** Identifies a path for framing purposes, so the camera is fitted once per distinct path. */
private fun List<UserLocation>.frameKey(): String = "$size:${firstOrNull()?.timestamp}"

/**
 * MapLibre's own lifecycle: [MapView.onStart] is what activates the connectivity receiver and the
 * file source that downloads style and tiles, and [MapView.onDestroy] releases the renderer. The
 * surface itself is created by the constructor, so these calls are about network and cleanup.
 */
@Composable
private fun BindMapViewToLifecycle(mapView: MapView) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        var started = false

        fun start() {
            if (started) return
            started = true
            mapView.onStart()
            mapView.onResume()
        }

        fun stop() {
            if (!started) return
            started = false
            mapView.onPause()
            mapView.onStop()
        }

        mapView.onCreate(null)
        // A composable entering while the activity is already resumed never receives
        // ON_START/ON_RESUME, so catch up to the current state instead of waiting for an event.
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) start()

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> start()
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stop()
            mapView.onDestroy()
        }
    }
}

private fun Style.drawUser(userLocation: UserLocation?) {
    val source = getSource(MapLayers.SOURCE_USER) as? GeoJsonSource ?: return
    if (userLocation == null) {
        source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        return
    }
    val feature = Feature
        .fromGeometry(Point.fromLngLat(userLocation.longitude, userLocation.latitude))
        .apply { addNumberProperty("bearing", userLocation.bearing) }
    source.setGeoJson(FeatureCollection.fromFeature(feature))
}

private fun Style.drawPath(path: List<UserLocation>) {
    val source = getSource(MapLayers.SOURCE_TRAVELED) as? GeoJsonSource ?: return
    if (path.size < 2) {
        source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        return
    }
    val line = LineString.fromLngLats(path.map { Point.fromLngLat(it.longitude, it.latitude) })
    source.setGeoJson(Feature.fromGeometry(line))
}

private fun MapLibreMap.frame(bounds: GeoBounds, viewWidth: Int, viewHeight: Int) {
    val center = LatLng(bounds.centerLatitude, bounds.centerLongitude)
    if (!bounds.spansAnArea || viewWidth == 0 || viewHeight == 0) {
        moveCamera(CameraUpdateFactory.newLatLngZoom(center, PATH_ZOOM))
        return
    }
    val latLngBounds = LatLngBounds.Builder()
        .include(LatLng(bounds.minLatitude, bounds.minLongitude))
        .include(LatLng(bounds.maxLatitude, bounds.maxLongitude))
        .build()
    moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, PATH_PADDING_PX))
}
