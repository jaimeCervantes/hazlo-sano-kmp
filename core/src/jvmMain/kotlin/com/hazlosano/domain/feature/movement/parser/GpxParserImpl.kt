package com.hazlosano.feature.movement.tracker.data.parser

import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.WayPoint
import com.hazlosano.domain.feature.movement.parser.GpxParser
import java.io.ByteArrayInputStream
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

class GpxParserImpl : GpxParser {

    override fun parse(data: ByteArray): Route {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(ByteArrayInputStream(data))
        doc.documentElement.normalize()

        return readGpx(doc.documentElement)
    }

    private fun readGpx(root: Element): Route {
        var name = "Ruta sin nombre"
        val points = mutableListOf<WayPoint>()

        val tracks = root.getElementsByTagName("trk")
        for (i in 0 until tracks.length) {
            val trk = tracks.item(i) as Element
            val nameNodes = trk.getElementsByTagName("name")
            if (nameNodes.length > 0) {
                name = nameNodes.item(0).textContent
            }

            val segments = trk.getElementsByTagName("trkseg")
            for (j in 0 until segments.length) {
                val seg = segments.item(j) as Element
                val pts = seg.getElementsByTagName("trkpt")
                for (k in 0 until pts.length) {
                    val pt = pts.item(k) as Element
                    val lat = pt.getAttribute("lat").toDouble()
                    val lon = pt.getAttribute("lon").toDouble()

                    var ele = 0.0
                    val eleNodes = pt.getElementsByTagName("ele")
                    if (eleNodes.length > 0) {
                        ele = eleNodes.item(0).textContent.toDoubleOrNull() ?: 0.0
                    }

                    var time = 0L
                    val timeNodes = pt.getElementsByTagName("time")
                    if (timeNodes.length > 0) {
                        val timeStr = timeNodes.item(0).textContent
                        try {
                            time = Instant.parse(timeStr).toEpochMilli()
                        } catch (_: Exception) {
                            time = 0L
                        }
                    }

                    points.add(WayPoint(lat, lon, ele, time))
                }
            }
        }

        return Route(
            name = name,
            distance = 0.0,
            elevationGain = 0.0,
            points = points
        )
    }
}
