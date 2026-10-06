package com.womenriskmap.core.domain.model

/** A published Android release newer than the installed one (self-updating "github" build only). */
data class AppUpdate(val versionName: String, val apkUrl: String)

sealed interface UpdateCheckResult {
    data class UpToDate(val currentVersionName: String) : UpdateCheckResult

    data class Available(val update: AppUpdate) : UpdateCheckResult
}
