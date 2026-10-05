package com.womenriskmap.core.data.platform

import io.github.xxfast.kstore.Codec
import kotlinx.serialization.KSerializer

/**
 * The one platform handle the data layer needs (Android: application Context; iOS/web: nothing).
 * Created in di/AppModule.kt via [org.koin.core.scope.Scope]-aware factories, never stored globally.
 */
expect class PlatformContext

/** Persistent storage for small JSON documents: app files dir on Android/iOS, localStorage on web. */
expect fun <T : Any> PlatformContext.codecFor(name: String, serializer: KSerializer<T>): Codec<T>
