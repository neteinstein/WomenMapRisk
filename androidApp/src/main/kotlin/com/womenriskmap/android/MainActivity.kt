package com.womenriskmap.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.womenriskmap.app.App
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val supabase: SupabaseClient by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleAuthDeeplink(intent)
        setContent { App() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthDeeplink(intent)
    }

    private fun handleAuthDeeplink(intent: Intent?) {
        if (intent?.data?.scheme == "womenriskmap") supabase.handleDeeplinks(intent)
    }
}
