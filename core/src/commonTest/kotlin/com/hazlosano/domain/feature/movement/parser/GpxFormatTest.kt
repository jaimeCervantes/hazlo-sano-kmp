package com.hazlosano.domain.feature.movement.parser

import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.WayPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GpxFormatTest {

    @Test
    fun `a track is read with its name and the position elevation and time of each point`() {
        val route = GpxFormat.parse(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Garmin">
              <trk>
                <name>Subida al cerro</name>
                <trkseg>
                  <trkpt lat="19.4300" lon="-99.1300">
                    <ele>2200.5</ele><time>2026-08-09T06:00:00Z</time>
                  </trkpt>
                  <trkpt lat="19.4310" lon="-99.1310">
                    <ele>2215.0</ele><time>2026-08-09T06:01:00Z</time>
                  </trkpt>
                </trkseg>
              </trk>
            </gpx>
            """.trimIndent(),
        )

        assertEquals("Subida al cerro", route.name)
        assertEquals(2, route.points.size)
        assertEquals(19.4300, route.points.first().latitude, 1e-9)
        assertEquals(-99.1300, route.points.first().longitude, 1e-9)
        assertEquals(2200.5, route.points.first().altitude)
        assertEquals(1_786_255_200_000L, route.points.first().timestamp) // 2026-08-09T06:00:00Z
        assertEquals(1_786_255_260_000L, route.points.last().timestamp)
        assertEquals(2215.0, route.points.last().altitude)
    }

    @Test
    fun `a point without elevation carries none rather than sitting at sea level`() {
        // The distinction the movement pillar already makes for its own readings: an exporter that
        // omits <ele> is saying nothing about height, not that the track ran along the coast.
        val route = GpxFormat.parse(
            """
            <gpx><trk><name>Sin altitud</name><trkseg>
              <trkpt lat="19.43" lon="-99.13"></trkpt>
              <trkpt lat="19.44" lon="-99.13"><ele>0.0</ele></trkpt>
            </trkseg></trk></gpx>
            """.trimIndent(),
        )

        assertNull(route.points.first().altitude)
        // Sea level is an elevation and has to survive as one.
        assertEquals(0.0, route.points.last().altitude)
    }

    @Test
    fun `a self-closing point is still a point`() {
        val route = GpxFormat.parse(
            """<gpx><trk><trkseg><trkpt lat="19.43" lon="-99.13"/></trkseg></trk></gpx>""",
        )

        assertEquals(1, route.points.size)
        assertEquals(19.43, route.points.single().latitude, 1e-9)
        assertNull(route.points.single().altitude)
    }

    @Test
    fun `namespaced tags are read like any other`() {
        // Files exported with a default namespace prefix are common and open everywhere else.
        val route = GpxFormat.parse(
            """
            <gpx:gpx xmlns:gpx="http://www.topografix.com/GPX/1/1">
              <gpx:trk><gpx:name>Con prefijo</gpx:name><gpx:trkseg>
                <gpx:trkpt lat="19.43" lon="-99.13"><gpx:ele>2200</gpx:ele></gpx:trkpt>
              </gpx:trkseg></gpx:trk>
            </gpx:gpx>
            """.trimIndent(),
        )

        assertEquals("Con prefijo", route.name)
        assertEquals(2200.0, route.points.single().altitude)
    }

    @Test
    fun `a route file that uses rtept instead of trkpt is read too`() {
        val route = GpxFormat.parse(
            """
            <gpx><rte><name>Ruta planeada</name>
              <rtept lat="19.43" lon="-99.13"/>
              <rtept lat="19.44" lon="-99.14"/>
            </rte></gpx>
            """.trimIndent(),
        )

        assertEquals(2, route.points.size)
    }

    @Test
    fun `the track name wins over the document name`() {
        val route = GpxFormat.parse(
            """
            <gpx>
              <metadata><name>export-2026-08-09.gpx</name></metadata>
              <trk><name>Vuelta al lago</name><trkseg>
                <trkpt lat="19.43" lon="-99.13"/>
              </trkseg></trk>
            </gpx>
            """.trimIndent(),
        )

        assertEquals("Vuelta al lago", route.name)
    }

    @Test
    fun `a track that names itself nothing falls back to the document name`() {
        val route = GpxFormat.parse(
            """
            <gpx>
              <metadata><name>Del reloj</name></metadata>
              <trk><trkseg><trkpt lat="19.43" lon="-99.13"/></trkseg></trk>
            </gpx>
            """.trimIndent(),
        )

        assertEquals("Del reloj", route.name)
    }

    @Test
    fun `an unnamed track gets a name rather than an empty one`() {
        val route = GpxFormat.parse("""<gpx><trk><trkseg><trkpt lat="1" lon="2"/></trkseg></trk></gpx>""")

        assertEquals(GpxFormat.DEFAULT_ROUTE_NAME, route.name)
    }

    @Test
    fun `escaped characters in a name come back as themselves`() {
        val route = GpxFormat.parse(
            """<gpx><trk><name>Cerro &amp; r&#237;o &lt;norte&gt;</name><trkseg>""" +
                """<trkpt lat="1" lon="2"/></trkseg></trk></gpx>""",
        )

        assertEquals("Cerro & r&#237;o <norte>", route.name)
    }

    @Test
    fun `a file with no points is refused rather than handed back empty`() {
        // An empty route cannot be followed, drawn or measured. Failing here names the problem;
        // returning an empty list would surface it somewhere far from the file that caused it.
        val failure = assertFailsWith<GpxParseException> {
            GpxFormat.parse("""<gpx><trk><name>Vacía</name><trkseg></trkseg></trk></gpx>""")
        }

        assertTrue(failure.message.orEmpty().isNotBlank())
    }

    @Test
    fun `a point with an unreadable position is skipped rather than placed at zero`() {
        val route = GpxFormat.parse(
            """
            <gpx><trk><trkseg>
              <trkpt lat="no-es-un-numero" lon="-99.13"/>
              <trkpt lat="19.44" lon="-99.14"/>
            </trkseg></trk></gpx>
            """.trimIndent(),
        )

        assertEquals(1, route.points.size)
        assertEquals(19.44, route.points.single().latitude, 1e-9)
    }

    @Test
    fun `an unreadable time leaves the point undated rather than dating it to 1970`() {
        val route = GpxFormat.parse(
            """<gpx><trk><trkseg><trkpt lat="1" lon="2"><time>ayer</time></trkpt></trkseg></trk></gpx>""",
        )

        assertNull(route.points.single().timestamp)
    }

    @Test
    fun `a route written out and read back says exactly what it said`() {
        val original = Route(
            name = "Cerro & río",
            distance = 0.0,
            elevationGain = 0.0,
            points = listOf(
                WayPoint(19.43, -99.13, altitude = 2200.5, timestamp = 1_786_312_800_000L),
                WayPoint(19.44, -99.14, altitude = null, timestamp = null),
                WayPoint(19.45, -99.15, altitude = 0.0, timestamp = 1_786_312_860_000L),
            ),
        )

        val reread = GpxFormat.parse(GpxFormat.write(original))

        assertEquals(original.name, reread.name)
        assertEquals(original.points, reread.points)
    }

    @Test
    fun `what is written is a file other tools accept`() {
        val gpx = GpxFormat.write(
            Route(
                name = "Vuelta",
                distance = 0.0,
                elevationGain = 0.0,
                points = listOf(WayPoint(19.43, -99.13, altitude = 2200.0)),
            ),
        )

        assertTrue(gpx.startsWith("""<?xml version="1.0" encoding="UTF-8"?>"""))
        assertTrue(gpx.contains("""<gpx version="1.1""""))
        assertTrue(gpx.contains("http://www.topografix.com/GPX/1/1"))
        assertTrue(gpx.contains("<trkpt lat=\"19.43\" lon=\"-99.13\">"))
        assertTrue(gpx.trimEnd().endsWith("</gpx>"))
    }

    @Test
    fun `bytes are read as UTF-8 so accented names survive the file`() {
        val gpx = """<gpx><trk><name>Montaña</name><trkseg><trkpt lat="1" lon="2"/></trkseg></trk></gpx>"""

        val route = GpxFormat.parse(gpx.encodeToByteArray())

        assertEquals("Montaña", route.name)
    }
}
