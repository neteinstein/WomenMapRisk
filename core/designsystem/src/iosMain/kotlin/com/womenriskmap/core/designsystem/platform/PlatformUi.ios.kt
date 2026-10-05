package com.womenriskmap.core.designsystem.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject

@Composable
actual fun rememberLocationPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onResult)
    val manager = remember { CLLocationManager() }
    val delegate = remember {
        object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                val status = manager.authorizationStatus
                if (status != kCLAuthorizationStatusNotDetermined) {
                    callback.value(status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways)
                }
            }
        }
    }
    DisposableEffect(manager) {
        manager.delegate = delegate
        onDispose { manager.delegate = null }
    }
    return remember(manager) { { manager.requestWhenInUseAuthorization() } }
}

@Composable
actual fun rememberShareText(): (text: String) -> Unit = remember {
    { text ->
        val controller = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
        topViewController()?.presentViewController(controller, animated = true, completion = null)
    }
}

@Composable
actual fun rememberOpenLanguageSettings(): () -> Boolean = remember {
    {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
        if (url != null) UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
        url != null
    }
}

private fun topViewController() = UIApplication.sharedApplication.connectedScenes
    .filterIsInstance<UIWindowScene>()
    .flatMap { scene -> scene.windows.filterIsInstance<UIWindow>() }
    .firstOrNull { it.isKeyWindow() }
    ?.rootViewController
    ?.let { root ->
        var top = root
        while (top.presentedViewController != null) top = top.presentedViewController!!
        top
    }
