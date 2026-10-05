package com.womenriskmap.core.data

import com.womenriskmap.core.data.update.AppInstaller
import com.womenriskmap.core.data.update.GitHubAppUpdater
import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.domain.model.AppUpdate
import com.womenriskmap.core.domain.model.UpdateCheckResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GitHubAppUpdaterTest {
    private class FakeInstaller(override val installedVersionName: String = "1.0.5") : AppInstaller {
        override val isSelfUpdateBuild = true
        val installed = mutableListOf<String>()
        override fun canInstallPackages() = true
        override fun openInstallPermissionSettings() = Unit
        override suspend fun downloadAndInstall(apkUrl: String, versionName: String) {
            installed += apkUrl
        }
    }

    // Newest first, as GitHub returns them: a draft, a non-Android tag, then real Android releases.
    private val releasesJson = """
        [
          {"tag_name":"android-v1.0.9","draft":true,"assets":[{"name":"WomenRiskMap_version1_0_9-github.apk","browser_download_url":"https://x/9.apk"}]},
          {"tag_name":"ios-v1.0.8","assets":[]},
          {"tag_name":"android-v1.0.7","assets":[
            {"name":"WomenRiskMap_version1_0_7-playstore.apk","browser_download_url":"https://x/7-play.apk"},
            {"name":"WomenRiskMap_version1_0_7-github.apk","browser_download_url":"https://x/7-github.apk"}]},
          {"tag_name":"android-v1.0.6","assets":[{"name":"WomenRiskMap_version1_0_6-github.apk","browser_download_url":"https://x/6.apk"}]}
        ]
    """.trimIndent()

    private fun updater(
        installer: AppInstaller,
        body: String = releasesJson,
        status: HttpStatusCode = HttpStatusCode.OK,
        onUrl: (String) -> Unit = {
        },
    ) =
        GitHubAppUpdater(
            installer = installer,
            http = HttpClient(
                MockEngine { request ->
                    onUrl(request.url.toString())
                    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
                },
            ) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                expectSuccess = true
            },
        )

    @Test
    fun picks_newest_published_android_release_and_its_github_apk() = runTest {
        var url = ""
        val result = updater(FakeInstaller("1.0.5")) { url = it }.checkForUpdate().getOrThrow()
        assertEquals(UpdateCheckResult.Available(AppUpdate("1.0.7", "https://x/7-github.apk")), result)
        assertTrue(url.startsWith("https://api.github.com/repos/neteinstein/WomenMapRisk/releases"), url)
    }

    @Test
    fun up_to_date_when_installed_is_latest_or_no_release_exists() = runTest {
        assertEquals(UpdateCheckResult.UpToDate("1.0.7"), updater(FakeInstaller("1.0.7")).checkForUpdate().getOrThrow())
        assertEquals(UpdateCheckResult.UpToDate("1.0.5"), updater(FakeInstaller("1.0.5"), body = "[]").checkForUpdate().getOrThrow())
    }

    @Test
    fun http_errors_map_to_domain_errors() = runTest {
        val error = updater(FakeInstaller(), body = "{}", status = HttpStatusCode.Forbidden).checkForUpdate().exceptionOrNull()
        assertIs<DomainException>(error)
    }

    @Test
    fun install_delegates_to_platform() = runTest {
        val installer = FakeInstaller()
        updater(installer).downloadAndInstall(AppUpdate("1.0.7", "https://x/7-github.apk")).getOrThrow()
        assertEquals(listOf("https://x/7-github.apk"), installer.installed)
    }
}
