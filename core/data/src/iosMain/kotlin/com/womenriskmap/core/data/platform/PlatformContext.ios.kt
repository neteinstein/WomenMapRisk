package com.womenriskmap.core.data.platform

import io.github.xxfast.kstore.Codec
import io.github.xxfast.kstore.file.FileCodec
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class PlatformContext

private val storeJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@OptIn(ExperimentalForeignApi::class)
actual fun <T : Any> PlatformContext.codecFor(name: String, serializer: KSerializer<T>): Codec<T> {
    val dir = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )?.path ?: error("No Application Support directory")
    val file = Path(dir, "$name.json")
    return FileCodec(file = file, tempFile = Path("$file.temp"), json = storeJson, serializer = serializer)
}
