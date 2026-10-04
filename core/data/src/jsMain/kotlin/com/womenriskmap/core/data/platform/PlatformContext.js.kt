package com.womenriskmap.core.data.platform

import io.github.xxfast.kstore.Codec
import io.github.xxfast.kstore.storage.StorageCodec
import kotlinx.browser.localStorage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

actual class PlatformContext

private val storeJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

actual fun <T : Any> PlatformContext.codecFor(name: String, serializer: KSerializer<T>): Codec<T> =
    StorageCodec(key = "womenriskmap.$name", format = storeJson, serializer = serializer, storage = localStorage)
