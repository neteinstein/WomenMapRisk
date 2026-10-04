package com.womenriskmap.app.di

import com.womenriskmap.core.data.platform.PlatformContext
import org.koin.core.scope.Scope

internal actual fun Scope.createPlatformContext(): PlatformContext = PlatformContext()
