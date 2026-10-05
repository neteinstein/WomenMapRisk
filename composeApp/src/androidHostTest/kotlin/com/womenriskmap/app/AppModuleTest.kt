package com.womenriskmap.app

import com.womenriskmap.app.di.appModule
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify
import kotlin.test.Test

class AppModuleTest {
    // Lives in androidHostTest (not commonTest) because the Android Context is supplied by androidContext() at bootstrap.

    /** Fails if any registered definition has a constructor dependency that isn't registered. */
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun app_module_graph_is_complete() {
        appModule.verify(
            extraTypes = listOf(
                kotlinx.coroutines.CoroutineScope::class,
                androidx.lifecycle.SavedStateHandle::class,
                android.content.Context::class,
            ),
        )
    }
}
