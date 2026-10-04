package com.womenriskmap.app.di

import com.womenriskmap.app.AppViewModel
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.singleOf
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
    single<ReportRepository> { SupabaseReportRepository(get(), get(), get()) }
    single<SavedZoneRepository> { SupabaseSavedZoneRepository(get()) }
    single<InviteRepository> { SupabaseInviteRepository(get()) }
    single<GeocodingRepository> { PhotonGeocodingRepository() }
    single<PreferencesRepository> { KStorePreferencesRepository(get(), get(AppScope)) }

    // Domain
    singleOf(::ZoneRiskCalculator)
    singleOf(::LoadZonesUseCase)

    // App-level ViewModels
    viewModelOf(::AppViewModel)
}
