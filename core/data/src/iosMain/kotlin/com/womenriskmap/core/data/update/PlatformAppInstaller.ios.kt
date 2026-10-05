package com.womenriskmap.core.data.update

import com.womenriskmap.core.data.platform.PlatformContext

actual class PlatformAppInstaller actual constructor(context: PlatformContext) : AppInstaller {
    override val isSelfUpdateBuild: Boolean = false
    override val installedVersionName: String = ""

    override fun canInstallPackages(): Boolean = false

    override fun openInstallPermissionSettings() = Unit

    override suspend fun downloadAndInstall(apkUrl: String, versionName: String): Unit =
        throw UnsupportedOperationException("Self-update is Android-only")
}
