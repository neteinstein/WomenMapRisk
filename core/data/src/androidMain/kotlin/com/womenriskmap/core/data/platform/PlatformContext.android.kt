package com.womenriskmap.core.data.platform

import android.content.Context
import io.github.xxfast.kstore.Codec
import io.github.xxfast.kstore.file.FileCodec
import kotlinx.io.files.Path
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

actual class PlatformContext(val context: Context)

private val storeJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

actual fun <T : Any> PlatformContext.codecFor(name: String, serializer: KSerializer<T>): Codec<T> {
    val file = Path(context.filesDir.absolutePath, "$name.json")
    return FileCodec(file = file, tempFile = Path("$file.temp"), json = storeJson, serializer = serializer)
}
