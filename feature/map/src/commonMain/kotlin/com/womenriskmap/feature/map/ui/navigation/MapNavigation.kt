package com.womenriskmap.feature.map.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.feature.map.ui.screens.MapFocus
import com.womenriskmap.feature.map.ui.screens.MapScreen
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Top-level map destination. Optional focus centres the map and opens a zone (from Saved). */
@Serializable
data class MapRoute(val focusLat: Double? = null, val focusLng: Double? = null, val focusZoneId: String? = null) {
    val focus: MapFocus? get() = if (focusLat != null && focusLng != null) MapFocus(GeoPoint(focusLat, focusLng), focusZoneId) else null
}

fun NavGraphBuilder.mapScreen(
    contentPadding: () -> PaddingValues,
    onOpenReport: (GeoPoint?) -> Unit,
    onSignUp: () -> Unit,
    onLogin: () -> Unit,
) {
    composable<MapRoute> { entry ->
        val route = entry.toRoute<MapRoute>()
        MapScreen(
            viewModel = koinViewModel(key = route.toString()) { parametersOf(route.focus) },
            contentPadding = contentPadding(),
            onOpenReport = onOpenReport,
            onSignUp = onSignUp,
            onLogin = onLogin,
        )
    }
}
