package com.womenriskmap.core.data.local

import com.womenriskmap.core.data.platform.PlatformContext
import com.womenriskmap.core.data.platform.codecFor
import com.womenriskmap.core.data.remote.dto.ProfileDto
import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.model.Report
import io.github.xxfast.kstore.KStore
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** Last successfully fetched reports, for offline mode (spec §7 "Sem internet"). Contains only public, anonymised data. */
@Serializable
data class AreaCache(val reports: List<Report> = emptyList(), val fetchedAt: Instant? = null)

@Serializable
data class StoredPreferences(
    val locationHistoryEnabled: Boolean = false,
    val lastKnownLocation: GeoPoint? = null,
    val welcomeSeen: Boolean = false,
)

@Serializable
data class CachedProfile(val profile: ProfileDto? = null)

/** All on-device stores, created once (see di/AppModule.kt). */
class LocalStores(context: PlatformContext) {
    val area: KStore<AreaCache> = KStore(default = AreaCache(), codec = context.codecFor("area_cache", AreaCache.serializer()))
    val preferences: KStore<StoredPreferences> =
        KStore(default = StoredPreferences(), codec = context.codecFor("preferences", StoredPreferences.serializer()))
    val profile: KStore<CachedProfile> = KStore(default = CachedProfile(), codec = context.codecFor("profile", CachedProfile.serializer()))

    /** Wipes everything personal (sign-out / account deletion). The anonymised area cache is public data and may stay. */
    suspend fun clearPersonal() {
        profile.set(CachedProfile())
        preferences.update { (it ?: StoredPreferences()).copy(lastKnownLocation = null) }
    }
}
