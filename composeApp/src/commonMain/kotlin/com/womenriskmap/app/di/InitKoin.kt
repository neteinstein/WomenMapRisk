package com.womenriskmap.app.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Platform DI bootstrap. Module DEFINITIONS live in [appModule] (AppModule.kt); this file only starts Koin.
 *
 * - Android: `WomenRiskMapApp.onCreate()` calls `initKoin { androidContext(this@WomenRiskMapApp) }`.
 * - iOS: `iOSApp.init()` calls `InitKoinKt.doInitKoin()`. Kotlin/Native's Obj-C exporter renames
 *   top-level functions starting with `init` to `doInit…`, hence the Swift name.
 * - Web: `main()` calls `initKoin()`.
 */
fun initKoin() = initKoin {}

fun initKoin(platform: KoinAppDeclaration) {
    startKoin {
        platform()
        modules(appModule)
    }
}
