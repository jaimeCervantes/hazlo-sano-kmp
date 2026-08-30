package com.hazlosano.domain.feature.movement.parser

import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.WayPoint
import kotlinx.datetime.Instant

/**
 * Reads and writes the slice of GPX 1.1 this app deals in: a named track, and the points along it.
 *
 * Deliberately not a general XML parser. GPX carries far more than a track — waypoints, routes,
 * extensions, arbitrary vendor namespaces — and none of it means anything to a route you follow.
 * What is understood here is the track: its name, and each point's position, elevation and time.
 * Everything else in the file is ignored rather than rejected, because a file exported by a watch
 * or by Strava is full of extensions this app has no opinion about and must still open.
 *
 * Lives in `core` and uses no XML library, so the same reader serves Android, iOS, desktop and web.
 * The previous implementation used `javax.xml` and sat in `core/src/jvmMain` under a package that
 * did not match its own path, which left it unreachable from the app on every target including JVM.
 */
object GpxFormat : GpxParser {

    const val DEFAULT_ROUTE_NAME: String = "Ruta sin nombre"

    override fun parse(data: ByteArray): Route = parse(data.decodeToString())

    /**
     * @throws GpxParseException when the file carries no usable track point. An empty route cannot
     * be followed, drawn or measured, so it is a failure to report rather than an empty list to
     * hand back and discover later.
     */
    fun parse(gpx: String): Route {
        val points = pointPattern.findAll(gpx).mapNotNull(::readPoint).toList()
        if (points.isEmpty()) {
            throw GpxParseException(
                "El archivo no contiene ningún punto de ruta (<trkpt> o <rtept>).",
            )
        }
        // Las cifras las calcula quien importa, con `calculateStats`. Aquí no se han medido, y un
        // desnivel sin medir es nulo y no cero.
        return Route(name = readName(gpx), distance = 0.0, elevationGain = null, points = points)
    }

    /**
     * The track as GPX 1.1. Points that carry no elevation or no time are written without those
     * elements rather than with a zero, so a route exported and read back says exactly what it said
     * before: a point with no altitude is not a point at sea level.
     */
    fun write(route: Route): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
        append(
            """<gpx version="1.1" creator="Hazlo Sano" xmlns="http://www.topografix.com/GPX/1/1">""",
        ).append('\n')
        append("  <metadata><name>").append(escape(route.name)).append("</name></metadata>\n")
        append("  <trk>\n    <name>").append(escape(route.name)).append("</name>\n")
        append("    <trkseg>\n")
        route.points.forEach { point ->
            append("      <trkpt lat=\"").append(point.latitude)
            append("\" lon=\"").append(point.longitude).append("\">")
            point.altitude?.let { append("<ele>").append(it).append("</ele>") }
            point.timestamp
                ?.let { Instant.fromEpochMilliseconds(it) }
                ?.let { append("<time>").append(it).append("</time>") }
            append("</trkpt>\n")
        }
        append("    </trkseg>\n  </trk>\n</gpx>\n")
    }

    private fun readPoint(match: MatchResult): WayPoint? {
        val attributes = match.groupValues[1]
        val latitude = attribute(attributes, "lat")?.toDoubleOrNull() ?: return null
        val longitude = attribute(attributes, "lon")?.toDoubleOrNull() ?: return null
        // A self-closing <trkpt/> has no body, so there is nothing to look inside for elevation.
        val body = match.groupValues[2]
        return WayPoint(
            latitude = latitude,
            longitude = longitude,
            altitude = elementText(body, "ele")?.trim()?.toDoubleOrNull(),
            timestamp = elementText(body, "time")?.trim()?.let(::parseTime),
        )
    }

    /**
     * The track's own name first: it is what the person who exported the file called this route.
     * The metadata name describes the document, which is often the file name or the exporting tool,
     * so it is only used when the track did not name itself.
     */
    private fun readName(gpx: String): String {
        val fromTrack = trackPattern.find(gpx)?.groupValues?.get(1)?.let { elementText(it, "name") }
        val name = fromTrack ?: metadataPattern.find(gpx)?.groupValues?.get(1)
            ?.let { elementText(it, "name") }
        return name?.let(::unescape)?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_ROUTE_NAME
    }

    /** An unreadable timestamp leaves the point without one rather than dating it to 1970. */
    private fun parseTime(raw: String): Long? =
        try {
            Instant.parse(raw).toEpochMilliseconds()
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun attribute(attributes: String, name: String): String? =
        Regex("""\b$name\s*=\s*["']([^"']*)["']""").find(attributes)?.groupValues?.get(1)

    private fun elementText(xml: String, name: String): String? =
        Regex("""<(?:[\w.-]+:)?$name\b[^>]*>([\s\S]*?)</(?:[\w.-]+:)?$name>""")
            .find(xml)?.groupValues?.get(1)

    private fun escape(raw: String): String = raw
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun unescape(raw: String): String = raw
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        // Ampersand last: doing it first would turn "&amp;lt;" into a "<" that was never there.
        .replace("&amp;", "&")

    /**
     * Matches a track or route point in one pass, self-closing or not, with or without a namespace
     * prefix. Group 1 is the attributes, group 2 the body — empty when the element closes itself.
     */
    private val pointPattern =
        Regex("""<(?:[\w.-]+:)?(?:trkpt|rtept)\b([^>]*?)(?:/>|>([\s\S]*?)</(?:[\w.-]+:)?(?:trkpt|rtept)>)""")

    private val trackPattern = Regex("""<(?:[\w.-]+:)?trk\b[^>]*>([\s\S]*?)</(?:[\w.-]+:)?trk>""")

    private val metadataPattern =
        Regex("""<(?:[\w.-]+:)?metadata\b[^>]*>([\s\S]*?)</(?:[\w.-]+:)?metadata>""")
}

class GpxParseException(message: String) : Exception(message)
