package com.womenriskmap.app

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import org.koin.mp.KoinPlatform
import platform.Foundation.NSURL

/** Called from iOSApp.swift `.onOpenURL`: completes Supabase email-confirmation / OAuth sign-in. */
fun handleAuthDeepLink(url: NSURL) {
    if (url.scheme != "womenriskmap") return
    KoinPlatform.getKoin().get<SupabaseClient>().handleDeeplinks(url)
}
