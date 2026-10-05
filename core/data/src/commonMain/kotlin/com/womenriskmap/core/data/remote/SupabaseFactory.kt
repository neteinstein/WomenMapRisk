package com.womenriskmap.core.data.remote

import com.womenriskmap.core.data.config.AppConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

/** Deep link used to return from email confirmation / Google OAuth on mobile: womenriskmap://login-callback */
const val AUTH_SCHEME = "womenriskmap"
const val AUTH_HOST = "login-callback"

/**
 * Builds the single Supabase client. When local.properties has no real project configured, it still
 * builds (placeholder URL), so the app runs offline/visitor-only and CI can compile without secrets.
 */
fun createAppSupabaseClient(
    url: String = AppConfig.SUPABASE_URL.ifBlank { "https://placeholder.supabase.co" },
    anonKey: String = AppConfig.SUPABASE_ANON_KEY.ifBlank { "placeholder" },
): SupabaseClient = createSupabaseClient(supabaseUrl = url, supabaseKey = anonKey) {
    install(Auth) {
        flowType = FlowType.PKCE
        scheme = AUTH_SCHEME
        host = AUTH_HOST
    }
    install(Postgrest)
    install(Realtime)
}
