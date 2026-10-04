package com.womenriskmap.core.data.platform

import com.womenriskmap.core.domain.repository.ConnectivityMonitor
import com.womenriskmap.core.domain.repository.LocationProvider

/** Platform location: Android LocationManager, iOS CoreLocation, web navigator.geolocation. */
expect class PlatformLocationProvider(context: PlatformContext) : LocationProvider

/** Platform connectivity: Android ConnectivityManager, iOS NWPathMonitor, web online/offline events. */
expect class PlatformConnectivityMonitor(context: PlatformContext) : ConnectivityMonitor
