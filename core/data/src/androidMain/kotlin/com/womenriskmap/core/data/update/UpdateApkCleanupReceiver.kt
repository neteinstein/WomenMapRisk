package com.womenriskmap.core.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Deletes the downloaded update APK once the app has been replaced by it. MY_PACKAGE_REPLACED is exempt from the
 * implicit-broadcast limits and reaches only the updated app; it must be manifest-declared (androidApp/src/github)
 * because the process is usually killed during the install. Deleting earlier is unsafe: the installer may still be
 * reading the file.
 */
class UpdateApkCleanupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        Thread {
            try {
                PlatformAppInstaller.updatesDir(context.cacheDir).deleteRecursively()
            } finally {
                pending.finish()
            }
        }.start()
    }
}
