package com.womenriskmap.core.data.update

import com.womenriskmap.core.data.remote.remote
import com.womenriskmap.core.domain.model.AppUpdate
import com.womenriskmap.core.domain.model.UpdateCheckResult
import com.womenriskmap.core.domain.repository.AppUpdater
import com.womenriskmap.core.domain.rules.AppVersion
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The platform half of self-update: what's installed, the install permission, and the actual APK install. */
interface AppInstaller {
    /** True only in the Android "github" flavour (manifest meta-data `com.womenriskmap.self_update`). */
    val isSelfUpdateBuild: Boolean
    val installedVersionName: String

    fun canInstallPackages(): Boolean

    fun openInstallPermissionSettings()

    suspend fun downloadAndInstall(apkUrl: String, versionName: String)
}

/**
 * Checks this repo's public GitHub Releases (no key; unauthenticated rate limit is 60 requests/hour per IP) for
 * the newest `android-v<version>` release published by release-android.yml, and installs its "-github.apk".
 */
class GitHubAppUpdater(
    private val installer: AppInstaller,
    private val http: HttpClient = defaultHttpClient(),
    private val repository: String = REPOSITORY,
) : AppUpdater {
    override val isSupported: Boolean get() = installer.isSelfUpdateBuild

    override suspend fun checkForUpdate(): Result<UpdateCheckResult> = remote {
        val releases: List<GitHubRelease> = http.get("https://api.github.com/repos/$repository/releases") {
            header("Accept", "application/vnd.github+json")
            parameter("per_page", 30)
        }.body()
        val current = installer.installedVersionName
        val latest = latestAndroidRelease(releases)
        if (latest != null && AppVersion.isNewer(current = current, candidate = latest.versionName)) {
            UpdateCheckResult.Available(latest)
        } else {
            UpdateCheckResult.UpToDate(current)
        }
    }

    override fun canInstallPackages(): Boolean = installer.canInstallPackages()

    override fun openInstallPermissionSettings() = installer.openInstallPermissionSettings()

    override suspend fun downloadAndInstall(update: AppUpdate): Result<Unit> = remote {
        installer.downloadAndInstall(update.apkUrl, update.versionName)
    }

    companion object {
        const val REPOSITORY = "neteinstein/WomenMapRisk"

        // Keep in sync with .github/workflows/release-android.yml (tag and asset names).
        const val TAG_PREFIX = "android-v"
        const val APK_SUFFIX = "-github.apk"

        fun defaultHttpClient() = HttpClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            expectSuccess = true
        }
    }
}

/** Newest published Android release that carries a self-updating APK. GitHub lists releases newest first. */
internal fun latestAndroidRelease(releases: List<GitHubRelease>): AppUpdate? = releases
    .asSequence()
    .filter { !it.draft && !it.prerelease && it.tagName.startsWith(GitHubAppUpdater.TAG_PREFIX) }
    .mapNotNull { release ->
        val apk = release.assets.firstOrNull { it.name.endsWith(GitHubAppUpdater.APK_SUFFIX) } ?: return@mapNotNull null
        AppUpdate(versionName = release.tagName.removePrefix(GitHubAppUpdater.TAG_PREFIX), apkUrl = apk.downloadUrl)
    }
    .firstOrNull()

@Serializable
internal data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
internal data class GitHubAsset(val name: String, @SerialName("browser_download_url") val downloadUrl: String)
