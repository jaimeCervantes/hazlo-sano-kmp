package com.hazlosano.data.catalog

import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.VisitorLocation
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogApiTest {

    private val emptyPage = """{"posts":[],"nextPage":null,"prevPage":null,"total":0}"""

    private class Captured {
        var request: HttpRequestData? = null
    }

    private fun apiRecording(captured: Captured, body: String = """{"posts":[]}"""): CatalogApi {
        val engine = MockEngine { request ->
            captured.request = request
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return CatalogApi(client, baseUrl = "https://example.test")
    }

    @Test
    fun `asks for the page and size in the path the site expects`() = runTest {
        val captured = Captured()

        apiRecording(captured).fetchPillarPage(PillarType.NUTRITION, page = 2, pageSize = 20, location = null)

        assertEquals("/api/posts/page/2/pageSize/20", captured.request?.url?.encodedPath)
    }

    @Test
    fun `sends the pillar key and not the enum name`() = runTest {
        val captured = Captured()

        apiRecording(captured).fetchPillarPage(PillarType.MIND, page = 1, pageSize = 20, location = null)

        // MIND maps to mindSpirit. Sending "MIND" would silently return the unfiltered catalogue.
        assertEquals("mindSpirit", captured.request?.url?.parameters?.get("pillar"))
    }

    @Test
    fun `asks for spanish`() = runTest {
        val captured = Captured()

        apiRecording(captured).fetchPillarPage(PillarType.SLEEP, page = 1, pageSize = 20, location = null)

        assertEquals("es", captured.request?.url?.parameters?.get("locale"))
    }

    @Test
    fun `sends the location as the cookie the site already reads`() = runTest {
        val captured = Captured()
        val here = VisitorLocation(19.4326, -99.1332, fixedAtEpochMillis = 1_756_512_000_000L)

        apiRecording(captured).fetchPillarPage(PillarType.MOVEMENT, page = 1, pageSize = 20, location = here)

        val cookie = captured.request?.headers?.get(HttpHeaders.Cookie)
        assertEquals("hs_location=19.4326,-99.1332,1756512000000", cookie)
    }

    @Test
    fun `a location with no timestamp sends only the two coordinates`() = runTest {
        val captured = Captured()

        apiRecording(captured).fetchPillarPage(
            PillarType.MOVEMENT,
            page = 1,
            pageSize = 20,
            location = VisitorLocation(19.4326, -99.1332),
        )

        // The site's parseFix treats the third field as optional, for ever: cookies written by the
        // older format are still out there with two fields.
        assertEquals("hs_location=19.4326,-99.1332", captured.request?.headers?.get(HttpHeaders.Cookie))
    }

    @Test
    fun `no location means no cookie at all`() = runTest {
        val captured = Captured()

        apiRecording(captured).fetchPillarPage(PillarType.SLEEP, page = 1, pageSize = 20, location = null)

        assertNull(captured.request?.headers?.get(HttpHeaders.Cookie))
    }

    @Test
    fun `an impossible location is not sent`() = runTest {
        val captured = Captured()

        apiRecording(captured).fetchPillarPage(
            PillarType.SLEEP,
            page = 1,
            pageSize = 20,
            location = VisitorLocation(0.0, 0.0),
        )

        assertNull(captured.request?.headers?.get(HttpHeaders.Cookie))
    }

    @Test
    fun `unknown fields in the response do not break the read`() = runTest {
        val captured = Captured()
        val withExtras = """
            {"posts":[{"id":"p1","title":"Algo","price":10.0,"kind":"producto",
            "somethingTheSiteAddedLater":{"nested":true}}],"nextPage":2}
        """.trimIndent()

        val response = apiRecording(captured, withExtras)
            .fetchPillarPage(PillarType.NUTRITION, page = 1, pageSize = 20, location = null)

        assertEquals(1, response.posts.size)
        assertEquals("Algo", response.posts.first().title)
        assertEquals(2, response.nextPage)
    }

    @Test
    fun `an empty page parses without inventing anything`() = runTest {
        val captured = Captured()

        val response = apiRecording(captured, emptyPage)
            .fetchPillarPage(PillarType.MIND, page = 9, pageSize = 20, location = null)

        assertTrue(response.posts.isEmpty())
        assertNull(response.nextPage)
    }
}
