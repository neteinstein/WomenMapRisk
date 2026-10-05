package com.womenriskmap.app.di

import com.womenriskmap.app.AppViewModel
import com.womenriskmap.core.data.config.AppConfig
import com.womenriskmap.core.data.demo.DemoReportRepository
import com.womenriskmap.core.data.geocoding.PhotonGeocodingRepository
import com.womenriskmap.core.data.local.LocalStores
import com.womenriskmap.core.data.platform.PlatformConnectivityMonitor
import com.womenriskmap.core.data.platform.PlatformLocationProvider
import com.womenriskmap.core.data.remote.createAppSupabaseClient
import com.womenriskmap.core.data.repository.KStorePreferencesRepository
import com.womenriskmap.core.data.repository.SupabaseInviteRepository
import com.womenriskmap.core.data.repository.SupabaseReportRepository
import com.womenriskmap.core.data.repository.SupabaseSavedZoneRepository
import com.womenriskmap.core.data.repository.SupabaseSessionRepository
import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.repository.ConnectivityMonitor
import com.womenriskmap.core.domain.repository.GeocodingRepository
import com.womenriskmap.core.domain.repository.InviteRepository
import com.womenriskmap.core.domain.repository.LocationProvider
import com.womenriskmap.core.domain.repository.PreferencesRepository
import com.womenriskmap.core.domain.repository.ReportRepository
import com.womenriskmap.core.domain.repository.SavedZoneRepository
import com.womenriskmap.core.domain.repository.SessionRepository
import com.womenriskmap.core.domain.rules.ZoneRiskCalculator
import com.womenriskmap.core.domain.usecase.LoadZonesUseCase
import com.womenriskmap.feature.auth.ui.screens.AuthMode
import com.womenriskmap.feature.auth.ui.screens.AuthViewModel
import com.womenriskmap.feature.auth.ui.screens.CheckEmailViewModel
import com.womenriskmap.feature.auth.ui.screens.InviteGateViewModel
import com.womenriskmap.feature.invites.domain.GetInviteOverviewUseCase
import com.womenriskmap.feature.invites.ui.screens.InvitesViewModel
import com.womenriskmap.feature.map.domain.ConfirmReportUseCase
import com.womenriskmap.feature.map.domain.FlagReportUseCase
import com.womenriskmap.feature.map.domain.LoadZoneDetailUseCase
import com.womenriskmap.feature.map.ui.screens.MapFocus
import com.womenriskmap.feature.map.ui.screens.MapViewModel
import com.womenriskmap.feature.moderation.data.SupabaseModerationRepository
import com.womenriskmap.feature.moderation.domain.ModerationRepository
import com.womenriskmap.feature.moderation.ui.screens.ModerationViewModel
import com.womenriskmap.feature.profile.data.SupabaseAccountRepository
import com.womenriskmap.feature.profile.domain.AccountRepository
import com.womenriskmap.feature.profile.ui.screens.ProfileViewModel
import com.womenriskmap.feature.profile.ui.screens.SettingsViewModel
import com.womenriskmap.feature.report.domain.SubmitReportUseCase
import com.womenriskmap.feature.report.ui.screens.ReportViewModel
import com.womenriskmap.feature.saved.domain.SavedZonesWithRiskUseCase
import com.womenriskmap.feature.saved.ui.screens.SavedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Clock

private val AppScope = named("appScope")

/**
 * THE single Koin module for the whole app (all platforms). Register every dependency here,
 * never per platform. Platform differences are hidden behind expect/actual classes in core:data.
 */
val appModule = module {
    // Infrastructure
    single<Clock> { Clock.System }
    single(AppScope) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { createPlatformContext() }
    single { LocalStores(get()) }
    single { createAppSupabaseClient() }

    // Platform services
    single<LocationProvider> { PlatformLocationProvider(get()) }
    single<ConnectivityMonitor> { PlatformConnectivityMonitor(get()) }

    // Repositories (core contracts -> data implementations)
    single<SessionRepository> { SupabaseSessionRepository(get(), get(), get(AppScope)) }
    // Demo mode: without a configured Supabase project (local.properties), show sample Porto data read-only.
    single<ReportRepository> { if (AppConfig.isConfigured) SupabaseReportRepository(get(), get(), get()) else DemoReportRepository(get()) }
    single<SavedZoneRepository> { SupabaseSavedZoneRepository(get()) }
    single<InviteRepository> { SupabaseInviteRepository(get()) }
    single<GeocodingRepository> { PhotonGeocodingRepository() }
    single<PreferencesRepository> { KStorePreferencesRepository(get(), get(AppScope)) }
    single<AccountRepository> { SupabaseAccountRepository(get(), get()) }
    single<ModerationRepository> { SupabaseModerationRepository(get()) }

    // Domain
    single { ZoneRiskCalculator() } // explicit: singleOf() would try to inject the defaulted RiskThresholds
    singleOf(::LoadZonesUseCase)
    factoryOf(::LoadZoneDetailUseCase)
    factoryOf(::ConfirmReportUseCase)
    factoryOf(::FlagReportUseCase)
    factoryOf(::SubmitReportUseCase)
    factoryOf(::SavedZonesWithRiskUseCase)
    factoryOf(::GetInviteOverviewUseCase)

    // ViewModels (one line per screen; route parameters arrive via parametersOf)
    viewModelOf(::AppViewModel)
    viewModel { (mode: AuthMode, invite: String?) -> AuthViewModel(get(), mode, invite) }
    viewModel { (email: String) -> CheckEmailViewModel(email, get()) }
    viewModelOf(::InviteGateViewModel)
    viewModel { (focus: MapFocus?) ->
        MapViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), focus)
    }
    viewModel { (near: GeoPoint?, editingId: String?) -> ReportViewModel(get(), get(), get(), get(), get(), get(), near, editingId) }
    viewModelOf(::SavedViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ModerationViewModel)
    viewModel { InvitesViewModel(get(), get(), linkBaseUrl = AppConfig.WEB_APP_URL.ifBlank { "https://womenriskmap.example" }) }
}
