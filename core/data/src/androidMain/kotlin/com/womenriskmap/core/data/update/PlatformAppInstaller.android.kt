package com.womenriskmap.core.data.update

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.womenriskmap.core.data.platform.PlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

actual class PlatformAppInstaller actual constructor(context: PlatformContext) : AppInstaller {
    private val context = context.context

    // Only the "github" flavour manifest (androidApp/src/github) sets this, together with
    // REQUEST_INSTALL_PACKAGES, the FileProvider and the cleanup receiver.
    override val isSelfUpdateBuild: Boolean by lazy {
        val info = this.context.packageManager.getApplicationInfo(this.context.packageName, PackageManager.GET_META_DATA)
        info.metaData?.getBoolean(SELF_UPDATE_META_DATA, false) == true
    }

    // minSdk 32: the PackageInfoFlags overload only exists from API 33.
    @Suppress("DEPRECATION")
    override val installedVersionName: String
        get() = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

    override fun canInstallPackages(): Boolean = context.packageManager.canRequestPackageInstalls()

    override fun openInstallPermissionSettings() {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    override suspend fun downloadAndInstall(apkUrl: String, versionName: String) {
        val apk = withContext(Dispatchers.IO) {
            val dir = updatesDir(context.cacheDir)
            dir.deleteRecursively() // only one pending update at a time
            dir.mkdirs()
            File(dir, "WomenRiskMap-$versionName.apk").also { download(apkUrl, it) }
        }
        // content:// URI: a file:// URI would throw FileUriExposedException. Authority matches the github manifest.
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.update.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun download(url: String, destination: File) {
        val connection = URL(url).openConnection() as HttpURLConnection // follows GitHub's redirect to its CDN
        try {
            if (connection.responseCode !=
                HttpURLConnection.HTTP_OK
            ) {
                throw IOException("APK download failed: HTTP ${connection.responseCode}")
            }
            connection.inputStream.use { input -> destination.outputStream().use { input.copyTo(it) } }
        } finally {
            connection.disconnect()
        }
    }

    internal companion object {
        const val SELF_UPDATE_META_DATA = "com.womenriskmap.self_update"

        // Keep in sync with androidApp/src/github/res/xml/update_file_paths.xml.
        fun updatesDir(cacheDir: File) = File(cacheDir, "updates")
    }
}
