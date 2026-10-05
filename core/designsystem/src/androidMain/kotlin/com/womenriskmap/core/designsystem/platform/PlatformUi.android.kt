package com.womenriskmap.core.designsystem.platform

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberLocationPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        onResult(result.values.any { it })
    }
    return remember(launcher) {
        { launcher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) }
    }
}

@Composable
actual fun rememberShareText(): (text: String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { text ->
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

@Composable
actual fun rememberOpenLanguageSettings(): () -> Boolean {
    val context = LocalContext.current
    return remember(context) {
        {
            val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null))
            runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        }
    }
}
