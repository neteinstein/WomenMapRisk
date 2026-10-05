package com.womenriskmap.core.data.platform

import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.repository.ConnectivityMonitor
import com.womenriskmap.core.domain.repository.LocationProvider
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.Foundation.NSError
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

actual class PlatformLocationProvider actual constructor(context: PlatformContext) : LocationProvider {
    private val manager = CLLocationManager().apply { desiredAccuracy = kCLLocationAccuracyHundredMeters }
    private var pending: CompletableDeferred<GeoPoint?>? = null

    @OptIn(ExperimentalForeignApi::class)
    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val location = didUpdateLocations.lastOrNull() as? CLLocation
            val point = location?.coordinate?.useContents { GeoPoint(latitude, longitude) }
            pending?.complete(point)
        }

        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            pending?.complete(null)
        }
    }

    init {
        manager.delegate = delegate
    }

    override fun hasPermission(): Boolean {
        val status = manager.authorizationStatus
        return status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways
    }

    override suspend fun currentLocation(): GeoPoint? {
        if (!hasPermission()) return null
        val deferred = CompletableDeferred<GeoPoint?>()
        pending = deferred
        manager.requestLocation()
        return withTimeoutOrNull(8_000) { deferred.await() }
    }
}

actual class PlatformConnectivityMonitor actual constructor(context: PlatformContext) : ConnectivityMonitor {
    private val state = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = state.asStateFlow()

    init {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_update_handler(monitor) { path ->
            state.value = nw_path_get_status(path) == nw_path_status_satisfied
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)
    }
}
