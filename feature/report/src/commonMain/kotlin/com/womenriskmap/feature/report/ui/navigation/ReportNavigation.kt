package com.womenriskmap.feature.report.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.feature.report.ui.screens.ReportScreen
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** New report near [lat]/[lng] (fallback when location is unavailable), or edit [reportId] (within 24 h). */
@Serializable
data class ReportRoute(val lat: Double? = null, val lng: Double? = null, val reportId: String? = null) {
    val near: GeoPoint? get() = if (lat != null && lng != null) GeoPoint(lat, lng) else null
}

fun NavGraphBuilder.reportScreen(onClose: () -> Unit) {
    composable<ReportRoute> { entry ->
        val route = entry.toRoute<ReportRoute>()
        ReportScreen(viewModel = koinViewModel { parametersOf(route.near, route.reportId) }, onClose = onClose)
    }
}
