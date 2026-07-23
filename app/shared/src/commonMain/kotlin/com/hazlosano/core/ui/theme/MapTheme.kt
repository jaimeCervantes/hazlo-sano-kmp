package com.hazlosano.core.ui.theme

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color

data class MapTheme(
    val routeColor: Int = Color(0xFF4285F4).toArgb(),
    val traveledPathColor: Int = Color(0xFFF0953E).toArgb(), // Orange
    val userLocationColor: Int = Color(0xFF4285F4).toArgb(),
    val startMarkerColor: Int = Color(0xFFC0CEBA).toArgb(), // GreenGrey80
    val endMarkerColor: Int = Color(0xFF88AB75).toArgb(), // Green
    val userHeadingBeamColor: Int = Color(0x554285F4).toArgb()
) {
    enum class MapType { OUTDOORS, SATELLITE }
}
