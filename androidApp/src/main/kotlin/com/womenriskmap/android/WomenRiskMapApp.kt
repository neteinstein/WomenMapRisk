package com.womenriskmap.android

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.womenriskmap.app.di.initKoin
import org.koin.android.ext.koin.androidContext

class WomenRiskMapApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase is auto-initialised by google-services; Crashlytics only collects in release builds.
        runCatching { FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !isDebuggable() }
        initKoin { androidContext(this@WomenRiskMapApp) }
    }

    private fun isDebuggable() = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
