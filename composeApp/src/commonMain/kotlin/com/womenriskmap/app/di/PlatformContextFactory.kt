package com.womenriskmap.app.di

import com.womenriskmap.core.data.platform.PlatformContext
import org.koin.core.scope.Scope

/** Builds the data layer's [PlatformContext] (Android: from `androidContext()`; iOS/web: empty). */
internal expect fun Scope.createPlatformContext(): PlatformContext
