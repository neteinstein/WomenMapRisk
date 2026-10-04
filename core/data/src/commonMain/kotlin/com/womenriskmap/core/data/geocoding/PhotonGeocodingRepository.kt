package com.womenriskmap.core.data.geocoding

import com.womenriskmap.core.data.remote.remote
import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.model.PilotCity
import com.womenriskmap.core.domain.model.Place
import com.womenriskmap.core.domain.repository.GeocodingRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Free geocoding via Photon (komoot, OpenStreetMap data). No API key; fair-use limits apply.
 * Searches are biased towards [near] (or the pilot city) so "Rua de Santa Catarina" finds Porto first.
 */
class PhotonGeocodingRepository(
    private val http: HttpClient = defaultHttpClient(),
    private val baseUrl: String = "https://photon.komoot.io",
) : GeocodingRepository {

    override suspend fun search(query: String, near: GeoPoint?): Result<List<Place>> = remote {
        if (query.isBlank()) return@remote emptyList()
        val bias = near ?: PilotCity.center
        val response: PhotonResponse = http.get("$baseUrl/api/") {
            parameter("q", query.trim())
            parameter("lat", bias.latitude)
            parameter("lon", bias.longitude)
            parameter("limit", 8)
        }.body()
        response.features.mapNotNull { it.toPlace() }
    }

    override suspend fun nameOf(point: GeoPoint): Result<String?> = remote {
        val response: PhotonResponse = http.get("$baseUrl/reverse") {
            parameter("lat", point.latitude)
            parameter("lon", point.longitude)
            parameter("limit", 1)
        }.body()
        response.features.firstOrNull()?.properties?.areaName()
    }

    companion object {
        fun defaultHttpClient() = HttpClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            expectSuccess = true
        }
    }
}

@Serializable
internal data class PhotonResponse(val features: List<PhotonFeature> = emptyList())

@Serializable
internal data class PhotonFeature(val geometry: PhotonGeometry? = null, val properties: PhotonProperties = PhotonProperties()) {
    fun toPlace(): Place? {
        val coords = geometry?.coordinates ?: return null
        if (coords.size < 2) return null
        val title = properties.name ?: properties.street?.let { s -> properties.housenumber?.let { "$s $it" } ?: s } ?: return null
        val detail = listOfNotNull(
            properties.street.takeIf { properties.name != null },
            properties.district ?: properties.locality,
            properties.city,
        ).distinct().joinToString(", ").ifBlank { null }
        return Place(name = title, detail = detail, location = GeoPoint(latitude = coords[1], longitude = coords[0]))
    }
}

@Serializable
internal data class PhotonGeometry(val coordinates: List<Double> = emptyList())

@Serializable
internal data class PhotonProperties(
    val name: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val district: String? = null,
    val locality: String? = null,
    val city: String? = null,
) {
    /** Approximate area name: never includes the house number (privacy: zones are approximate). */
    fun areaName(): String? = street ?: name ?: district ?: locality ?: city
}
