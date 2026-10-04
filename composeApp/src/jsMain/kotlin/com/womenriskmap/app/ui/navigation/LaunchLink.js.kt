package com.womenriskmap.app.ui.navigation

import kotlinx.browser.window
import org.w3c.dom.url.URLSearchParams

actual fun launchInviteCode(): String? = URLSearchParams(window.location.search).get("invite")?.takeIf { it.isNotBlank() }
