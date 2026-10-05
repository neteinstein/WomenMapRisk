package com.womenriskmap.core.data

import com.womenriskmap.core.data.geocoding.PhotonGeocodingRepository
import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.domain.model.GeoPoint
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PhotonGeocodingRepositoryTest {
    private val searchJson = """
        {"type":"FeatureCollection","features":[
          {"geometry":{"type":"Point","coordinates":[-8.6061,41.1466]},
           "properties":{"name":"Mercado do Bolhão","street":"Rua Formosa","city":"Porto","district":"Bonfim"}},
          {"geometry":{"type":"Point","coordinates":[-8.6110,41.1496]},
           "properties":{"street":"Rua de Santa Catarina","housenumber":"112","city":"Porto"}},
          {"properties":{"name":"no geometry"}}
        ]}
    """.trimIndent()

    private fun repo(body: String, status: HttpStatusCode = HttpStatusCode.OK, onRequest: (String) -> Unit = {}) =
        PhotonGeocodingRepository(
            http = HttpClient(
                MockEngine { request ->
                    onRequest(request.url.toString())
                    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
                },
            ) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                expectSuccess = true
            },
        )

    @Test
    fun parses_places_and_skips_features_without_geometry() = runTest {
        var url = ""
        val places = repo(searchJson) { url = it }.search("bolhão", near = null).getOrThrow()
        assertEquals(2, places.size)
        assertEquals("Mercado do Bolhão", places[0].name)
        assertEquals("Rua Formosa, Bonfim, Porto", places[0].detail)
        assertEquals(GeoPoint(41.1466, -8.6061), places[0].location)
        assertEquals("Rua de Santa Catarina 112", places[1].name)
        assertTrue("lat=41.1496" in url, "biased to pilot city: $url")
    }

    @Test
    fun blank_query_returns_empty_without_network() = runTest {
        var called = false
        assertEquals(emptyList(), repo(searchJson) { called = true }.search("  ", null).getOrThrow())
        assertEquals(false, called)
    }

    @Test
    fun reverse_returns_street_without_house_number() = runTest {
        val name = repo(searchJson.replace("\"name\":\"Mercado do Bolhão\",", "")).nameOf(GeoPoint(41.1, -8.6)).getOrThrow()
        assertEquals("Rua Formosa", name)
    }

    @Test
    fun server_errors_map_to_domain_errors() = runTest {
        val result = repo("{}", HttpStatusCode.InternalServerError).search("x", null)
        assertIs<DomainException>(result.exceptionOrNull())
    }
}
