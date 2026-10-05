package com.womenriskmap.core.designsystem.platform

import androidx.compose.runtime.Composable

/**
 * The few UI interactions that genuinely differ per platform. Everything else is commonMain.
 */

/** Returns a launcher that asks for location permission and reports the result. */
@Composable
expect fun rememberLocationPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit

/** Returns a function that opens the system share sheet with [text] (web: Web Share API, else clipboard). */
@Composable
expect fun rememberShareText(): (text: String) -> Unit

/** Opens the OS settings page where the per-app language can be chosen. Returns false if not supported (web). */
@Composable
expect fun rememberOpenLanguageSettings(): () -> Boolean
