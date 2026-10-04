package com.womenriskmap.core.designsystem.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

@Composable
actual fun rememberLocationPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit = remember(onResult) {
    {
        val geolocation = window.navigator.asDynamic().geolocation
        if (geolocation == null) {
            onResult(false)
        } else {
            geolocation.getCurrentPosition({ _: dynamic -> onResult(true) }, { _: dynamic -> onResult(false) })
        }
    }
}

@Composable
actual fun rememberShareText(): (text: String) -> Unit = remember {
    { text ->
        val navigator = window.navigator.asDynamic()
        if (navigator.share != null) {
            val data: dynamic = js("({})")
            data.text = text
            navigator.share(data)
        } else {
            navigator.clipboard?.writeText(text)
        }
    }
}

@Composable
actual fun rememberOpenLanguageSettings(): () -> Boolean = remember { { false } }
