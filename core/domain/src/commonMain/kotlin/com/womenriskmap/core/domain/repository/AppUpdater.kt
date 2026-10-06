package com.womenriskmap.core.domain.repository

import com.womenriskmap.core.domain.model.AppUpdate
import com.womenriskmap.core.domain.model.UpdateCheckResult

/**
 * In-app self-update from GitHub Releases. Only the Android "github" flavour supports it: Play Store builds,
 * iOS and web report [isSupported] = false and the Settings screen hides the section.
 */
interface AppUpdater {
    val isSupported: Boolean

    suspend fun checkForUpdate(): Result<UpdateCheckResult>

    /** Android's per-app "install unknown apps" permission. */
    fun canInstallPackages(): Boolean

    fun openInstallPermissionSettings()

    /** Downloads the APK and hands it to the system package installer, which asks the user to confirm. */
    suspend fun downloadAndInstall(update: AppUpdate): Result<Unit>
}
