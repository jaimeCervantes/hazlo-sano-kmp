package com.hazlosano.feature.movement.tracker.ui.map

import android.graphics.Color
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.eq
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.FeatureCollection
import com.hazlosano.core.ui.theme.MapTheme

object MapLayers {
    const val SOURCE_ROUTE = "route-source"
    const val SOURCE_TRAVELED = "traveled-source"
    const val SOURCE_USER = "user-source"
    const val SOURCE_MARKERS = "markers-source"

    const val LAYER_ROUTE = "route-main"
    const val LAYER_ROUTE_ARROWS = "route-arrows"
    const val LAYER_TRAVELED = "traveled-path"
    const val LAYER_TRAVELED_ARROWS = "traveled-arrows"
    const val LAYER_START_MARKER = "start-marker"
    const val LAYER_END_MARKER = "end-marker"
    const val LAYER_USER_LOCATION = "user-location"
    const val LAYER_USER_HEADING = "user-heading"

    fun setup(style: Style, theme: MapTheme = MapTheme()) {
        val userHeadingBeamColor = String.format("#%06X", 0xFFFFFF and theme.userLocationColor)
        val routeColor = String.format("#%06X", 0xFFFFFF and theme.routeColor)
        val traveledColor = String.format("#%06X", 0xFFFFFF and theme.traveledPathColor)
        val startMarkerColor = String.format("#%06X", 0xFFFFFF and theme.startMarkerColor)
        val endMarkerColor = String.format("#%06X", 0xFFFFFF and theme.endMarkerColor)
        val userLocationColor = String.format("#%06X", 0xFFFFFF and theme.userLocationColor)

        if (style.getSource(SOURCE_ROUTE) == null) {
            style.addSource(GeoJsonSource(SOURCE_ROUTE, FeatureCollection.fromFeatures(emptyList())))
        }
        if (style.getSource(SOURCE_TRAVELED) == null) {
            style.addSource(GeoJsonSource(SOURCE_TRAVELED, FeatureCollection.fromFeatures(emptyList())))
        }
        if (style.getSource(SOURCE_USER) == null) {
            style.addSource(GeoJsonSource(SOURCE_USER, FeatureCollection.fromFeatures(emptyList())))
        }
        if (style.getSource(SOURCE_MARKERS) == null) {
            style.addSource(GeoJsonSource(SOURCE_MARKERS, FeatureCollection.fromFeatures(emptyList())))
        }

        if (style.getLayer(LAYER_ROUTE) == null) {
            style.addLayer(LineLayer(LAYER_ROUTE, SOURCE_ROUTE).apply {
                setProperties(
                    PropertyFactory.lineColor(Color.parseColor(routeColor)),
                    PropertyFactory.lineWidth(5f),
                    PropertyFactory.lineOpacity(0.6f)
                )
            })
        }

        if (style.getLayer(LAYER_ROUTE_ARROWS) == null) {
            style.addLayer(SymbolLayer(LAYER_ROUTE_ARROWS, SOURCE_ROUTE).apply {
                setProperties(
                    PropertyFactory.symbolPlacement(Property.SYMBOL_PLACEMENT_LINE),
                    PropertyFactory.symbolSpacing(50f),
                    PropertyFactory.iconImage(MapBitmaps.ICON_ARROW),
                    PropertyFactory.iconSize(0.6f),
                    PropertyFactory.iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_MAP),
                    PropertyFactory.iconKeepUpright(false),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true)
                )
            })
        }

        if (style.getLayer(LAYER_TRAVELED) == null) {
            style.addLayer(LineLayer(LAYER_TRAVELED, SOURCE_TRAVELED).apply {
                setProperties(
                    PropertyFactory.lineColor(Color.parseColor(traveledColor)),
                    PropertyFactory.lineWidth(6f),
                    PropertyFactory.lineOpacity(0.9f)
                )
            })
        }

        if (style.getLayer(LAYER_TRAVELED_ARROWS) == null) {
            style.addLayer(SymbolLayer(LAYER_TRAVELED_ARROWS, SOURCE_TRAVELED).apply {
                setProperties(
                    PropertyFactory.symbolPlacement(Property.SYMBOL_PLACEMENT_LINE),
                    PropertyFactory.symbolSpacing(40f),
                    PropertyFactory.iconImage(MapBitmaps.ICON_ARROW),
                    PropertyFactory.iconSize(0.6f),
                    PropertyFactory.iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_MAP),
                    PropertyFactory.iconKeepUpright(false),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true)
                )
            })
        }

        if (style.getLayer(LAYER_START_MARKER) == null) {
            style.addLayer(CircleLayer(LAYER_START_MARKER, SOURCE_MARKERS).apply {
                setProperties(
                    PropertyFactory.circleRadius(6f),
                    PropertyFactory.circleColor(Color.parseColor(startMarkerColor)),
                    PropertyFactory.circleStrokeColor(Color.WHITE),
                    PropertyFactory.circleStrokeWidth(2f)
                )
                setFilter(eq(get("type"), "start"))
            })
        }

        if (style.getLayer(LAYER_END_MARKER) == null) {
            style.addLayer(CircleLayer(LAYER_END_MARKER, SOURCE_MARKERS).apply {
                setProperties(
                    PropertyFactory.circleRadius(6f),
                    PropertyFactory.circleColor(Color.parseColor(endMarkerColor)),
                    PropertyFactory.circleStrokeColor(Color.WHITE),
                    PropertyFactory.circleStrokeWidth(2f)
                )
                setFilter(eq(get("type"), "end"))
            })
        }

        if (style.getLayer(LAYER_USER_HEADING) == null) {
            style.addLayer(SymbolLayer(LAYER_USER_HEADING, SOURCE_USER).apply {
                setProperties(
                    PropertyFactory.iconImage(MapBitmaps.ICON_HEADING),
                    PropertyFactory.iconSize(0.5f),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                    PropertyFactory.iconRotate(get("bearing"))
                )
            })
        }

        if (style.getLayer(LAYER_USER_LOCATION) == null) {
            style.addLayer(CircleLayer(LAYER_USER_LOCATION, SOURCE_USER).apply {
                setProperties(
                    PropertyFactory.circleRadius(8f),
                    PropertyFactory.circleColor(Color.parseColor(userLocationColor)),
                    PropertyFactory.circleStrokeColor(Color.WHITE),
                    PropertyFactory.circleStrokeWidth(3f)
                )
            })
        }
    }
}
