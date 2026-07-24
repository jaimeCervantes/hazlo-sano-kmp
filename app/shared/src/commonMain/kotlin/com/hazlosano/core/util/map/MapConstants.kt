package com.hazlosano.core.util.map

/**
 * Map style URLs. Hosted remotely because MapLibre's offline (C++) engine requires HTTP URLs and
 * cannot resolve `asset://`.
 */
object MapConstants {
    const val OSM_STYLE_URL =
        "https://raw.githubusercontent.com/jaimeCervantes/HazloSano-MapStyles/refs/heads/main/osm_style.json"

    const val SATELLITE_STYLE_URL =
        "https://raw.githubusercontent.com/jaimeCervantes/HazloSano-MapStyles/refs/heads/main/sat_style.json"
}
