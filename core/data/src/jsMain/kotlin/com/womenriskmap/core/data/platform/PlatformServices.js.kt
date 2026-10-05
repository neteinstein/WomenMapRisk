package com.womenriskmap.core.data.platform

import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.repository.ConnectivityMonitor
import com.womenriskmap.core.domain.repository.LocationProvider
import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

actual class PlatformLocationProvider actual constructor(context: PlatformContext) : LocationProvider {
    private var granted = false

    // The browser prompts on first use; we can't query synchronously, so assume "maybe" until a fix succeeds.
    override fun hasPermission(): Boolean = granted

    override suspend fun currentLocation(): GeoPoint? {
        val geolocation = window.navigator.asDynamic().geolocation ?: return null
        return withTimeoutOrNull(10_000) {
            suspendCancellableCoroutine { cont ->
                geolocation.getCurrentPosition(
                    { pos: dynamic ->
                        granted = true
                        cont.resume(GeoPoint(pos.coords.latitude as Double, pos.coords.longitude as Double))
                    },
                    { _: dynamic -> cont.resume(null) },
                    js("({ enableHighAccuracy: false, timeout: 9000, maximumAge: 60000 })"),
                )
            }
        }
    }
}

actual class PlatformConnectivityMonitor actual constructor(context: PlatformContext) : ConnectivityMonitor {
    private val state = MutableStateFlow(window.navigator.onLine)
    override val isOnline: StateFlow<Boolean> = state.asStateFlow()

    init {
        window.addEventListener("online", { state.value = true })
        window.addEventListener("offline", { state.value = false })
    }
}
