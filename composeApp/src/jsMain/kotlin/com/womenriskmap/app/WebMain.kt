package com.womenriskmap.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.womenriskmap.app.di.initKoin
import kotlinx.browser.document
import org.jetbrains.skiko.wasm.onWasmReady
import org.maplibre.compose.browser.installMapLibreCompose

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin()
    onWasmReady {
        // MapLibre GL JS needs Compose's graphics context: must run inside onWasmReady, before ComposeViewport.
        installMapLibreCompose()
        ComposeViewport(document.body!!) { App() }
    }
}
